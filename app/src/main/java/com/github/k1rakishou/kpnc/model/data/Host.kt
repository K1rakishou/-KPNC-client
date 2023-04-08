package com.github.k1rakishou.kpnc.model.data

@JvmInline
value class Host(
  private val value: String
) {

  fun getAccountInfoEndpoint(): String {
    return "${value}/get_account_info"
  }

  fun updateFirebaseTokenEndpoint(): String {
    return "${value}/update_firebase_token"
  }

  fun sendTestPushEndpoint(): String {
    return "${value}/send_test_push"
  }

}