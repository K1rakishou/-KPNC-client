package com.github.k1rakishou.kpnc.model.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SendTestPushRequest(
  @Json(name = "email")
  val email: String
)