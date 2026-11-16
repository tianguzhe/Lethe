package io.github.xiaolong125.lethe.core

/**
 * Client-side view of the [io.github.xiaolong125.lethe.service.UninstallService] line protocol:
 * first line is a status keyword, the rest is a human-readable message.
 *
 * @property done true when the requested end state is confirmed (removed, or restored), including "already so".
 */
data class OperationOutcome(val done: Boolean, val message: String) {
  companion object {
    private val DONE_STATUSES = setOf("SUCCESS", "ALREADY_ABSENT", "ALREADY_INSTALLED")

    fun parse(raw: String): OperationOutcome {
      val newline = raw.indexOf('\n')
      if (newline < 0) return OperationOutcome(false, raw)
      return OperationOutcome(raw.substring(0, newline) in DONE_STATUSES, raw.substring(newline + 1))
    }
  }
}
