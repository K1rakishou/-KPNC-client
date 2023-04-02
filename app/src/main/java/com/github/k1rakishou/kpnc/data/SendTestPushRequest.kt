package com.github.k1rakishou.kpnc.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SendTestPushRequest(
  val email: String
)