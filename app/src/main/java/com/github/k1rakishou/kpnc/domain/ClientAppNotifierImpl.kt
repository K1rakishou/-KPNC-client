package com.github.k1rakishou.kpnc.domain

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.github.k1rakishou.kpnc.helpers.logcatDebug
import com.github.k1rakishou.kpnc.helpers.sendOrderedBroadcastSuspend
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ClientAppNotifierImpl(
  private val appContext: Context,
) : ClientAppNotifier {
  private val mainScope = MainScope()

  override fun onRepliesReceived(postUrls: List<String>) {
    logcatDebug(TAG) { "onRepliesReceived() postUrls: ${postUrls.size}" }

    if (postUrls.isEmpty()) {
      return
    }

    postUrls.forEach { postUrl ->
      logcatDebug(TAG) { "onRepliesReceived() postUrl: ${postUrl}" }
    }

    mainScope.launch { onRepliesReceivedInternal(postUrls) }
  }

  private suspend fun onRepliesReceivedInternal(postUrls: List<String>) {
    val intent = Intent(ACTION_ON_NEW_REPLIES_RECEIVED)
    intent.putExtra(POST_URLS_PARAM, postUrls.toTypedArray())

    sendBroadcastInternal(appContext, intent)
  }

  private suspend fun sendBroadcastInternal(context: Context, intent: Intent): Bundle? {
    val broadcastReceiversInfo = context.packageManager.queryBroadcastReceivers(intent, 0)
    logcat.logcat(TAG) { "broadcastReceiversInfo=${broadcastReceiversInfo.size}" }

    val broadcastReceiver = broadcastReceiversInfo.firstOrNull()
      ?: return null

    logcat.logcat(TAG) {
      "Using packageName: ${broadcastReceiver.activityInfo.packageName}, "
      "name: ${broadcastReceiver.activityInfo.name}"
    }

    intent.component = ComponentName(
      broadcastReceiver.activityInfo.packageName,
      broadcastReceiver.activityInfo.name
    )

    return sendOrderedBroadcastSuspend(context, intent, null)
  }

  companion object {
    private const val TAG = "ClientAppNotifierImpl"
    private const val PACKAGE = "com.github.k1rakishou.kurobaexlite"

    private const val ACTION_ON_NEW_REPLIES_RECEIVED = "$PACKAGE.on_new_replies_received"
    private const val POST_URLS_PARAM = "post_urls"
  }
}