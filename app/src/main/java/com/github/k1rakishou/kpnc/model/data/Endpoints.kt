package com.github.k1rakishou.kpnc.model.data

@JvmInline
value class Endpoints(
  private val value: String
) {

  fun getAccountInfoEndpoint(): String {
    return "${value}/get_account_info"
  }

  fun updateFirebaseTokenEndpoint(): String {
    return "${value}/update_firebase_token"
  }

  fun watchPost(): String {
    return "${value}/watch_post"
  }

  fun sendTestPushEndpoint(): String {
    return "${value}/send_test_push"
  }

}