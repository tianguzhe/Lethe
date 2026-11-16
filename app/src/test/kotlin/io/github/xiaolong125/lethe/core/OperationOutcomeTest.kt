package io.github.xiaolong125.lethe.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationOutcomeTest {
  @Test fun successIsDone() {
    val outcome = OperationOutcome.parse("SUCCESS\n卸载成功")
    assertTrue(outcome.done)
    assertEquals("卸载成功", outcome.message)
  }

  @Test fun alreadyInTargetStateIsDone() {
    assertTrue(OperationOutcome.parse("ALREADY_ABSENT\n无需卸载").done)
    assertTrue(OperationOutcome.parse("ALREADY_INSTALLED\n无需恢复").done)
  }

  @Test fun errorKeepsMultilineDetail() {
    val outcome = OperationOutcome.parse("ERROR\nIllegalStateException: 核对失败\n仍安装")
    assertFalse(outcome.done)
    assertEquals("IllegalStateException: 核对失败\n仍安装", outcome.message)
  }

  @Test fun unknownOrMalformedIsNotDone() {
    assertFalse(OperationOutcome.parse("SUCCESS").done)
    assertFalse(OperationOutcome.parse("SUCCESSFUL\nx").done)
    assertFalse(OperationOutcome.parse("").done)
  }
}
