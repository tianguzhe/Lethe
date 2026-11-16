package io.github.xiaolong125.lethe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.xiaolong125.lethe.R
import io.github.xiaolong125.lethe.core.AppEntry
import io.github.xiaolong125.lethe.core.formatBytes
import io.github.xiaolong125.lethe.data.IconLoader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailSheet(
  app: AppEntry,
  protected: Boolean,
  connected: Boolean,
  icons: IconLoader,
  onCopy: (String) -> Unit,
  onUninstall: () -> Unit,
  onRestore: () -> Unit,
  onDismiss: () -> Unit,
) {
  // Open fully so the uninstall/restore action is visible without an extra drag.
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(app.pkg, app.name, icons, 64.dp, Modifier.clip(RoundedCornerShape(16.dp)))
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
          Text(app.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
          Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            badgesOf(app, protected).forEach { Badge(it) }
          }
        }
      }
      Row(
        Modifier
          .padding(top = 16.dp)
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
          .clickable(role = Role.Button, onClickLabel = "复制包名") { onCopy(app.pkg) }
          .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(app.pkg, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = { onCopy(app.pkg) }) { Icon(painterResource(R.drawable.ic_copy), contentDescription = "复制包名") }
      }

      Spacer(Modifier.height(12.dp))
      val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT)
      InfoRow("版本", app.versionName ?: "—")
      InfoRow("安装包大小", formatBytes(app.apkBytes))
      InfoRow("首次安装", if (app.firstInstallTime > 0) date.format(Date(app.firstInstallTime)) else "—")
      InfoRow("最近更新", if (app.lastUpdateTime > 0) date.format(Date(app.lastUpdateTime)) else "—")
      InfoRow("状态", if (!app.installed) "用户 0 已卸载，系统保留安装包" else if (app.enabled) "已启用" else "已停用")
      app.sourceDir?.let { InfoRow("安装路径", it, mono = true) }

      Spacer(Modifier.height(20.dp))
      when {
        protected -> Notice("本工具和 Shizuku 受保护，不能通过本工具操作。", R.drawable.ic_shield)
        !connected -> Notice("连接并授权 Shizuku 后才能${if (app.installed) "卸载" else "恢复"}。")
        app.installed -> Button(
          onClick = onUninstall,
          modifier = Modifier.fillMaxWidth().height(52.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
        ) {
          Icon(painterResource(R.drawable.ic_delete), contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(Modifier.size(8.dp))
          Text("卸载（用户 0）")
        }
        else -> Button(onClick = onRestore, modifier = Modifier.fillMaxWidth().height(52.dp)) {
          Icon(painterResource(R.drawable.ic_restore), contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(Modifier.size(8.dp))
          Text("恢复到用户 0")
        }
      }
    }
  }
}

@Composable
private fun InfoRow(label: String, value: String, mono: Boolean = false) {
  Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 16.dp))
    SelectionContainer(Modifier.weight(1f)) {
      Text(
        value,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = if (mono) FontFamily.Monospace else null,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun Notice(text: String, icon: Int = R.drawable.ic_info) {
  Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(start = 12.dp))
  }
}

@Composable
fun ConfirmDialog(op: Operation, onConfirm: () -> Unit, onDismiss: () -> Unit) {
  val app = op.app
  val uninstall = op.kind == OperationKind.UNINSTALL
  // System components can break core phone features, so they require an explicit acknowledgement.
  val needsAck = uninstall && app.system
  var acknowledged by remember(op) { mutableStateOf(false) }
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(painterResource(if (uninstall) R.drawable.ic_delete else R.drawable.ic_restore), contentDescription = null) },
    title = { Text(if (uninstall) "卸载“${app.name}”？" else "恢复“${app.name}”？") },
    text = {
      Column {
        Text(app.pkg, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Text(
          if (uninstall) {
            "将卸载用户 0 下的此应用，并删除其应用数据。此操作无法撤销数据。" +
              if (app.system) "\n\n这是系统应用，卸载可能影响系统功能。系统通常仍保留其安装包，之后可在“可恢复”中恢复（数据不会恢复）。" else ""
          } else {
            "将使用系统保留的安装包，为用户 0 重新安装此应用。之前删除的数据不会恢复。"
          },
          style = MaterialTheme.typography.bodyMedium,
        )
        if (needsAck) {
          Row(
            Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Checkbox) { acknowledged = !acknowledged },
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Checkbox(checked = acknowledged, onCheckedChange = { acknowledged = it })
            Text("我了解卸载系统应用的风险", style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onConfirm,
        enabled = !needsAck || acknowledged,
        colors = if (uninstall) ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.textButtonColors(),
      ) { Text(if (uninstall) "确认卸载" else "确认恢复") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
  )
}

/** Blocking while the service works: the user must not start another operation or leave mid-way unknowingly. */
@Composable
fun ProgressDialog(op: Operation) {
  AlertDialog(
    onDismissRequest = {},
    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    icon = { CircularProgressIndicator(Modifier.size(32.dp)) },
    title = { Text(if (op.kind == OperationKind.UNINSTALL) "正在卸载并核对…" else "正在恢复并核对…") },
    text = { Text("${op.app.name}\n${op.app.pkg}", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
    confirmButton = {},
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsSheet(logs: String, onCopy: () -> Unit, onDismiss: () -> Unit) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("执行日志", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onCopy, enabled = logs.isNotEmpty()) {
          Icon(painterResource(R.drawable.ic_copy), contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.size(6.dp))
          Text("复制")
        }
      }
      SelectionContainer(
        Modifier
          .padding(top = 12.dp)
          .fillMaxWidth()
          .heightIn(min = 120.dp, max = 480.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
          .verticalScroll(rememberScrollState())
          .padding(12.dp),
      ) {
        Text(logs.ifEmpty { "暂无日志" }, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
      }
    }
  }
}
