package com.github.k1rakishou.kpnc.data

@JvmInline
value class Host(
  private val value: String
) {

  fun updateFirebaseTokenEndpoint(): String {
    return "${value}/update_firebase_token"
  }

  fun sendTestPushEndpoint(): String {
    return "${value}/send_test_push"
  }

}