package io.github.xiaolong125.lethe.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageRulesTest {
  @Test fun rejectsShellInjection() {
    assertFalse(PackageRules.valid("com.example.app; rm -rf /"))
    assertFalse(PackageRules.valid("com.app\nother"))
    assertFalse(PackageRules.valid("android"))
    assertFalse(PackageRules.valid(null))
    assertTrue(PackageRules.valid("com.part.sunfics"))
  }

  @Test fun protectsSelfShizukuAndInvalidNames() {
    val self = "io.github.xiaolong125.lethe"
    assertTrue(PackageRules.isProtected(self, self))
    assertTrue(PackageRules.isProtected(SHIZUKU_PACKAGE, self))
    assertTrue(PackageRules.isProtected("android", self))
    assertFalse(PackageRules.isProtected("com.mobiletools.systemhelper", self))
  }
}
