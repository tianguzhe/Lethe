package io.github.xiaolong125.lethe.service

import android.content.Context
import android.content.pm.VersionedPackage
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.Process
import io.github.xiaolong125.lethe.BuildConfig
import io.github.xiaolong125.lethe.IUninstallService
import io.github.xiaolong125.lethe.core.PackageRules
import io.github.xiaolong125.lethe.core.SHIZUKU_PACKAGE
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

private const val SHELL_UID = 2000
private const val ROOT_UID = 0
private const val TARGET_USER = 0
private const val DELETE_SYSTEM_APP = 4
private const val DELETE_SUCCEEDED = 1
private const val RESULT_TIMEOUT_SECONDS = 25L
private const val INSTALL_SUCCEEDED = 1
private const val INSTALL_REASON_UNKNOWN = 0
/** Same default as `pm install-existing` on Android 10+. */
private const val INSTALL_ALL_WHITELIST_RESTRICTED_PERMISSIONS = 0x00400000

/**
 * Runs only inside the Shizuku shell/root process, never as an exported Android service.
 *
 * Results use a line protocol: `SUCCESS`, `ALREADY_ABSENT`, `ALREADY_INSTALLED` or `ERROR` on the first line,
 * followed by a human-readable message. See [io.github.xiaolong125.lethe.core.OperationOutcome].
 */
class UninstallService(context: Context) : IUninstallService.Stub() {
  private val ownerUid = context.packageManager.getApplicationInfo(BuildConfig.APPLICATION_ID, 0).uid

  private fun enforceCaller() {
    val uid = Binder.getCallingUid()
    if (uid != ownerUid && uid != SHELL_UID && uid != ROOT_UID) throw SecurityException("调用方未获授权")
  }

  override fun getUid(): Int {
    enforceCaller()
    return Process.myUid()
  }

  override fun destroy() {
    enforceCaller()
    exitProcess(0)
  }

  private fun packageManager(): Any {
    val binder = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java).invoke(null, "package")
    return Class.forName("android.content.pm.IPackageManager\$Stub").getMethod("asInterface", IBinder::class.java).invoke(null, binder)!!
  }

  /** Common precondition checks for both operations; returns the validated package name. */
  private fun checkedTarget(pkg: String?): String {
    require(PackageRules.valid(pkg)) { "包名无效" }
    require(pkg != BuildConfig.APPLICATION_ID && pkg != SHIZUKU_PACKAGE) { "不能操作本工具或 Shizuku" }
    val uid = Process.myUid()
    if (uid != SHELL_UID && uid != ROOT_UID) throw SecurityException("服务未取得 shell 权限")
    return pkg!!
  }

  private fun errorResult(error: Exception): String {
    val cause = (error as? InvocationTargetException)?.targetException ?: error
    return "ERROR\n${cause.javaClass.simpleName}: ${cause.message}"
  }

  @Synchronized
  override fun uninstall(packageName: String?): String {
    enforceCaller()
    return try {
      val pkg = checkedTarget(packageName)
      val pm = packageManager()
      if (!InstalledPackageState.isInstalled(pm, pkg)) return "ALREADY_ABSENT\n用户 0 未安装此应用，无需卸载。"
      deleteAndWait(pm, pkg)
      check(!InstalledPackageState.isInstalled(pm, pkg)) { "核对失败，用户 0 仍安装此包。" }
      "SUCCESS\n卸载成功，已确认用户 0 不再安装 $pkg。"
    } catch (error: Exception) {
      errorResult(error)
    }
  }

  /**
   * Re-enables a package for user 0 whose APK the system still keeps (typically a preinstalled app that was
   * uninstalled for this user). Equivalent to `pm install-existing --user 0`; verified by re-querying afterwards.
   */
  @Synchronized
  override fun restore(packageName: String?): String {
    enforceCaller()
    return try {
      val pkg = checkedTarget(packageName)
      val pm = packageManager()
      if (InstalledPackageState.isInstalled(pm, pkg)) return "ALREADY_INSTALLED\n用户 0 已安装此应用，无需恢复。"
      val code = installExisting(pm, pkg)
      check(code == INSTALL_SUCCEEDED) { "恢复未成功，系统返回 $code。系统可能已不再保留此应用的安装包。" }
      check(InstalledPackageState.isInstalled(pm, pkg)) { "核对失败，用户 0 仍未安装此包。" }
      "SUCCESS\n恢复成功，已确认用户 0 重新安装 $pkg。"
    } catch (error: Exception) {
      errorResult(error)
    }
  }

  private fun installExisting(pm: Any, pkg: String): Int {
    val api = Class.forName("android.content.pm.IPackageManager")
    val int = Int::class.javaPrimitiveType
    return try {
      // Android 10+: (packageName, userId, installFlags, installReason, whiteListedPermissions)
      api.getMethod("installExistingPackageAsUser", String::class.java, int, int, int, List::class.java)
        .invoke(pm, pkg, TARGET_USER, INSTALL_ALL_WHITELIST_RESTRICTED_PERMISSIONS, INSTALL_REASON_UNKNOWN, null) as Int
    } catch (olderApi: NoSuchMethodException) {
      // Android 9 has no restricted-permission whitelist flag.
      api.getMethod("installExistingPackageAsUser", String::class.java, int, int, int)
        .invoke(pm, pkg, TARGET_USER, 0, INSTALL_REASON_UNKNOWN) as Int
    }
  }

  /** Same hidden API and flags as the original Mac-side JAR: user 0, DELETE_SYSTEM_APP. */
  private fun deleteAndWait(pm: Any, pkg: String) {
    val descriptor = "android.content.pm.IPackageDeleteObserver2"
    val done = CountDownLatch(1)
    // Written on a binder thread; CountDownLatch publishes them to the waiting thread.
    var result = -999
    var detail: String? = ""
    val callback = object : Binder() {
      override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code == INTERFACE_TRANSACTION) {
          reply?.writeString(descriptor)
          return true
        }
        data.enforceInterface(descriptor)
        when (code) {
          2 -> { // onPackageDeleted(String packageName, int returnCode, String msg)
            data.readString()
            result = data.readInt()
            detail = data.readString()
          }
          1 -> detail = "系统要求用户操作，未执行自动确认。" // onUserActionRequired(Intent)
          else -> return false
        }
        reply?.writeNoException()
        done.countDown()
        return true
      }
    }
    val observerClass = Class.forName(descriptor)
    val observer = Proxy.newProxyInstance(observerClass.classLoader, arrayOf(observerClass)) { proxy, method, args ->
      when (method.name) {
        "asBinder" -> callback
        // Object methods may be invoked by framework logging or collections; answer them instead of crashing.
        "equals" -> proxy === args?.firstOrNull()
        "hashCode" -> System.identityHashCode(proxy)
        "toString" -> "LetheDeleteObserver"
        else -> throw UnsupportedOperationException(method.name)
      }
    }
    Class.forName("android.content.pm.IPackageManager")
      .getMethod("deletePackageVersioned", VersionedPackage::class.java, observerClass, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
      .invoke(pm, VersionedPackage(pkg, -1L), observer, TARGET_USER, DELETE_SYSTEM_APP)
    check(done.await(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "等待卸载结果超时，请刷新列表核对应用状态。" }
    check(result == DELETE_SUCCEEDED) { "卸载未成功，系统返回 $result；$detail" }
  }
}
