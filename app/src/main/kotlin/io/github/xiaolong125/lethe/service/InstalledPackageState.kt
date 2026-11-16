package io.github.xiaolong125.lethe.service

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.lang.reflect.Method

/** Queries the exact package and user; OEM command-line lists may omit installed packages. */
object InstalledPackageState {
  /**
   * @param packageManager an `android.content.pm.IPackageManager` binder proxy.
   * @return whether user 0 currently has [pkg] installed.
   * @throws Exception when the query fails. An unreadable state must never count as successful removal.
   */
  fun isInstalled(packageManager: Any, pkg: String): Boolean {
    val api = Class.forName("android.content.pm.IPackageManager")
    // API 33+ takes `long flags`; older releases take `int flags`.
    val (query: Method, flags: Any) = try {
      api.getMethod("getApplicationInfo", String::class.java, Long::class.javaPrimitiveType, Int::class.javaPrimitiveType) to
        PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()
    } catch (olderApi: NoSuchMethodException) {
      api.getMethod("getApplicationInfo", String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType) to
        PackageManager.MATCH_UNINSTALLED_PACKAGES
    }
    // Include retained system APKs, but distinguish their per-user installed flag.
    val info = query.invoke(packageManager, pkg, flags, 0) as ApplicationInfo? ?: return false
    check(pkg == info.packageName) { "系统返回的包名与查询目标不一致" }
    return info.flags and ApplicationInfo.FLAG_INSTALLED != 0
  }
}
