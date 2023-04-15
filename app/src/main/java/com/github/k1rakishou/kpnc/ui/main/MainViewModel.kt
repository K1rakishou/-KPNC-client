package com.github.k1rakishou.kpnc.ui.main

import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.k1rakishou.kpnc.AppConstants
import com.github.k1rakishou.kpnc.domain.GoogleServicesChecker
import com.github.k1rakishou.kpnc.domain.MessageReceiver
import com.github.k1rakishou.kpnc.domain.TokenUpdater
import com.github.k1rakishou.kpnc.helpers.*
import com.github.k1rakishou.kpnc.model.data.ui.UiResult
import com.github.k1rakishou.kpnc.model.data.ui.AccountInfo
import com.github.k1rakishou.kpnc.model.repository.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime

class MainViewModel(
  private val sharedPrefs: SharedPreferences,
  private val googleServicesChecker: GoogleServicesChecker,
  private val messageReceiver: MessageReceiver,
  private val tokenUpdater: TokenUpdater,
  private val accountRepository: AccountRepository,
) : ViewModel() {
  private val _rememberedUserId = mutableStateOf<String?>(null)
  val rememberedUserId: State<String?>
    get() = _rememberedUserId
  
  private val _googleServicesCheckResult = mutableStateOf<GoogleServicesChecker.Result>(GoogleServicesChecker.Result.Empty)
  val googleServicesCheckResult: State<GoogleServicesChecker.Result>
    get() = _googleServicesCheckResult

  private val _firebaseToken = mutableStateOf<UiResult<String>>(UiResult.Empty)
  val firebaseToken: State<UiResult<String>>
    get() = _firebaseToken

  private val _accountInfo = mutableStateOf<UiResult<AccountInfo>>(UiResult.Empty)
  val accountInfo: State<UiResult<AccountInfo>>
    get() = _accountInfo

  init {
    viewModelScope.launch {
      val result = googleServicesChecker.checkGoogleServicesStatus()
      _googleServicesCheckResult.value = result

      if (result != GoogleServicesChecker.Result.Success) {
        return@launch
      }

      _firebaseToken.value = UiResult.Loading
      _rememberedUserId.value = sharedPrefs.getString(AppConstants.PrefKeys.USER_ID, null)
        ?.takeIf { it.isNotBlank() }

      retrieveFirebaseToken()
        .onFailure { error -> _firebaseToken.value = UiResult.Error(error) }
        .onSuccess { token ->
          sharedPrefs.edit { putString(AppConstants.PrefKeys.TOKEN, token) }
          _firebaseToken.value = UiResult.Value(token)
        }
    }
  }

  fun login(userId: String) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        _accountInfo.value = UiResult.Loading

        val firebaseToken = sharedPrefs.getString(AppConstants.PrefKeys.TOKEN, null)
        if (firebaseToken.isNullOrEmpty()) {
          return@withContext
        }

        if (!tokenUpdater.updateToken(userId, firebaseToken)) {
          logcatError(TAG) { "updateFirebaseToken() updateToken() returned false" }
          return@withContext
        }

        val accountInfoResult = accountRepository.getAccountInfo(userId)
        if (accountInfoResult.isFailure) {
          logcatError(TAG) {
            "updateFirebaseToken() error: " +
              "${accountInfoResult.exceptionOrNull()?.asLogIfImportantOrErrorMessage()}"
          }

          _accountInfo.value = UiResult.Error(accountInfoResult.exceptionOrNull()!!)
          return@withContext
        }

        val accountInfoResponse = accountInfoResult.getOrThrow()

        val accountInfo = AccountInfo(
          isValid = accountInfoResponse.isValid,
          validUntil = DateTime(accountInfoResponse.validUntil),
        )

        sharedPrefs.edit { 
          putString(AppConstants.PrefKeys.USER_ID, userId)
        }
        _accountInfo.value = UiResult.Value(accountInfo)

        logcatDebug(TAG) { "updateFirebaseToken() success" }
      }
    }
  }

  fun logout() {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        sharedPrefs.edit {
          remove(AppConstants.PrefKeys.TOKEN)
          remove(AppConstants.PrefKeys.USER_ID)
        }

        _accountInfo.value = UiResult.Empty
        _rememberedUserId.value = null
      }
    }
  }

  companion object {
    private const val TAG = "MainViewModel"
  }

}