package com.github.k1rakishou.kpnc.model.repository

import com.github.k1rakishou.kpnc.helpers.Try
import com.github.k1rakishou.kpnc.helpers.suspendCall
import com.github.k1rakishou.kpnc.helpers.unwrap
import com.github.k1rakishou.kpnc.model.GenericClientException
import com.github.k1rakishou.kpnc.model.JsonConversionException
import com.github.k1rakishou.kpnc.model.data.network.AccountInfoRequest
import com.github.k1rakishou.kpnc.model.data.network.AccountInfoResponse
import com.github.k1rakishou.kpnc.model.data.Host
import com.github.k1rakishou.kpnc.model.data.network.AccountInfoResponseWrapper
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AccountRepositoryImpl(
  private val host: Host,
  private val okHttpClient: OkHttpClient,
  private val moshi: Moshi
) : AccountRepository {

  override suspend fun getAccountInfo(accountId: String): Result<AccountInfoResponse> {
    return Result.Try {
      val accountInfoRequest = AccountInfoRequest(accountId)

      val updateTokenRequestJson = moshi.adapter(AccountInfoRequest::class.java)
        .toJson(accountInfoRequest)

      val requestBody = updateTokenRequestJson.toRequestBody("application/json".toMediaType())

      val request = Request.Builder()
        .url(host.getAccountInfoEndpoint())
        .post(requestBody)
        .build()

      val responseBody = okHttpClient.suspendCall(request).unwrap().body!!

      val accountInfoResponseWrapper = responseBody.source().use { source ->
        moshi
          .adapter<AccountInfoResponseWrapper>(AccountInfoResponseWrapper::class.java)
          .fromJson(source)
      }

      if (accountInfoResponseWrapper == null) {
        throw JsonConversionException("Failed to convert response body into AccountInfoResponse")
      }

      val accountInfoResponse = accountInfoResponseWrapper.dataOrThrow()
      if (!accountInfoResponse.isValid) {
        throw GenericClientException("accountInfoResponse is not valid")
      }

      return@Try accountInfoResponse
    }
  }

}