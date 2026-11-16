package io.github.xiaolong125.lethe.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import io.github.xiaolong125.lethe.core.AppEntry
import java.io.File
import java.text.Collator

private const val TAG = "AppRepository"

/**
 * Reads package metadata visible to this (user 0) process. No icons are decoded here; see [IconLoader].
 * All functions block on PackageManager IPC and must run off the main thread.
 */
class AppRepository(context: Context) {
  private val pm = context.packageManager
  private val collator = Collator.getInstance()

  /**
   * @return installed apps plus packages uninstalled for user 0 whose APK the system still keeps, sorted by name.
   */
  fun loadApps(): List<AppEntry> = sortByName(installedPackages().map(::toEntry))

  /** @return the current state of [pkg], or null when the system no longer knows it at all. */
  fun loadEntry(pkg: String): AppEntry? = try {
    toEntry(packageInfo(pkg))
  } catch (gone: PackageManager.NameNotFoundException) {
    null
  }

  fun sortByName(apps: List<AppEntry>): List<AppEntry> = apps.sortedWith { a, b -> collator.compare(a.name, b.name) }

  private fun installedPackages(): List<PackageInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()))
    } else {
      @Suppress("DEPRECATION")
      pm.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES)
    }

  private fun packageInfo(pkg: String): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()))
    } else {
      @Suppress("DEPRECATION")
      pm.getPackageInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
    }

  private fun toEntry(info: PackageInfo): AppEntry {
    val app = info.applicationInfo
    val name = app?.let { a ->
      runCatching { pm.getApplicationLabel(a).toString() }
        .onFailure { Log.w(TAG, "label failed for ${info.packageName}", it) }
        .getOrNull()
    } ?: info.packageName
    return AppEntry(
      pkg = info.packageName,
      name = name,
      system = app != null && app.flags and ApplicationInfo.FLAG_SYSTEM != 0,
      installed = app != null && app.flags and ApplicationInfo.FLAG_INSTALLED != 0,
      enabled = app?.enabled ?: true,
      versionName = info.versionName,
      firstInstallTime = info.firstInstallTime,
      lastUpdateTime = info.lastUpdateTime,
      apkBytes = app?.let(::apkSize) ?: 0,
      sourceDir = app?.sourceDir,
    )
  }

  /** Unreadable paths count as 0 rather than failing the whole list; size is informational only. */
  private fun apkSize(app: ApplicationInfo): Long =
    (listOfNotNull(app.sourceDir) + app.splitSourceDirs.orEmpty()).sumOf { File(it).length() }
}
