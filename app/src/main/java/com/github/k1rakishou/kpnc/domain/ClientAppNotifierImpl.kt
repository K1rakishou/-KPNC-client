package com.github.k1rakishou.kpnc.domain

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.github.k1rakishou.kpnc.helpers.Try
import com.github.k1rakishou.kpnc.helpers.logcatDebug
import com.github.k1rakishou.kpnc.helpers.sendOrderedBroadcastBlocking

class ClientAppNotifierImpl(
  private val appContext: Context,
) : ClientAppNotifier {

  override fun onRepliesReceived(postUrls: List<String>): Result<Unit> {
    return Result.Try {
      logcatDebug(TAG) { "onRepliesReceived() postUrls: ${postUrls.size}" }

      if (postUrls.isEmpty()) {
        return@Try
      }

      postUrls.forEach { postUrl ->
        logcatDebug(TAG) { "onRepliesReceived() postUrl: ${postUrl}" }
      }

      onRepliesReceivedInternal(postUrls)
    }
  }

  private fun onRepliesReceivedInternal(postUrls: List<String>) {
    val intent = Intent(ACTION_ON_NEW_REPLIES_RECEIVED)
    intent.putExtra(POST_URLS_PARAM, postUrls.toTypedArray())

    sendBroadcastInternal(appContext, intent)
  }

  private fun sendBroadcastInternal(context: Context, intent: Intent) {
    val broadcastReceiversInfo = context.packageManager.queryBroadcastReceivers(intent, 0)
    logcat.logcat(TAG) { "broadcastReceiversInfo=${broadcastReceiversInfo.size}" }

    for (broadcastReceiver in broadcastReceiversInfo) {
      logcat.logcat(TAG) {
        "Using packageName: ${broadcastReceiver.activityInfo.packageName}, " +
          "name: ${broadcastReceiver.activityInfo.name}"
      }

      intent.component = ComponentName(
        broadcastReceiver.activityInfo.packageName,
        broadcastReceiver.activityInfo.name
      )

      logcatDebug(TAG) { "sendOrderedBroadcastSuspend() start..." }
      sendOrderedBroadcastBlocking(context, intent, null)
      logcatDebug(TAG) { "sendOrderedBroadcastSuspend() end..." }
    }
  }

  companion object {
    private const val TAG = "ClientAppNotifierImpl"
    private const val PACKAGE = "com.github.k1rakishou.kurobaexlite"

    private const val ACTION_ON_NEW_REPLIES_RECEIVED = "$PACKAGE.on_new_replies_received"
    private const val POST_URLS_PARAM = "post_urls"
  }
}