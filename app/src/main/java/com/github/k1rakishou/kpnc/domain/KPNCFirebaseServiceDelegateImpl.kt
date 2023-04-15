package com.github.k1rakishou.kpnc.domain

import android.content.SharedPreferences
import androidx.core.content.edit
import com.github.k1rakishou.kpnc.AppConstants
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.Executors

class KPNCFirebaseServiceDelegateImpl(
  private val sharedPrefs: SharedPreferences,
  private val tokenUpdater: TokenUpdater,
  private val messageProcessor: MessageReceiver
) : KPNCFirebaseServiceDelegate {
  private val executor = Executors.newSingleThreadExecutor()

  init {
    executor.execute {
      sharedPrefs.getString(AppConstants.PrefKeys.TOKEN, null)
        ?.let { token -> tokenUpdater.updateToken(null, token) }
    }
  }

  override fun onNewToken(token: String) {
    sharedPrefs.edit { putString(AppConstants.PrefKeys.TOKEN, token) }

    tokenUpdater.reset()
    tokenUpdater.updateToken(null, token)
  }

  override fun onMessageReceived(message: RemoteMessage) {
    tokenUpdater.awaitUntilTokenUpdated()

    messageProcessor.onGotNewMessage(
      messageId = message.messageId,
      data = message.data[AppConstants.MessageKeys.MESSAGE_BODY]
    )
  }

  companion object {
    private const val TAG = "KPNCFirebaseServiceDelegateImpl"
  }

}