package com.github.k1rakishou.kpnc.helpers

import com.github.k1rakishou.kpnc.model.BadStatusResponseException
import com.github.k1rakishou.kpnc.model.EmptyBodyResponseException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import okhttp3.*
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.coroutines.resume

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

suspend fun OkHttpClient.suspendCall(request: Request): Result<Response> {
  val responseResult = suspendCancellableCoroutine<Result<Response>> { continuation ->
    val call = newCall(request)

    continuation.invokeOnCancellation {
      if (!call.isCanceled()) {
        call.cancel()
      }
    }

    call.enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        continuation.resumeValueSafe(Result.failure(e))
      }

      override fun onResponse(call: Call, response: Response) {
        continuation.resumeValueSafe(Result.success(response))
      }
    })
  }

  val response = if (responseResult.isFailure) {
    return responseResult
  } else {
    responseResult.getOrThrow()
  }

  if (!response.isSuccessful) {
    return Result.failure(BadStatusResponseException(response.code))
  }

  if (response.body == null) {
    return Result.failure(EmptyBodyResponseException())
  }

  return Result.success(response)
}


fun <T> CancellableContinuation<T>.resumeValueSafe(value: T) {
  if (isActive) {
    resume(value)
  }
}

inline fun <T> Result.Companion.Try(func: () -> T): Result<T> {
  return try {
    Result.success(func())
  } catch (error: Throwable) {
    Result.failure(error)
  }
}

fun <T> Result<T>.unwrap(): T {
  return getOrThrow()
}

@JvmOverloads
fun Throwable.errorMessageOrClassName(userReadable: Boolean = false): String {
  val actualMessage = if (message?.isNotNullNorBlank() == true) {
    message
  } else {
    cause?.message
  }

  if (userReadable && actualMessage.isNotNullNorBlank()) {
    return actualMessage
  }

  if (!isExceptionImportant()) {
    return this::class.java.simpleName
  }

  val exceptionClassName = this::class.java.simpleName

  if (!actualMessage.isNullOrBlank()) {
    if (actualMessage.contains(exceptionClassName)) {
      return actualMessage
    }

    return "${exceptionClassName}: ${actualMessage}"
  }

  return exceptionClassName
}

fun Throwable.asLogIfImportantOrErrorMessage(): String {
  if (isExceptionImportant()) {
    return asLog()
  }

  return errorMessageOrClassName()
}

fun Throwable.isExceptionImportant(): Boolean {
  return when (this) {
    is CancellationException,
    is SocketTimeoutException,
    is TimeoutException,
    is InterruptedIOException,
    is InterruptedException,
    is BadStatusResponseException,
    is EmptyBodyResponseException,
    is SSLException -> false
    else -> true
  }
}


@Suppress("ReplaceSizeCheckWithIsNotEmpty", "NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
inline fun CharSequence?.isNotNullNorEmpty(): Boolean {
  contract {
    returns(true) implies (this@isNotNullNorEmpty != null)
  }

  return this != null && this.length > 0
}

@Suppress("ReplaceSizeCheckWithIsNotEmpty", "NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
inline fun CharSequence?.isNotNullNorBlank(): Boolean {
  contract {
    returns(true) implies (this@isNotNullNorBlank != null)
  }

  return this != null && this.isNotBlank()
}

@Suppress("ReplaceSizeCheckWithIsNotEmpty", "NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
inline fun String?.isNotNullNorEmpty(): Boolean {
  contract {
    returns(true) implies (this@isNotNullNorEmpty != null)
  }

  return this != null && this.length > 0
}

@Suppress("ReplaceSizeCheckWithIsNotEmpty", "NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
inline fun String?.isNotNullNorBlank(): Boolean {
  contract {
    returns(true) implies (this@isNotNullNorBlank != null)
  }

  return this != null && this.isNotBlank()
}

@Suppress("ReplaceSizeCheckWithIsNotEmpty", "NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
inline fun <T> Collection<T>?.isNotNullNorEmpty(): Boolean {
  contract {
    returns(true) implies (this@isNotNullNorEmpty != null)
  }

  return this != null && this.size > 0
}
