package io.github.xiaolong125.lethe.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.xiaolong125.lethe.R
import io.github.xiaolong125.lethe.core.AppCounts
import io.github.xiaolong125.lethe.core.AppEntry
import io.github.xiaolong125.lethe.core.AppFilter
import io.github.xiaolong125.lethe.core.AppSort
import io.github.xiaolong125.lethe.core.formatBytes
import io.github.xiaolong125.lethe.data.IconLoader

@Composable
fun SearchBar(
  pinned: Boolean,
  query: String,
  filter: AppFilter,
  sort: AppSort,
  counts: AppCounts,
  onQuery: (String) -> Unit,
  onFilter: (AppFilter) -> Unit,
  onSort: (AppSort) -> Unit,
) {
  val focus = LocalFocusManager.current
  val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val topInset by animateDpAsState(if (pinned) statusBar else 0.dp, label = "searchTopInset")
  Surface(color = MaterialTheme.colorScheme.background, shadowElevation = if (pinned) 3.dp else 0.dp) {
    Column(Modifier.fillMaxWidth().padding(top = topInset).padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        TextField(
          value = query,
          onValueChange = onQuery,
          modifier = Modifier.weight(1f),
          singleLine = true,
          shape = RoundedCornerShape(28.dp),
          placeholder = { Text("搜索名称或包名") },
          leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
          trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { onQuery("") }) { Text("清空") } },
          colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
          ),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        )
        SortMenu(sort, onSort)
      }
      Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
          FilterSpec(AppFilter.ALL, "全部", counts.all, R.drawable.ic_apps),
          FilterSpec(AppFilter.USER, "用户", counts.user, R.drawable.ic_person),
          FilterSpec(AppFilter.SYSTEM, "系统", counts.system, R.drawable.ic_android),
          FilterSpec(AppFilter.RESTORABLE, "可恢复", counts.restorable, R.drawable.ic_restore),
        ).forEach { spec ->
          FilterCard(spec, selected = filter == spec.filter, modifier = Modifier.weight(1f)) { onFilter(spec.filter) }
        }
      }
    }
  }
}

private data class FilterSpec(val filter: AppFilter, val label: String, val count: Int, @DrawableRes val icon: Int)

/**
 * Compact icon card used in the pinned filter bar. Selection mirrors the launcher art: a green gradient fill,
 * a primary hairline and a soft green glow. Kept two lines tall so the pinned bar stays short.
 */
@Composable
private fun FilterCard(spec: FilterSpec, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
  val scheme = MaterialTheme.colorScheme
  val shape = RoundedCornerShape(16.dp)
  val glow by animateDpAsState(if (selected) 10.dp else 0.dp, label = "filterGlow")
  val border by animateColorAsState(if (selected) scheme.primary.copy(alpha = 0.7f) else scheme.outlineVariant.copy(alpha = 0.6f), label = "filterBorder")
  val content by animateColorAsState(if (selected) scheme.onPrimaryContainer else scheme.onSurface, label = "filterContent")
  val iconTint by animateColorAsState(if (selected) scheme.primary else scheme.onSurfaceVariant, label = "filterIcon")
  val fill = if (selected) {
    Brush.linearGradient(listOf(scheme.primaryContainer, scheme.tertiaryContainer))
  } else {
    SolidColor(scheme.surfaceContainerHigh)
  }
  Column(
    modifier
      .shadow(glow, shape, ambientColor = scheme.primary, spotColor = scheme.primary)
      .clip(shape)
      .background(fill)
      .border(1.dp, border, shape)
      .selectable(selected = selected, role = Role.Tab, onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 8.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(painterResource(spec.icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
      Spacer(Modifier.width(6.dp))
      Text(spec.count.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = content, maxLines = 1)
    }
    Text(spec.label, style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.75f), maxLines = 1, modifier = Modifier.padding(top = 2.dp))
  }
}

@Composable
private fun SortMenu(sort: AppSort, onSort: (AppSort) -> Unit) {
  var open by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { open = true }) { Icon(painterResource(R.drawable.ic_sort), contentDescription = "排序") }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      listOf(AppSort.NAME to "按名称", AppSort.RECENT to "最近更新", AppSort.SIZE to "安装包大小").forEach { (s, label) ->
        DropdownMenuItem(
          text = { Text(label, fontWeight = if (s == sort) FontWeight.Bold else FontWeight.Normal) },
          onClick = { open = false; onSort(s) },
          trailingIcon = { if (s == sort) Text("✓", color = MaterialTheme.colorScheme.primary) },
        )
      }
    }
  }
}

