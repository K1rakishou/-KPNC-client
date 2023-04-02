package com.github.k1rakishou.kpnc.domain

interface MessageProcessor {
  fun onGotNewMessage(messageId: String?, data: String?)
}