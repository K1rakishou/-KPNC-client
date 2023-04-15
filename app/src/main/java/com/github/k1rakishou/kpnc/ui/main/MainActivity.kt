package com.github.k1rakishou.kpnc.ui.main

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.kpnc.domain.GoogleServicesChecker
import com.github.k1rakishou.kpnc.helpers.errorMessageOrClassName
import com.github.k1rakishou.kpnc.helpers.isUserIdValid
import com.github.k1rakishou.kpnc.model.data.ui.AccountInfo
import com.github.k1rakishou.kpnc.model.data.ui.UiResult
import com.github.k1rakishou.kpnc.ui.theme.KPNCTheme
import logcat.asLog
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent

class MainActivity : ComponentActivity(), KoinComponent {
  private val mainViewModel: MainViewModel by viewModel<MainViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      KPNCTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colors.background
        ) {
          val context = LocalContext.current

          val firebaseToken by mainViewModel.firebaseToken
          val googleServicesCheckResult by mainViewModel.googleServicesCheckResult
          val accountInfo by mainViewModel.accountInfo
          val rememberedUserId by mainViewModel.rememberedUserId

          Box(modifier = Modifier.fillMaxSize()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp)
            ) {
              Content(
                googleServicesCheckResult = googleServicesCheckResult,
                firebaseToken = firebaseToken,
                accountInfo = accountInfo,
                rememberedUserId = rememberedUserId,
                onLogin = { userId -> mainViewModel.login(userId) },
                onLogout = { mainViewModel.logout() }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ColumnScope.Content(
  googleServicesCheckResult: GoogleServicesChecker.Result,
  rememberedUserId: String?,
  accountInfo: UiResult<AccountInfo>,
  firebaseToken: UiResult<String>,
  onLogin: (String) -> Unit,
  onLogout: () -> Unit
) {
  val context = LocalContext.current

  if (googleServicesCheckResult == GoogleServicesChecker.Result.Empty) {
    Text(text = "Checking for Google services availability...")
    return
  }

  val googleServicesCheckResultText = when (googleServicesCheckResult) {
    GoogleServicesChecker.Result.Empty -> return
    GoogleServicesChecker.Result.Success -> "Google services detected"
    GoogleServicesChecker.Result.ServiceMissing -> "Google services are missing"
    GoogleServicesChecker.Result.ServiceUpdating -> "Google services are currently updating"
    GoogleServicesChecker.Result.ServiceUpdateRequired -> "Google services need to be updated"
    GoogleServicesChecker.Result.ServiceDisabled -> "Google services are disabled"
    GoogleServicesChecker.Result.ServiceInvalid -> "Google services are not working correctly"
    GoogleServicesChecker.Result.Unknown -> "Google services unknown error"
  }

  Text(text = googleServicesCheckResultText)
  Spacer(modifier = Modifier.height(4.dp))

  if (googleServicesCheckResult != GoogleServicesChecker.Result.Success) {
    return
  }

  if (firebaseToken is UiResult.Empty || firebaseToken is UiResult.Loading) {
    Text(text = "Loading firebase token...")
    return
  }

  if (firebaseToken is UiResult.Error) {
    Text(text = "Failed to load firebase token, error: ${firebaseToken.throwable.asLog()}")
    return
  }

  firebaseToken as UiResult.Value
  Text(text = "Firebase token: ${firebaseToken.value}")
  Spacer(modifier = Modifier.height(16.dp))

  var userId by remember { mutableStateOf(rememberedUserId ?: "") }
  var isError by remember { mutableStateOf(!isUserIdValid(userId)) }
  val isLoggedIn = ((accountInfo as? UiResult.Value)?.value?.isValid == true) || isUserIdValid(rememberedUserId)

  TextField(
    modifier = Modifier
      .wrapContentHeight()
      .fillMaxWidth(),
    enabled = !isLoggedIn,
    label = { Text(text = "UserId (${userId.length}/128)") },
    isError = isError,
    value = userId,
    onValueChange = {
      userId = it
      isError = !isUserIdValid(userId)

      if (isError) {
        showToast(context, "UserId length must be within 32..128 characters range")
      }
    }
  )

  Spacer(modifier = Modifier.height(4.dp))

  if (accountInfo is UiResult.Value) {
    Text(text = "Account info: ${accountInfo.value}")
  } else if (accountInfo is UiResult.Error) {
    LaunchedEffect(
      key1 = accountInfo.throwable,
      block = {
        showToast(
          context = context,
          message = accountInfo.throwable.errorMessageOrClassName(userReadable = true)
        )
      }
    )
  }

  Spacer(modifier = Modifier.height(4.dp))

  val buttonEnabled = if (isLoggedIn) {
    true
  } else {
    isUserIdValid(userId) && accountInfo !is UiResult.Loading
  }

  Row {
    Button(
      enabled = buttonEnabled,
      onClick = {
        if (isLoggedIn) {
          userId = ""
          onLogout()
        } else {
          onLogin(userId)
        }
      }
    ) {
      if (isLoggedIn) {
        Text(text = "Logout")
      } else {
        Text(text = "Login")
      }
    }
  }
}

private var prevToast: Toast? = null

private fun showToast(
  context: Context,
  message: String
) {
  prevToast?.cancel()

  val toast = Toast.makeText(
    context,
    message,
    Toast.LENGTH_LONG
  )

  prevToast = toast
  toast.show()
}