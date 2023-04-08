package com.github.k1rakishou.kpnc.domain

import android.content.SharedPreferences
import com.github.k1rakishou.kpnc.AppConstants
import com.github.k1rakishou.kpnc.model.data.Host
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
  private val host: Host,
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

  override fun updateToken(token: String) {
    if (!updatingToken.compareAndSet(false, true)) {
      awaitUntilTokenUpdated()
      return
    }

    val prevLatch = synchronized(this) { updateTokenLatch }
    logcatDebug(TAG) { "updateToken()" }

    try {
      val email = sharedPrefs.getString(AppConstants.PrefKeys.EMAIL, null)
      if (email.isNullOrEmpty()) {
        logcatError(TAG) { "onNewToken() accountId is null or empty" }
        return
      }

      val updateTokenRequest = UpdateTokenRequest(
        email = email,
        firebaseToken = token
      )

      val updateTokenRequestJson = moshi.adapter(UpdateTokenRequest::class.java)
        .toJson(updateTokenRequest)

      val requestBody = updateTokenRequestJson.toRequestBody("application/json".toMediaType())

      val request = Request.Builder()
        .url(host.updateFirebaseTokenEndpoint())
        .post(requestBody)
        .build()

      logcatDebug(TAG) { "onNewToken() updating token on the server..." }
      val isSuccessful = okHttpClient.newCall(request).execute().isSuccessful
      logcatDebug(TAG) { "onNewToken() updating token on the server... Done. Success: ${isSuccessful}" }
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