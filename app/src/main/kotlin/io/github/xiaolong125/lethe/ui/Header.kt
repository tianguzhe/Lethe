package io.github.xiaolong125.lethe.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.xiaolong125.lethe.R
import io.github.xiaolong125.lethe.data.ConnectionState
import io.github.xiaolong125.lethe.ui.theme.DroidGreen
import io.github.xiaolong125.lethe.ui.theme.GlowMint
import io.github.xiaolong125.lethe.ui.theme.RiverBottom
import io.github.xiaolong125.lethe.ui.theme.RiverTop
import io.github.xiaolong125.lethe.ui.theme.StatusOff
import io.github.xiaolong125.lethe.ui.theme.StatusOk
import io.github.xiaolong125.lethe.ui.theme.StatusWarn

private val OnRiver = Color.White
private val OnRiverMuted = Color.White.copy(alpha = 0.72f)
private val Glass = Color.White.copy(alpha = 0.10f)
private val GlassBorder = Color.White.copy(alpha = 0.16f)

@Composable
fun RiverHeader(
  connection: ConnectionState,
  ownerUser: Boolean,
  onConnect: () -> Unit,
  onOpenShizuku: () -> Unit,
  onRefresh: () -> Unit,
  onLogs: () -> Unit,
) {
  Box(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
      .background(Brush.linearGradient(listOf(RiverTop, RiverBottom))),
  ) {
    GlowSwoosh(Modifier.matchParentSize())
    Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 22.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        GlowingLogo()
        Text(
          "Lethe",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = OnRiver,
          modifier = Modifier.weight(1f).padding(start = 14.dp),
        )
        GlassIconButton(R.drawable.ic_refresh, "刷新应用列表", onRefresh)
        Spacer(Modifier.width(8.dp))
        GlassIconButton(R.drawable.ic_log, "执行日志", onLogs)
      }
      Spacer(Modifier.height(20.dp))
      StatusPanel(connection, ownerUser, onConnect, onOpenShizuku)
    }
  }
}

/** The launcher art with a soft green halo behind it, echoing the glow around the icon. */
@Composable
private fun GlowingLogo() {
  Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
    Canvas(Modifier.matchParentSize()) {
      drawCircle(Brush.radialGradient(listOf(GlowMint.copy(alpha = 0.45f), Color.Transparent)), radius = size.minDimension / 2)
    }
    Image(painterResource(R.drawable.lethe_logo), contentDescription = null, modifier = Modifier.size(48.dp))
  }
}

@Composable
private fun GlassIconButton(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
  Box(
    Modifier
      .size(40.dp)
      .clip(CircleShape)
      .background(Glass)
      .border(1.dp, GlassBorder, CircleShape)
      .clickable(role = Role.Button, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Icon(painterResource(icon), contentDescription = description, tint = OnRiver, modifier = Modifier.size(20.dp))
  }
}

@Composable
private fun StatusPanel(c: ConnectionState, ownerUser: Boolean, onConnect: () -> Unit, onOpenShizuku: () -> Unit) {
  val (title, hint, tone) = when {
    !ownerUser -> Triple("请在主用户中运行", "本工具只处理用户 0 下的应用", StatusOff)
    !c.alive -> Triple("Shizuku 未运行", "浏览无需授权；卸载前请先启动 Shizuku", StatusOff)
    c.binding -> Triple("正在连接卸载服务…", "首次连接需在 Shizuku 弹窗中允许", StatusWarn)
    c.connected -> Triple("已连接 · 可以卸载", "以 ${if (c.uid == 0) "Root" else "Shell"} 身份运行 · UID ${c.uid}", StatusOk)
    else -> Triple("Shizuku 已运行", "点击连接并授权后即可卸载", StatusWarn)
  }
  val accent by animateColorAsState(tone, label = "statusAccent")
  val shape = RoundedCornerShape(24.dp)
  Row(
    Modifier
      .fillMaxWidth()
      .clip(shape)
      .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.16f), Glass)))
      // The rim takes the status colour so the connection state reads at a glance, fading out to glass.
      .border(1.dp, Brush.horizontalGradient(listOf(accent.copy(alpha = 0.55f), GlassBorder)), shape)
      .padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    StatusBadge(accent, pulsing = c.connected || c.binding)
    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = OnRiver)
      Text(hint, style = MaterialTheme.typography.bodySmall, color = OnRiverMuted)
    }
    val action: (() -> Unit)? = when {
      !ownerUser || c.connected || c.binding -> null
      !c.alive -> onOpenShizuku
      else -> onConnect
    }
    if (action != null) {
      Button(
        onClick = action,
        colors = ButtonDefaults.buttonColors(containerColor = DroidGreen, contentColor = RiverBottom),
      ) { Text(if (!c.alive) "打开 Shizuku" else "连接", fontWeight = FontWeight.SemiBold) }
    }
  }
}

/** Shield in a status-tinted ring; the ring pulses outward while connected or connecting. */
@Composable
private fun StatusBadge(color: Color, pulsing: Boolean) {
  val pulse = rememberInfiniteTransition(label = "pulse")
  val halo by pulse.animateFloat(1f, 1.5f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "halo")
  Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
    Canvas(Modifier.matchParentSize()) {
      val r = size.minDimension / 2 / 1.5f
      if (pulsing) drawCircle(color.copy(alpha = (1.5f - halo) * 0.8f), radius = r * halo, style = Stroke(1.5.dp.toPx()))
      drawCircle(color.copy(alpha = 0.18f), radius = r)
      drawCircle(color.copy(alpha = 0.6f), radius = r, style = Stroke(1.dp.toPx()))
    }
    Icon(painterResource(R.drawable.ic_shield), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
  }
}

/**
 * Glowing swooshes echoing the launcher art: each arc is a wide faint stroke (the glow) under a thin bright one,
 * faded at both ends. Static on purpose so the header costs nothing while scrolling.
 */
@Composable
private fun GlowSwoosh(modifier: Modifier) {
  Canvas(modifier) {
    val w = size.width
    val h = size.height
    val fade = Brush.horizontalGradient(listOf(Color.Transparent, GlowMint, Color.Transparent))
    val arcs = listOf(
      Path().apply { moveTo(-0.05f * w, 0.62f * h); quadraticTo(0.40f * w, 1.08f * h, 1.05f * w, 0.30f * h) },
      Path().apply { moveTo(0.45f * w, -0.02f * h); quadraticTo(0.95f * w, 0.08f * h, 1.05f * w, 0.48f * h) },
    )
    arcs.forEach { arc ->
      drawPath(arc, fade, alpha = 0.10f, style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round))
      drawPath(arc, fade, alpha = 0.40f, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
    }
  }
}