enum class AppBadge(val label: String) { USER("用户"), SYSTEM("系统"), PROTECTED("受保护"), RESTORABLE("可恢复"), DISABLED("已停用") }

fun badgesOf(app: AppEntry, protected: Boolean): List<AppBadge> = buildList {
  add(if (app.system) AppBadge.SYSTEM else AppBadge.USER)
  if (protected) add(AppBadge.PROTECTED)
  if (!app.installed) add(AppBadge.RESTORABLE)
  if (app.installed && !app.enabled) add(AppBadge.DISABLED)
}

@Composable
fun Badge(badge: AppBadge) {
  val scheme = MaterialTheme.colorScheme
  val (bg, fg) = when (badge) {
    AppBadge.USER -> scheme.primaryContainer to scheme.onPrimaryContainer
    AppBadge.SYSTEM -> scheme.secondaryContainer to scheme.onSecondaryContainer
    AppBadge.PROTECTED -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    AppBadge.RESTORABLE -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    AppBadge.DISABLED -> scheme.errorContainer to scheme.onErrorContainer
  }
  Text(
    badge.label,
    style = MaterialTheme.typography.labelSmall,
    color = fg,
    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 6.dp, vertical = 2.dp),
  )
}

private sealed interface IconState {
  data object Loading : IconState
  data object Missing : IconState
  data class Ready(val bitmap: ImageBitmap) : IconState
}

/**
 * The placeholder is drawn in Compose on purpose: system default icons may be adaptive-icon XML,
 * which `painterResource` cannot load and would crash on.
 */
@Composable
fun AppIcon(pkg: String, name: String, icons: IconLoader, size: Dp, modifier: Modifier = Modifier) {
  val state by produceState(icons.cached(pkg)?.let { IconState.Ready(it) } ?: IconState.Loading, pkg) {
    if (value is IconState.Loading) value = icons.load(pkg)?.let { IconState.Ready(it) } ?: IconState.Missing
  }
  when (val s = state) {
    is IconState.Ready -> Image(s.bitmap, contentDescription = null, modifier = modifier.size(size))
    else -> Box(
      modifier.size(size).background(MaterialTheme.colorScheme.surfaceContainerHighest),
      contentAlignment = Alignment.Center,
    ) {
      if (s is IconState.Missing) {
        Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
fun AppCard(app: AppEntry, protected: Boolean, icons: IconLoader, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val badges = badgesOf(app, protected)
  val meta = listOfNotNull(app.versionName?.let { "v$it" }, formatBytes(app.apkBytes).takeIf { app.apkBytes > 0 }).joinToString(" · ")
  Row(
    modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .clip(RoundedCornerShape(20.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerLow)
      .clickable(onClick = onClick)
      .semantics(mergeDescendants = true) {
        contentDescription = "${app.name}，${app.pkg}，${badges.joinToString("，") { it.label }}，点按查看详情"
      }
      .padding(12.dp)
      .alpha(if (app.installed) 1f else 0.7f),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    AppIcon(app.pkg, app.name, icons, ICON_DP.dp, Modifier.clip(RoundedCornerShape(12.dp)))
    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          app.name,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
        badges.forEach { Spacer(Modifier.width(6.dp)); Badge(it) }
      }
      Text(
        app.pkg,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (meta.isNotEmpty()) {
        Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
      }
    }
    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.outline)
  }
}

@Composable
fun SkeletonCard() {
  val pulse = rememberInfiniteTransition(label = "skeleton")
  val alpha by pulse.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha")
  val block = MaterialTheme.colorScheme.surfaceContainerHighest
  Row(
    Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .clip(RoundedCornerShape(20.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerLow)
      .padding(12.dp)
      .alpha(alpha),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(Modifier.size(ICON_DP.dp).clip(RoundedCornerShape(12.dp)).background(block))
    Column(Modifier.padding(start = 12.dp)) {
      Box(Modifier.width(140.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(block))
      Spacer(Modifier.height(8.dp))
      Box(Modifier.width(200.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(block))
    }
  }
}

@Composable
fun EmptyState(title: String, hint: String) {
  Column(Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Icon(painterResource(R.drawable.ic_wave), contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.titleMedium)
    Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
  }
}
