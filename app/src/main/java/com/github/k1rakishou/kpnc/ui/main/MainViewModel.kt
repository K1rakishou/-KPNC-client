package com.github.k1rakishou.kpnc.ui.main

import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.k1rakishou.kpnc.AppConstants
import com.github.k1rakishou.kpnc.domain.GoogleServicesChecker
import com.github.k1rakishou.kpnc.domain.TeshPushMessageSender
import com.github.k1rakishou.kpnc.domain.TokenUpdater
import com.github.k1rakishou.kpnc.domain.UiResult
import com.github.k1rakishou.kpnc.helpers.retrieveFirebaseToken
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
  private val sharedPrefs: SharedPreferences,
  private val googleServicesChecker: GoogleServicesChecker,
  private val teshPushMessageSender: TeshPushMessageSender
) : ViewModel() {

  private val _googleServicesCheckResult = mutableStateOf<GoogleServicesChecker.Result>(GoogleServicesChecker.Result.Empty)
  val googleServicesCheckResult: State<GoogleServicesChecker.Result>
    get() = _googleServicesCheckResult

  private val _firebaseToken = mutableStateOf<UiResult<String>>(UiResult.Empty)
  val firebaseToken: State<UiResult<String>>
    get() = _firebaseToken

  init {
    viewModelScope.launch {
      val result = googleServicesChecker.checkGoogleServicesStatus()
      _googleServicesCheckResult.value = result

      if (result != GoogleServicesChecker.Result.Success) {
        return@launch
      }

      retrieveFirebaseToken()
        .onFailure { error -> _firebaseToken.value = UiResult.Error(error) }
        .onSuccess { token ->
          sharedPrefs.edit { putString(AppConstants.PrefKeys.TOKEN, token) }
          _firebaseToken.value = UiResult.Result(token)
        }
    }
  }

  fun sendTestPush(email: String) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        sharedPrefs.edit { putString(AppConstants.PrefKeys.EMAIL, email) }
        teshPushMessageSender.sendTestPushMessage(email)
      }
    }
  }

}