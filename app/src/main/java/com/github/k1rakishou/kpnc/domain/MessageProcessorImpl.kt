package com.github.k1rakishou.kpnc.domain

import com.github.k1rakishou.kpnc.helpers.logcatDebug

class MessageProcessorImpl : MessageProcessor {

  override fun onGotNewMessage(messageId: String?, data: String?) {
    logcatDebug(TAG) { "onGotNewMessage() messageId='${messageId}', data='${data?.take(256)}'" }
  }

  companion object {
    private const val TAG = "MessageProcessorImpl"
  }

}