package io.github.xiaolong125.lethe.core

import java.util.Locale

/**
 * Metadata for one package as seen by user 0. Icons are deliberately not stored here: they are loaded lazily
 * by [io.github.xiaolong125.lethe.data.IconLoader] so that listing hundreds of apps stays cheap.
 *
 * @property installed false means the package is uninstalled for user 0 but the system still keeps its APK,
 *   so it can be restored.
 * @property apkBytes size of base + split APKs on disk; excludes data and cache.
 */
data class AppEntry(
  val pkg: String,
  val name: String,
  val system: Boolean,
  val installed: Boolean = true,
  val enabled: Boolean = true,
  val versionName: String? = null,
  val firstInstallTime: Long = 0,
  val lastUpdateTime: Long = 0,
  val apkBytes: Long = 0,
  val sourceDir: String? = null,
) {
  /** Precomputed so search does not lowercase every entry on every keystroke. */
  val searchKey: String = (name + "\u0000" + pkg).lowercase(Locale.ROOT)
}

enum class AppFilter { ALL, USER, SYSTEM, RESTORABLE }

enum class AppSort { NAME, RECENT, SIZE }

data class AppCounts(val all: Int = 0, val user: Int = 0, val system: Int = 0, val restorable: Int = 0)

fun List<AppEntry>.counts(): AppCounts {
  val installed = filter { it.installed }
  return AppCounts(installed.size, installed.count { !it.system }, installed.count { it.system }, size - installed.size)
}

/**
 * @receiver apps already sorted by name; other sorts are stable, so ties keep name order.
 */
fun List<AppEntry>.query(query: String, filter: AppFilter, sort: AppSort): List<AppEntry> {
  val q = query.trim().lowercase(Locale.ROOT)
  val matched = filter {
    val typeMatches = when (filter) {
      AppFilter.ALL -> it.installed
      AppFilter.USER -> it.installed && !it.system
      AppFilter.SYSTEM -> it.installed && it.system
      AppFilter.RESTORABLE -> !it.installed
    }
    typeMatches && (q.isEmpty() || it.searchKey.contains(q))
  }
  return when (sort) {
    AppSort.NAME -> matched
    AppSort.RECENT -> matched.sortedByDescending { it.lastUpdateTime }
    AppSort.SIZE -> matched.sortedByDescending { it.apkBytes }
  }
}

fun formatBytes(bytes: Long): String = when {
  bytes <= 0 -> "—"
  bytes < 1024 -> "$bytes B"
  bytes < 1024 * 1024 -> String.format(Locale.ROOT, "%.0f KB", bytes / 1024.0)
  bytes < 1024L * 1024 * 1024 -> String.format(Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024)
  else -> String.format(Locale.ROOT, "%.2f GB", bytes / 1024.0 / 1024 / 1024)
}
