package io.github.xiaolong125.lethe.core

const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

object PackageRules {
  private val PACKAGE_NAME = Regex("[A-Za-z0-9_]+([.][A-Za-z0-9_]+)+")

  /** Rejects anything that is not a dotted package name, so no shell metacharacters can ever reach the service. */
  fun valid(name: String?): Boolean = name != null && PACKAGE_NAME.matches(name)

  /** This tool and Shizuku itself must never be removed through the tool: doing so would cut the branch it sits on. */
  fun isProtected(pkg: String, selfPackage: String): Boolean =
    pkg == selfPackage || pkg == SHIZUKU_PACKAGE || !valid(pkg)
}
