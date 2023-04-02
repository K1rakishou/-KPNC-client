package com.github.k1rakishou.kpnc.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.kpnc.domain.GoogleServicesChecker
import com.github.k1rakishou.kpnc.domain.UiResult
import com.github.k1rakishou.kpnc.ui.theme.KPNCTheme
import logcat.asLog
import logcat.logcat
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
          val firebaseToken by mainViewModel.firebaseToken
          val googleServicesCheckResult by mainViewModel.googleServicesCheckResult

          Box(modifier = Modifier.fillMaxSize()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
            ) {
              Content(
                googleServicesCheckResult = googleServicesCheckResult,
                firebaseToken = firebaseToken,
                onSendTestPushButtonClicked = { email -> mainViewModel.sendTestPush(email) }
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
  firebaseToken: UiResult<String>,
  onSendTestPushButtonClicked: (String) -> Unit
) {
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

  if (firebaseToken is UiResult.Empty) {
    Text(text = "Loading firebase token...")
    return
  }

  if (firebaseToken is UiResult.Error) {
    Text(text = "Failed to load firebase token, error: ${firebaseToken.throwable.asLog()}")
    return
  }

  firebaseToken as UiResult.Result
  Text(text = "Firebase token: ${firebaseToken.value}")
  Spacer(modifier = Modifier.height(4.dp))

  var email by remember { mutableStateOf("") }

  TextField(
    modifier = Modifier
      .wrapContentHeight()
      .fillMaxWidth(),
    value = email,
    onValueChange = { email = it }
  )

  Spacer(modifier = Modifier.height(4.dp))

  Button(
    enabled = email.isNotEmpty(),
    onClick = { onSendTestPushButtonClicked(email) }
  ) {
    Text(text = "Send test push")
  }
}