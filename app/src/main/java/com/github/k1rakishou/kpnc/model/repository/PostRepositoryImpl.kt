package com.github.k1rakishou.kpnc.model.repository

import android.content.SharedPreferences
import com.github.k1rakishou.kpnc.AppConstants
import com.github.k1rakishou.kpnc.helpers.*
import com.github.k1rakishou.kpnc.model.GenericClientException
import com.github.k1rakishou.kpnc.model.data.Endpoints
import com.github.k1rakishou.kpnc.model.data.network.WatchPostRequest
import com.github.k1rakishou.kpnc.model.data.network.WatchPostResponseWrapper
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class PostRepositoryImpl(
  private val sharedPrefs: SharedPreferences,
  private val endpoints: Endpoints,
  private val moshi: Moshi,
  private val okHttpClient: OkHttpClient
) : PostRepository {

  override suspend fun watchPost(postUrl: String): Result<Boolean> {
    return withContext(Dispatchers.IO) {
      return@withContext Result.Try {
        val userId = sharedPrefs.getString(AppConstants.PrefKeys.USER_ID, null)
          ?.takeIf { it.isNotNullNorBlank() }
          ?: throw GenericClientException("UserId is not set")

        val watchPostRequest = WatchPostRequest(
          userId = userId,
          postUrl = postUrl
        )

        val watchPostRequestJson = moshi.adapter(WatchPostRequest::class.java)
          .toJson(watchPostRequest)

        val requestBody = watchPostRequestJson.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
          .url(endpoints.watchPost())
          .post(requestBody)
          .build()

        val watchPostResponseWrapperAdapter = moshi
          .adapter<WatchPostResponseWrapper>(WatchPostResponseWrapper::class.java)

        okHttpClient.suspendConvertWithJsonAdapter(
          request = request,
          adapter = watchPostResponseWrapperAdapter
        )
          .unwrap()
          .dataOrThrow()
          .ensureSuccess()

        return@Try true
      }
    }
  }

  override suspend fun unwatchPost(postUrl: String): Result<Boolean> {
    TODO("Not yet implemented")
  }

  companion object {
    private const val TAG = "PostRepositoryImpl"
  }

}