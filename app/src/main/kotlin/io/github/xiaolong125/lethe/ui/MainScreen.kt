package io.github.xiaolong125.lethe.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.xiaolong125.lethe.core.AppFilter
import io.github.xiaolong125.lethe.core.SHIZUKU_PACKAGE
import io.github.xiaolong125.lethe.data.InstalledAppsPermission

private const val SHIZUKU_DOWNLOAD = "https://shizuku.rikka.app/download/"
private const val SKELETON_ROWS = 8

private class NoticeVisuals(override val message: String, val error: Boolean) : SnackbarVisuals {
  override val actionLabel: String? = null
  override val withDismissAction = true
  override val duration = if (error) SnackbarDuration.Long else SnackbarDuration.Short
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
  val list by viewModel.list.collectAsStateWithLifecycle()
  val connection by viewModel.connection.collectAsStateWithLifecycle()
  val operation by viewModel.operation.collectAsStateWithLifecycle()
  val confirm by viewModel.confirm.collectAsStateWithLifecycle()
  val selected by viewModel.selected.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbar = remember { SnackbarHostState() }
  var showLogs by remember { mutableStateOf(false) }
  var showShizukuInstall by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.noticeFlow.collect { snackbar.showSnackbar(NoticeVisuals(it.text, it.error)) }
  }
  // The initial load may have run before the OEM app-list permission was granted; reload once it is.
  val appListPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    if (granted) viewModel.loadApps()
  }
  LaunchedEffect(Unit) {
    if (InstalledAppsPermission.needsRequest(context)) appListPermission.launch(InstalledAppsPermission.NAME)
  }
  // Shizuku may have been started in its own app while we were in the background.
  LifecycleResumeEffect(Unit) {
    viewModel.refreshConnection()
    onPauseOrDispose {}
  }

  val listState = rememberLazyListState()
  // The search bar only needs the status-bar inset once the header has scrolled away and it is pinned at the top.
  val searchPinned by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
  // The river header is always dark, so status-bar icons must be light over it; once the search bar is pinned
  // the bar sits on the theme background and the icons follow the theme.
  val view = LocalView.current
  val lightBackground = !isSystemInDarkTheme()
  LaunchedEffect(searchPinned, lightBackground) {
    val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = searchPinned && lightBackground
  }

  Box(Modifier.fillMaxSize().imePadding()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      state = listState,
      contentPadding = PaddingValues(bottom = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      item(key = "header") {
        Box(Modifier.widthIn(max = 840.dp)) {
          RiverHeader(
            connection = connection,
            ownerUser = viewModel.ownerUser,
            onConnect = viewModel::connect,
            onOpenShizuku = {
              val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
              if (launch != null) context.startActivity(launch) else showShizukuInstall = true
            },
            onRefresh = viewModel::loadApps,
            onLogs = { showLogs = true },
          )
        }
      }
      stickyHeader(key = "search") {
        Box(Modifier.widthIn(max = 840.dp)) {
          SearchBar(searchPinned, list.query, list.filter, list.sort, list.counts, viewModel::onQueryChange, viewModel::onFilterChange, viewModel::onSortChange)
        }
      }
      when {
        list.loading && list.visible.isEmpty() -> items(SKELETON_ROWS) { Box(Modifier.widthIn(max = 840.dp)) { SkeletonCard() } }
        list.visible.isEmpty() -> item(key = "empty") {
          when {
            list.query.isNotBlank() -> EmptyState("没有匹配的应用", "换个名称或包名试试")
            list.filter == AppFilter.RESTORABLE -> EmptyState("没有可恢复的应用", "系统应用在用户 0 卸载后会出现在这里")
            else -> EmptyState("暂无应用", "点击右上角刷新按钮重新读取")
          }
        }
        else -> items(list.visible, key = { it.pkg }) { app ->
          Box(Modifier.widthIn(max = 840.dp).animateItem()) {
            AppCard(app, viewModel.isProtected(app), viewModel.icons, onClick = { viewModel.select(app) })
          }
        }
      }
      item(key = "bottomInset") { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
    }

    // 3-button navigation is drawn transparently edge-to-edge; a scrim keeps the keys readable over list text.
    Spacer(
      Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        .windowInsetsBottomHeight(WindowInsets.navigationBars)
        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
    )

    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) { data ->
      val error = (data.visuals as? NoticeVisuals)?.error == true
      Snackbar(
        data,
        containerColor = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.inverseSurface,
        contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.inverseOnSurface,
      )
    }
  }

  selected?.let { app ->
    AppDetailSheet(
      app = app,
      protected = viewModel.isProtected(app),
      connected = connection.connected,
      icons = viewModel.icons,
      onCopy = { copy(context, "包名", it, "包名已复制") },
      onUninstall = { viewModel.request(OperationKind.UNINSTALL, app) },
      onRestore = { viewModel.request(OperationKind.RESTORE, app) },
      onDismiss = { viewModel.select(null) },
    )
  }
  confirm?.let { ConfirmDialog(it, onConfirm = viewModel::confirm, onDismiss = viewModel::dismissConfirm) }
  operation?.let { ProgressDialog(it) }
  if (showLogs) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    LogsSheet(logs, onCopy = { copy(context, "卸载日志", logs, "日志已复制") }, onDismiss = { showLogs = false })
  }
  if (showShizukuInstall) {
    AlertDialog(
      onDismissRequest = { showShizukuInstall = false },
      title = { Text("需要安装 Shizuku") },
      text = { Text("请从 Shizuku 官方网站安装，并按引导通过无线调试或电脑启动。手机重启后通常需要重新启动 Shizuku。") },
      confirmButton = {
        TextButton(onClick = {
          showShizukuInstall = false
          try {
            context.startActivity(Intent(Intent.ACTION_VIEW, SHIZUKU_DOWNLOAD.toUri()))
          } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "请访问 $SHIZUKU_DOWNLOAD", Toast.LENGTH_LONG).show()
          }
        }) { Text("打开官网") }
      },
      dismissButton = { TextButton(onClick = { showShizukuInstall = false }) { Text("取消") } },
    )
  }
}

private fun copy(context: Context, label: String, text: String, toast: String) {
  context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, text))
  Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
}
