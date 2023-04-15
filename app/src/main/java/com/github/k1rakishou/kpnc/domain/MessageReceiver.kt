package com.github.k1rakishou.kpnc.domain


interface MessageReceiver {
  fun onGotNewMessage(messageId: String?, data: String?)

  data class Message(
    val messageId: String,
    val data: String
  )
}