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
import com.github.k1rakishou.kpnc.domain.TeshPushMessageSender
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
  private val teshPushMessageSender: TeshPushMessageSender,
  private val messageReceiver: MessageReceiver,
  private val accountRepository: AccountRepository,
) : ViewModel() {
  private val _rememberedEmail = mutableStateOf<String?>(null)
  val rememberedEmail: State<String?>
    get() = _rememberedEmail
  
  private val _googleServicesCheckResult = mutableStateOf<GoogleServicesChecker.Result>(GoogleServicesChecker.Result.Empty)
  val googleServicesCheckResult: State<GoogleServicesChecker.Result>
    get() = _googleServicesCheckResult

  private val _firebaseToken = mutableStateOf<UiResult<String>>(UiResult.Empty)
  val firebaseToken: State<UiResult<String>>
    get() = _firebaseToken

  private val _accountInfo = mutableStateOf<UiResult<AccountInfo>>(UiResult.Empty)
  val accountInfo: State<UiResult<AccountInfo>>
    get() = _accountInfo

  val messageQueue = messageReceiver.messageQueue

  init {
    viewModelScope.launch {
      val result = googleServicesChecker.checkGoogleServicesStatus()
      _googleServicesCheckResult.value = result

      if (result != GoogleServicesChecker.Result.Success) {
        return@launch
      }

      _firebaseToken.value = UiResult.Loading
      _rememberedEmail.value = sharedPrefs.getString(AppConstants.PrefKeys.EMAIL, null)
        ?.takeIf { it.isNotBlank() }

      retrieveFirebaseToken()
        .onFailure { error -> _firebaseToken.value = UiResult.Error(error) }
        .onSuccess { token ->
          sharedPrefs.edit { putString(AppConstants.PrefKeys.TOKEN, token) }
          _firebaseToken.value = UiResult.Value(token)
        }
    }
  }

  fun login(email: String) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        _accountInfo.value = UiResult.Loading

        val accountInfoResult = accountRepository.getAccountInfo(email)
        if (accountInfoResult.isFailure) {
          logcatError(TAG) {
            "getAccountInfo() error: " +
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
          putString(AppConstants.PrefKeys.EMAIL, email) 
        }
        _accountInfo.value = UiResult.Value(accountInfo)

        logcatDebug(TAG) { "getAccountInfo() success" }
      }
    }
  }

  fun sendTestPush(email: String) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        teshPushMessageSender.sendTestPushMessage(email)
      }
    }
  }

  companion object {
    private const val TAG = "MainViewModel"
  }

}