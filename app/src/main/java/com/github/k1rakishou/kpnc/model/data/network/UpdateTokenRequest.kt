package com.github.k1rakishou.kpnc.model.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateTokenRequest(
  @Json(name = "email")
  val email: String,
  @Json(name = "firebase_token")
  val firebaseToken: String
)