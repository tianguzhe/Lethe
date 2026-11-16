package io.github.xiaolong125.lethe.ui

import android.app.Application
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.xiaolong125.lethe.IUninstallService
import io.github.xiaolong125.lethe.core.AppCounts
import io.github.xiaolong125.lethe.core.AppEntry
import io.github.xiaolong125.lethe.core.AppFilter
import io.github.xiaolong125.lethe.core.AppSort
import io.github.xiaolong125.lethe.core.OperationOutcome
import io.github.xiaolong125.lethe.core.PackageRules
import io.github.xiaolong125.lethe.core.counts
import io.github.xiaolong125.lethe.core.query
import io.github.xiaolong125.lethe.data.AppRepository
import io.github.xiaolong125.lethe.data.ConnectionState
import io.github.xiaolong125.lethe.data.IconLoader
import io.github.xiaolong125.lethe.data.ShizukuConnection
import io.github.xiaolong125.lethe.data.packageChanges
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

private const val LOG_LIMIT = 20_000
private const val LOG_KEEP = 16_000
const val ICON_DP = 44

enum class OperationKind { UNINSTALL, RESTORE }

data class Operation(val kind: OperationKind, val app: AppEntry)

data class Notice(val text: String, val error: Boolean)

data class ListUi(
  val loading: Boolean = true,
  val visible: List<AppEntry> = emptyList(),
  val counts: AppCounts = AppCounts(),
  val query: String = "",
  val filter: AppFilter = AppFilter.ALL,
  val sort: AppSort = AppSort.NAME,
)

private data class ListSource(
  val apps: List<AppEntry> = emptyList(),
  val loading: Boolean = true,
  val query: String = "",
  val filter: AppFilter = AppFilter.ALL,
  val sort: AppSort = AppSort.NAME,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val selfPackage = application.packageName
  private val repository = AppRepository(application)
  val icons = IconLoader(application, ICON_DP)

  private val source = MutableStateFlow(ListSource())
  private val _logs = MutableStateFlow("")
  private val _operation = MutableStateFlow<Operation?>(null)
  private val _confirm = MutableStateFlow<Operation?>(null)
  private val _selected = MutableStateFlow<String?>(null)
  private val notices = Channel<Notice>(Channel.BUFFERED)
  private val shizuku = ShizukuConnection(::report)

  /** Only user 0 is supported: the service always targets user 0, so running elsewhere would act on another user's apps. */
  val ownerUser: Boolean = Process.myUid() / 100_000 == 0

  val list: StateFlow<ListUi> = source
    .map { s -> ListUi(s.loading, s.apps.query(s.query, s.filter, s.sort), s.apps.counts(), s.query, s.filter, s.sort) }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.Eagerly, ListUi())
  val connection: StateFlow<ConnectionState> = shizuku.state
  val logs: StateFlow<String> = _logs.asStateFlow()
  /** The operation currently running in the service, if any. Only one runs at a time. */
  val operation: StateFlow<Operation?> = _operation.asStateFlow()
  /** An operation waiting for the user's confirmation. */
  val confirm: StateFlow<Operation?> = _confirm.asStateFlow()
  val selected: StateFlow<AppEntry?> = combine(source, _selected) { s, pkg -> pkg?.let { p -> s.apps.find { it.pkg == p } } }
    .stateIn(viewModelScope, SharingStarted.Eagerly, null)
  val noticeFlow: Flow<Notice> = notices.receiveAsFlow()

  init {
    shizuku.start()
    loadApps()
    viewModelScope.launch { packageChanges(application).collect(::syncPackage) }
  }

  fun isProtected(app: AppEntry): Boolean = PackageRules.isProtected(app.pkg, selfPackage)

  fun onQueryChange(query: String) = source.update { it.copy(query = query) }
  fun onFilterChange(filter: AppFilter) = source.update { it.copy(filter = filter) }
  fun onSortChange(sort: AppSort) = source.update { it.copy(sort = sort) }
  fun select(app: AppEntry?) = _selected.update { app?.pkg }
  fun refreshConnection() = shizuku.refresh()

  /** Connecting is independent of list loading and running operations: binding never touches app state. */
  fun connect() {
    if (ownerUser) shizuku.connect()
  }

  fun loadApps() {
    if (!ownerUser) return report("本工具只支持用户 0，请在主用户中安装运行。", true)
    source.update { it.copy(loading = true) }
    viewModelScope.launch {
      try {
        val loaded = withContext(Dispatchers.IO) { repository.loadApps() }
        source.update { it.copy(apps = loaded) }
        log("已读取 ${loaded.size} 个应用。")
      } catch (e: Exception) {
        report("读取应用列表失败：${e.message}", true)
      } finally {
        source.update { it.copy(loading = false) }
      }
    }
  }

  private suspend fun syncPackage(pkg: String) {
    icons.evict(pkg)
    val entry = try {
      withContext(Dispatchers.IO) { repository.loadEntry(pkg) }
    } catch (e: Exception) {
      log("同步 $pkg 状态失败：${e.message}")
      return
    }
    source.update { s ->
      val rest = s.apps.filterNot { it.pkg == pkg }
      s.copy(apps = if (entry == null) rest else repository.sortByName(rest + entry))
    }
  }

  /** Validates preconditions and, if they hold, asks the UI to confirm. */
  fun request(kind: OperationKind, app: AppEntry) {
    if (_operation.value != null) return report("请等待当前操作完成。", true)
    if (isProtected(app)) return report("此应用不能通过本工具操作。", true)
    if (shizuku.service == null || !Shizuku.pingBinder()) return report("请先连接并授权 Shizuku。", true)
    _confirm.value = Operation(kind, app)
  }

  fun dismissConfirm() {
    _confirm.value = null
  }

  fun confirm() {
    val op = _confirm.value ?: return
    _confirm.value = null
    val target = shizuku.service
    if (_operation.value != null || target == null) return report("Shizuku 服务不可用，请重新连接。", true)
    _operation.value = op
    val verb = if (op.kind == OperationKind.UNINSTALL) "卸载" else "恢复"
    log("开始${verb}：${op.app.name} (${op.app.pkg})")
    viewModelScope.launch {
      val raw = try {
        withContext(Dispatchers.IO) { call(target, op) }
      } catch (e: Exception) {
        "ERROR\n服务已断开或调用失败，请刷新核对应用状态：${e.message}"
      }
      val outcome = OperationOutcome.parse(raw)
      // Reflect the verified end state immediately; the package broadcast will refine it shortly after.
      if (outcome.done) syncPackage(op.app.pkg)
      if (outcome.done && op.kind == OperationKind.UNINSTALL) _selected.value = null
      _operation.value = null
      report(outcome.message, !outcome.done)
    }
  }

  private fun call(target: IUninstallService, op: Operation): String = when (op.kind) {
    OperationKind.UNINSTALL -> target.uninstall(op.app.pkg)
    OperationKind.RESTORE -> target.restore(op.app.pkg)
  }

  /** Logs and shows [text] to the user. */
  private fun report(text: String, error: Boolean) {
    log(text)
    notices.trySend(Notice(text, error))
  }

  private fun log(text: String) {
    val line = SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(Date()) + "  " + text + "\n"
    _logs.update { (it + line).let { all -> if (all.length > LOG_LIMIT) all.takeLast(LOG_KEEP) else all } }
  }

  override fun onCleared() {
    shizuku.close()
  }
}
