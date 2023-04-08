package com.github.k1rakishou.kpnc.model.data.ui

import androidx.compose.runtime.Immutable
import org.joda.time.DateTime

@Immutable
data class AccountInfo(
  val isValid: Boolean,
  val validUntil: DateTime
)