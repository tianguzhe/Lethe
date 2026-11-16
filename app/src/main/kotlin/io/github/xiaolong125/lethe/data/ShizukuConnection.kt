package io.github.xiaolong125.lethe.data

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import io.github.xiaolong125.lethe.BuildConfig
import io.github.xiaolong125.lethe.IUninstallService
import io.github.xiaolong125.lethe.service.UninstallService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import rikka.shizuku.Shizuku

private const val PERMISSION_REQUEST = 1
private const val BIND_TIMEOUT_MS = 15_000L
private const val MIN_SHIZUKU_VERSION = 13

/** @property uid the service process uid once connected: 2000 for shell (ADB / wireless debugging), 0 for root. */
data class ConnectionState(
  val alive: Boolean = false,
  val binding: Boolean = false,
  val connected: Boolean = false,
  val uid: Int? = null,
)

/**
 * Owns the Shizuku listeners and the bound [UninstallService]. All callbacks are funnelled to the main thread,
 * so [service] and [state] are only mutated there.
 *
 * @param report receives user-facing messages; `true` marks an error.
 */
class ShizukuConnection(private val report: (String, Boolean) -> Unit) {
  private val main = Handler(Looper.getMainLooper())
  private val _state = MutableStateFlow(ConnectionState())
  val state: StateFlow<ConnectionState> = _state.asStateFlow()

  var service: IUninstallService? = null
    private set

  private val args = Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, UninstallService::class.java.name))
    .daemon(false) // Shizuku stops the service when this app process exits.
    .processNameSuffix("uninstall")
    .version(BuildConfig.VERSION_CODE)
    .tag("user0-uninstall")

  private val received = Shizuku.OnBinderReceivedListener { main.post(::refresh) }
  private val died = Shizuku.OnBinderDeadListener {
    main.post {
      service = null
      _state.update { it.copy(binding = false) }
      refresh()
      report("Shizuku 已断开，请重新启动后连接。", true)
    }
  }
  private val permission = Shizuku.OnRequestPermissionResultListener { _, grant ->
    main.post {
      if (grant == PackageManager.PERMISSION_GRANTED) connect()
      else report("未获 Shizuku 授权。可浏览应用；卸载需要授权。", true)
    }
  }
  private val connection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
      main.post {
        main.removeCallbacks(bindTimeout)
        val candidate = IUninstallService.Stub.asInterface(binder)
        var uid: Int? = null
        service = try {
          uid = candidate.uid
          if (uid != 2000 && uid != 0) throw SecurityException("服务权限异常：$uid")
          report("已连接卸载服务，UID=$uid", false)
          candidate
        } catch (e: Exception) {
          report("连接服务失败：${e.message}", true)
          uid = null
          null
        }
        _state.update { it.copy(binding = false, uid = uid) }
        refresh()
      }
    }

    override fun onServiceDisconnected(name: ComponentName) {
      main.post {
        service = null
        _state.update { it.copy(binding = false) }
        refresh()
      }
    }
  }
  private val bindTimeout = Runnable {
    if (_state.value.binding) {
      _state.update { it.copy(binding = false) }
      report("连接服务超时，请检查 Shizuku 并重试。", true)
    }
  }

  fun start() {
    Shizuku.addBinderReceivedListenerSticky(received)
    Shizuku.addBinderDeadListener(died)
    Shizuku.addRequestPermissionResultListener(permission)
    refresh()
  }

  fun refresh() {
    _state.update { it.copy(alive = Shizuku.pingBinder(), connected = service != null, uid = if (service != null) it.uid else null) }
  }

  /** Requests permission if needed, then binds the UserService. Progress is reported through [state] and [report]. */
  fun connect() {
    if (_state.value.binding) return
    try {
      if (!Shizuku.pingBinder()) return report("请先打开并启动 Shizuku，再回来连接。", true)
      if (Shizuku.isPreV11() || Shizuku.getVersion() < MIN_SHIZUKU_VERSION) return report("请升级到 Shizuku 13 或更新版本。", true)
      if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
        if (Shizuku.shouldShowRequestPermissionRationale()) return report("请在 Shizuku 的应用管理中允许本工具使用权限。", true)
        return Shizuku.requestPermission(PERMISSION_REQUEST)
      }
      if (service != null) return refresh()
      _state.update { it.copy(binding = true) }
      Shizuku.bindUserService(args, connection)
      main.postDelayed(bindTimeout, BIND_TIMEOUT_MS)
    } catch (e: Exception) {
      _state.update { it.copy(binding = false) }
      report("连接失败：${e.message}", true)
    }
  }

  fun close() {
    Shizuku.removeBinderReceivedListener(received)
    Shizuku.removeBinderDeadListener(died)
    Shizuku.removeRequestPermissionResultListener(permission)
    main.removeCallbacksAndMessages(null)
    // Detach only; the non-daemon service stops with this process.
    runCatching { Shizuku.unbindUserService(args, connection, false) }
      .onFailure { android.util.Log.w("ShizukuConnection", "unbind failed", it) }
  }
}
