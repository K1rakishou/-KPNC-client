package com.github.k1rakishou.kpnc.model.repository

import com.github.k1rakishou.kpnc.helpers.Try
import com.github.k1rakishou.kpnc.helpers.suspendCall
import com.github.k1rakishou.kpnc.helpers.unwrap
import com.github.k1rakishou.kpnc.model.GenericClientException
import com.github.k1rakishou.kpnc.model.JsonConversionException
import com.github.k1rakishou.kpnc.model.data.network.AccountInfoRequest
import com.github.k1rakishou.kpnc.model.data.network.AccountInfoResponse
import com.github.k1rakishou.kpnc.model.data.Endpoints
import com.github.k1rakishou.kpnc.model.data.network.AccountInfoResponseWrapper
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AccountRepositoryImpl(
  private val endpoints: Endpoints,
  private val okHttpClient: OkHttpClient,
  private val moshi: Moshi
) : AccountRepository {

  override suspend fun getAccountInfo(userId: String): Result<AccountInfoResponse> {
    return withContext(Dispatchers.IO) {
      return@withContext Result.Try {
        val accountInfoRequest = AccountInfoRequest(userId)

        val updateTokenRequestJson = moshi.adapter(AccountInfoRequest::class.java)
          .toJson(accountInfoRequest)

        val requestBody = updateTokenRequestJson.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
          .url(endpoints.getAccountInfoEndpoint())
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

}