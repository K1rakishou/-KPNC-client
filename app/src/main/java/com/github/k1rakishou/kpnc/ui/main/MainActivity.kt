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
import com.github.k1rakishou.kpnc.helpers.asLogIfImportantOrErrorMessage
import com.github.k1rakishou.kpnc.helpers.errorMessageOrClassName
import com.github.k1rakishou.kpnc.model.data.ui.AccountInfo
import com.github.k1rakishou.kpnc.model.data.ui.UiResult
import com.github.k1rakishou.kpnc.ui.theme.KPNCTheme
import kotlinx.coroutines.flow.collect
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
          val rememberedEmail by mainViewModel.rememberedEmail

          LaunchedEffect(
            key1 = Unit,
            block = {
              mainViewModel.messageQueue.collect { message ->
                showToast(context, "(${message.messageId}): ${message.data}")
              }
            }
          )

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
                rememberedEmail = rememberedEmail,
                onLoginClicked = { email -> mainViewModel.login(email) },
                onSendTestPushClicked = { email -> mainViewModel.sendTestPush(email) },
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
  rememberedEmail: String?,
  accountInfo: UiResult<AccountInfo>,
  firebaseToken: UiResult<String>,
  onLoginClicked: (String) -> Unit,
  onSendTestPushClicked: (String) -> Unit,
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

  var email by remember { mutableStateOf(rememberedEmail ?: "") }

  TextField(
    modifier = Modifier
      .wrapContentHeight()
      .fillMaxWidth(),
    label = { Text(text = "Email") },
    value = email,
    onValueChange = { email = it }
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

  Row {
    Button(
      enabled = email.isNotEmpty() && accountInfo !is UiResult.Loading,
      onClick = { onLoginClicked(email) }
    ) {
      Text(text = "Login")
    }

    Spacer(modifier = Modifier.width(16.dp))

    Button(
      enabled = email.isNotEmpty() && accountInfo is UiResult.Value,
      onClick = { onSendTestPushClicked(email) }
    ) {
      Text(text = "Send test push")
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