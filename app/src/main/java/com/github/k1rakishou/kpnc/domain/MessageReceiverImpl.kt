package com.github.k1rakishou.kpnc.domain

import com.github.k1rakishou.kpnc.helpers.logcatError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MessageReceiverImpl : MessageReceiver {
  private val _messageQueue = MutableSharedFlow<MessageReceiver.Message>(extraBufferCapacity = Channel.UNLIMITED)
  override val messageQueue: SharedFlow<MessageReceiver.Message>
    get() = _messageQueue.asSharedFlow()

  override fun onGotNewMessage(messageId: String?, data: String?) {
    if (messageId.isNullOrEmpty() || data.isNullOrEmpty()) {
      logcatError(TAG) { "onGotNewMessage() invalid messageId='${messageId}', data='${data?.take(256)}'" }
      return
    }

    val message = MessageReceiver.Message(messageId, data)
    _messageQueue.tryEmit(message)
  }

  companion object {
    private const val TAG = "MessageProcessorImpl"
  }

}