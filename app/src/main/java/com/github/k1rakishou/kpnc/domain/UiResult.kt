package com.github.k1rakishou.kpnc.domain

import androidx.compose.runtime.Stable

@Stable
sealed interface UiResult<out T> {
  object Empty : UiResult<Nothing>
  data class Result<T>(val value: T) : UiResult<T>
  data class Error(val throwable: Throwable) : UiResult<Nothing>
}