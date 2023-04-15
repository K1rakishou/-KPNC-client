package com.github.k1rakishou.kpnc.domain

import android.content.SharedPreferences
import com.github.k1rakishou.kpnc.AppConstants
import com.github.k1rakishou.kpnc.model.data.Endpoints
import com.github.k1rakishou.kpnc.model.data.network.UpdateTokenRequest
import com.github.k1rakishou.kpnc.helpers.logcatDebug
import com.github.k1rakishou.kpnc.helpers.logcatError
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

class TokenUpdaterImpl(
  private val sharedPrefs: SharedPreferences,
  private val endpoints: Endpoints,
  private val moshi: Moshi,
  private val okHttpClient: OkHttpClient
) : TokenUpdater {
  private var updateTokenLatch = CountDownLatch(1)
  private val updatingToken = AtomicBoolean(false)

  override fun reset() {
    if (updatingToken.get()) {
      return
    }

    logcatDebug(TAG) { "reset()" }

    synchronized(this) {
      updateTokenLatch = CountDownLatch(1)
      updatingToken.set(false)
    }
  }

  override fun updateToken(userId: String?, token: String): Boolean {
    if (!updatingToken.compareAndSet(false, true)) {
      awaitUntilTokenUpdated()
      return false
    }

    val prevLatch = synchronized(this) { updateTokenLatch }
    logcatDebug(TAG) { "updateToken()" }

    try {
      var userIdToUse = userId
      if (userIdToUse.isNullOrEmpty()) {
        userIdToUse = sharedPrefs.getString(AppConstants.PrefKeys.USER_ID, null)
        if (userIdToUse.isNullOrEmpty()) {
          logcatError(TAG) { "onNewToken() accountId is null or empty" }
          return false
        }
      }

      val updateTokenRequest = UpdateTokenRequest(
        userId = userIdToUse,
        firebaseToken = token
      )

      val updateTokenRequestJson = moshi.adapter(UpdateTokenRequest::class.java)
        .toJson(updateTokenRequest)

      val requestBody = updateTokenRequestJson.toRequestBody("application/json".toMediaType())

      val request = Request.Builder()
        .url(endpoints.updateFirebaseTokenEndpoint())
        .post(requestBody)
        .build()

      logcatDebug(TAG) { "onNewToken() updating token on the server..." }
      // TODO: check for actual server response, not just status code
      val isSuccessful = okHttpClient.newCall(request).execute().isSuccessful
      logcatDebug(TAG) { "onNewToken() updating token on the server... Done. Success: ${isSuccessful}" }

      return true
    } finally {
      synchronized(this) {
        if (prevLatch === updateTokenLatch) {
          updateTokenLatch.countDown()
        }
      }

      updatingToken.set(false)
    }
  }

  override fun awaitUntilTokenUpdated() {
    val latch = synchronized(this) { updateTokenLatch }
    latch.await()
  }

  companion object {
    private const val TAG = "TokenUpdaterImpl"
  }
}