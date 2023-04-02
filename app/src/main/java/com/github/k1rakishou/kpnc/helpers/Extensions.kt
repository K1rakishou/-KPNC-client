package com.github.k1rakishou.kpnc.helpers

import logcat.LogPriority
import logcat.logcat

inline fun logcatError(
  tag: String,
  message: () -> String
) {
  logcat(priority = LogPriority.ERROR, tag = tag, message = message)
}

inline fun Any.logcatError(
  tag: String? = null,
  message: () -> String
) {
  logcat(priority = LogPriority.ERROR, tag = tag, message = message)
}

inline fun logcatVerbose(
  tag: String,
  message: () -> String
) {
  logcat(priority = LogPriority.VERBOSE, tag = tag, message = message)
}

inline fun Any.logcatVerbose(
  tag: String? = null,
  message: () -> String
) {
  logcat(priority = LogPriority.VERBOSE, tag = tag, message = message)
}

inline fun logcatDebug(
  tag: String,
  message: () -> String
) {
  logcat(priority = LogPriority.DEBUG, tag = tag, message = message)
}

inline fun Any.logcatDebug(
  tag: String? = null,
  message: () -> String
) {
  logcat(priority = LogPriority.DEBUG, tag = tag, message = message)
}