package io.github.xiaolong125.lethe.data

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Chinese OEM ROMs (MIUI/HyperOS, vivo OriginOS, ...) gate `getInstalledPackages` behind an extra runtime
 * permission on top of QUERY_ALL_PACKAGES, per YD/T 2407-2021. Without it the list silently shrinks to a few
 * system apps. AOSP does not define the permission, so there is nothing to request there.
 */
object InstalledAppsPermission {
  const val NAME = "com.android.permission.GET_INSTALLED_APPS"

  /** @return true when the ROM defines the permission and it has not been granted yet. */
  fun needsRequest(context: Context): Boolean {
    val defined = try {
      context.packageManager.getPermissionInfo(NAME, 0)
      true
    } catch (absent: PackageManager.NameNotFoundException) {
      false
    }
    return defined && ContextCompat.checkSelfPermission(context, NAME) != PackageManager.PERMISSION_GRANTED
  }
}
