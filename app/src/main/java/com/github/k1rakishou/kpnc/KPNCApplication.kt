package com.github.k1rakishou.kpnc

import android.app.Application
import android.util.Log
import logcat.LogPriority
import logcat.LogcatLogger
import org.koin.core.context.GlobalContext

class KPNCApplication : Application() {

  override fun onCreate() {
    super.onCreate()

    LogcatLogger.install(KPNCLogger())

    GlobalContext.startKoin {
      modules(
        DependencyGraph.initialize(applicationContext = this@KPNCApplication.applicationContext)
      )
    }
  }

  private class KPNCLogger : LogcatLogger {
    override fun log(priority: LogPriority, tag: String, message: String) {
      when (priority) {
        LogPriority.VERBOSE -> Log.v("$GLOBAL_TAG | $tag", message)
        LogPriority.DEBUG -> Log.d("$GLOBAL_TAG | $tag", message)
        LogPriority.INFO -> Log.i("$GLOBAL_TAG | $tag", message)
        LogPriority.WARN -> Log.w("$GLOBAL_TAG | $tag", message)
        LogPriority.ERROR -> Log.e("$GLOBAL_TAG | $tag", message)
        LogPriority.ASSERT -> Log.e("$GLOBAL_TAG | $tag", message)
      }
    }
  }

  companion object {
    private const val GLOBAL_TAG = "KPNC"
  }

}