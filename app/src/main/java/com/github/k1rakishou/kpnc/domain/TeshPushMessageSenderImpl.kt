package com.github.k1rakishou.kpnc.domain

import android.content.SharedPreferences
import com.github.k1rakishou.kpnc.AppConstants
import com.github.k1rakishou.kpnc.data.Host
import com.github.k1rakishou.kpnc.data.SendTestPushRequest
import com.github.k1rakishou.kpnc.helpers.logcatDebug
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class TeshPushMessageSenderImpl(
  private val host: Host,
  private val moshi: Moshi,
  private val sharedPrefs: SharedPreferences,
  private val okHttpClient: OkHttpClient,
  private val tokenUpdater: TokenUpdater
) : TeshPushMessageSender {

  override suspend fun sendTestPushMessage(email: String) {
    val token = sharedPrefs.getString(AppConstants.PrefKeys.TOKEN, null)
    if (token.isNullOrEmpty()) {
      logcatDebug(TAG) { "sendTestPushMessage() token is null or empty, can't update it on the server" }
      return
    }

    tokenUpdater.updateToken(token)

    val sendTestPushRequest = SendTestPushRequest(
      email = email
    )

    val sendTestPushRequestJson = moshi.adapter(SendTestPushRequest::class.java)
      .toJson(sendTestPushRequest)

    val requestBody = sendTestPushRequestJson.toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
      .url(host.sendTestPushEndpoint())
      .post(requestBody)
      .build()

    logcatDebug(TAG) { "sendTestPushMessage() requesting for a test push..." }
    val isSuccessful = okHttpClient.newCall(request).execute().isSuccessful
    logcatDebug(TAG) { "sendTestPushMessage() requesting for a test push... Done. Success: ${isSuccessful}" }
  }

  companion object {
    private const val TAG = "TeshPushMessageSenderImpl"
  }

}