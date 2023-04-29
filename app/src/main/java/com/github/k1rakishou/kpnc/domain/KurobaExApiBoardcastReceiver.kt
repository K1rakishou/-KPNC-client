package com.github.k1rakishou.kpnc.domain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.os.bundleOf
import com.github.k1rakishou.kpnc.AppConstants
import com.github.k1rakishou.kpnc.helpers.asLogIfImportantOrErrorMessage
import com.github.k1rakishou.kpnc.helpers.errorMessageOrClassName
import com.github.k1rakishou.kpnc.helpers.isNotNullNorBlank
import com.github.k1rakishou.kpnc.helpers.isUserIdValid
import com.github.k1rakishou.kpnc.helpers.logcatDebug
import com.github.k1rakishou.kpnc.helpers.logcatError
import com.github.k1rakishou.kpnc.model.repository.AccountRepository
import com.github.k1rakishou.kpnc.model.repository.PostRepository
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class KurobaExApiBoardcastReceiver : BroadcastReceiver(), KoinComponent {
  private val moshi: Moshi by inject<Moshi>()
  private val postRepository: PostRepository by inject<PostRepository>()
  private val accountRepository: AccountRepository by inject<AccountRepository>()
  private val sharedPrefs: SharedPreferences by inject<SharedPreferences>()

  private val coroutineScope = MainScope()

  init {
    logcatDebug(TAG) { "Initialized" }
  }

  override fun onReceive(context: Context?, intent: Intent?) {
    if (intent == null || context == null) {
      logcatDebug(TAG) { "intent == null || context == null" }
      return
    }

    val action = intent.action

    if (!isActionSupported(action)) {
      logcatDebug(TAG) { "Action is not supported: ${action}" }
      return
    }

    val pendingResult = goAsync()

    coroutineScope.launch {
      try {
        withTimeout(30_000) {
          when (action) {
            ACTION_GET_INFO -> {
              handleGetInfo(pendingResult)
            }
            ACTION_START_WATCHING_POST -> {
              handleStartWatchingPost(intent, pendingResult)
            }
            else -> {
              logcatDebug(TAG) { "Unknown action: ${action}" }
            }
          }
        }
      } catch (error: Throwable) {
        logcatError(TAG) { "Error: ${error.asLogIfImportantOrErrorMessage()}" }
      } finally {
        pendingResult.finish()
      }
    }
  }

  private suspend fun handleStartWatchingPost(intent: Intent, pendingResult: PendingResult) {
    logcatDebug(TAG) { "Got ACTION_START_WATCHING_POST" }

    val postUrl = intent.getStringExtra(PARAM_POST_URL)
    if (postUrl.isNullOrBlank()) {
      logcatError(TAG) { "handleStartWatchingPost() Post url was not provided" }
      return
    }

    val watchPostResult = postRepository.watchPost(postUrl)

    val watchPostClientResult = if (watchPostResult.isFailure) {
      val exception = watchPostResult.exceptionOrNull()!!
      logcatError(TAG) { "handleStartWatchingPost(${postUrl}) error: ${exception.asLogIfImportantOrErrorMessage()}" }

      WatchPostResult(
        error = exception.errorMessageOrClassName(userReadable = true)
      )
    } else {
      WatchPostResult(data = DefaultSuccessResult())
    }

    val watchPostResultJson = moshi
      .adapter<WatchPostResult>(WatchPostResult::class.java)
      .toJson(watchPostClientResult)

    val resultBundle = bundleOf(START_WATCHING_POST_RESULT to watchPostResultJson)
    pendingResult.setResultExtras(resultBundle)

    logcatDebug(TAG) { "ACTION_START_WATCHING_POST done" }
  }

  private suspend fun handleGetInfo(pendingResult: PendingResult) {
    logcatDebug(TAG) { "Got ACTION_GET_INFO" }

    val userId = sharedPrefs.getString(AppConstants.PrefKeys.USER_ID, null)
      ?.takeIf { userId -> isUserIdValid(userId) }
    val instanceAddress = sharedPrefs.getString(AppConstants.PrefKeys.INSTANCE_ADDRESS, null)
      ?.takeIf { instanceAddress -> instanceAddress.isNotNullNorBlank() }

    val kpncInfoResult = if (instanceAddress != null && userId != null) {
      logcatDebug(TAG) { "InstanceAddress and UserId are OK" }

      val accountInfoResult = accountRepository.getAccountInfo(instanceAddress, userId)
      if (accountInfoResult.isFailure) {
        val exception = accountInfoResult.exceptionOrNull()!!
        logcatDebug(TAG) { "getAccountInfo() error: ${exception.asLogIfImportantOrErrorMessage()}" }

        KPNCInfoResult(
          error = exception.errorMessageOrClassName(userReadable = true)
        )
      } else {
        val isValid = accountInfoResult.getOrNull()!!.isValid
        logcatDebug(TAG) { "getAccountInfo() success, isValid: ${isValid}" }

        KPNCInfoResult(
          data = KPNCInfoJson(appApiVersion = API_VERSION, isAccountValid = isValid)
        )
      }
    } else {
      logcatDebug(TAG) { "Bad userId: ${userId} or instanceAddress: ${instanceAddress}" }

      KPNCInfoResult(
        data = KPNCInfoJson(appApiVersion = API_VERSION, isAccountValid = false)
      )
    }

    val json = moshi
      .adapter<KPNCInfoResult>(KPNCInfoResult::class.java)
      .toJson(kpncInfoResult)

    val resultBundle = bundleOf(GET_INFO_RESULT to json)
    pendingResult.setResultExtras(resultBundle)

    logcatDebug(TAG) { "ACTION_GET_INFO done" }
  }

  private fun isActionSupported(action: String?): Boolean {
    return action == ACTION_GET_INFO || action == ACTION_START_WATCHING_POST
  }

  @JsonClass(generateAdapter = true)
  data class KPNCInfoResult(
    override val data: KPNCInfoJson? = null,
    override val error: String? = null
  ) : GenericResult<KPNCInfoJson>()

  @JsonClass(generateAdapter = true)
  data class WatchPostResult(
    override val data: DefaultSuccessResult? = null,
    override val error: String? = null
  ) : GenericResult<DefaultSuccessResult>()

  @JsonClass(generateAdapter = true)
  data class KPNCInfoJson(
    @Json(name = "app_api_version")
    val appApiVersion: Int,
    @Json(name = "is_account_valid")
    val isAccountValid: Boolean
  )

  abstract class GenericResult<T> {
    abstract val data: T?
    abstract val error: String?
  }

  @JsonClass(generateAdapter = true)
  data class DefaultSuccessResult(
    val success: Boolean = true
  )

  companion object {
    private const val TAG = "KurobaExApiBoardcastReceiver"

    private const val API_VERSION = 1
    private const val PACKAGE = "com.github.k1rakishou.kpnc"

    private const val ACTION_GET_INFO = "${PACKAGE}.get_info"
    private const val GET_INFO_RESULT = "${PACKAGE}.get_info_result"

    private const val ACTION_START_WATCHING_POST = "${PACKAGE}.start_watching_post"
    private const val PARAM_POST_URL = "post_url"
    private const val START_WATCHING_POST_RESULT = "${PACKAGE}.start_watching_post_result"
  }

}