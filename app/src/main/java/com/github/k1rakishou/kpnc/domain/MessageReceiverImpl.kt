package com.github.k1rakishou.kpnc.domain

import com.github.k1rakishou.kpnc.helpers.logcatError
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MessageReceiverImpl : MessageReceiver, KoinComponent {
  private val clientAppNotifier: ClientAppNotifier by inject()
  private val moshi: Moshi by inject()

  override fun onGotNewMessage(messageId: String?, data: String?) {
    if (messageId.isNullOrEmpty() || data.isNullOrEmpty()) {
      logcatError(TAG) { "onGotNewMessage() invalid messageId='${messageId}', data='${data?.take(256)}'" }
      return
    }

    clientAppNotifier.onRepliesReceived(extractPostUrl(data))
  }

  private fun extractPostUrl(data: String): List<String> {
    val newFcmRepliesMessage = try {
      moshi.adapter<NewFcmRepliesMessage>(NewFcmRepliesMessage::class.java).fromJson(data)
    } catch (error: Throwable) {
      logcatError(TAG) { "Error while trying to deserialize \'$data\' as NewFcmRepliesMessage" }
      return emptyList()
    }

    if (newFcmRepliesMessage == null) {
      logcatError(TAG) { "Failed to deserialize \'$data\' as NewFcmRepliesMessage" }
      return emptyList()
    }

    return newFcmRepliesMessage.newReplyUrls
  }

  @JsonClass(generateAdapter = true)
  data class NewFcmRepliesMessage(
    @Json(name = "new_reply_urls")
    val newReplyUrls: List<String>
  )

  companion object {
    private const val TAG = "MessageProcessorImpl"
  }

}