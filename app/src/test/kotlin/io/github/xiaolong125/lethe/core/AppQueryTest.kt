package io.github.xiaolong125.lethe.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AppQueryTest {
  private val apps = listOf(
    AppEntry("com.google.android.apps.photos", "相册", system = false, lastUpdateTime = 30, apkBytes = 50),
    AppEntry("com.android.vending", "Google Play Store", system = true, lastUpdateTime = 10, apkBytes = 300),
    AppEntry("com.tencent.wetype", "微信输入法", system = false, lastUpdateTime = 20, apkBytes = 100),
    AppEntry("com.sec.removed", "Removed Helper", system = true, installed = false),
  )

  private fun pkgs(query: String, filter: AppFilter = AppFilter.ALL, sort: AppSort = AppSort.NAME) =
    apps.query(query, filter, sort).map { it.pkg }

  @Test fun searchesChineseName() = assertEquals(listOf("com.tencent.wetype"), pkgs("微信"))

  @Test fun searchesEnglishIgnoringCaseAndSpaces() = assertEquals(listOf("com.android.vending"), pkgs(" PLAY STORE "))

  @Test fun searchesPackage() = assertEquals(listOf("com.google.android.apps.photos"), pkgs("APPS.PHOTOS"))

  @Test fun queryDoesNotMatchAcrossNameAndPackage() = assertEquals(emptyList<String>(), pkgs("相册com"))

  @Test fun allExcludesRestorable() = assertEquals(3, pkgs("").size)

  @Test fun filtersByType() {
    assertEquals(listOf("com.google.android.apps.photos", "com.tencent.wetype"), pkgs("", AppFilter.USER))
    assertEquals(listOf("com.android.vending"), pkgs("", AppFilter.SYSTEM))
    assertEquals(listOf("com.sec.removed"), pkgs("", AppFilter.RESTORABLE))
  }

  @Test fun sortsByRecentAndSize() {
    assertEquals(listOf("com.google.android.apps.photos", "com.tencent.wetype", "com.android.vending"), pkgs("", sort = AppSort.RECENT))
    assertEquals(listOf("com.android.vending", "com.tencent.wetype", "com.google.android.apps.photos"), pkgs("", sort = AppSort.SIZE))
  }

  @Test fun countsSeparateRestorable() = assertEquals(AppCounts(all = 3, user = 2, system = 1, restorable = 1), apps.counts())

  @Test fun formatsBytes() {
    assertEquals("—", formatBytes(0))
    assertEquals("512 B", formatBytes(512))
    assertEquals("2 KB", formatBytes(2048))
    assertEquals("1.5 MB", formatBytes(1024L * 1024 * 3 / 2))
    assertEquals("2.00 GB", formatBytes(2L * 1024 * 1024 * 1024))
  }
}
