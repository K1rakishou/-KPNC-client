package com.github.k1rakishou.kpnc.domain

interface TeshPushMessageSender {
  suspend fun sendTestPushMessage(email: String)
}