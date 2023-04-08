package com.github.k1rakishou.kpnc.domain

import kotlinx.coroutines.flow.SharedFlow

interface MessageReceiver {
  val messageQueue: SharedFlow<Message>

  fun onGotNewMessage(messageId: String?, data: String?)

  data class Message(
    val messageId: String,
    val data: String
  )
}