package com.github.k1rakishou.kpnc

import android.content.Context
import com.github.k1rakishou.kpnc.data.Host
import com.github.k1rakishou.kpnc.domain.*
import com.github.k1rakishou.kpnc.ui.main.MainViewModel
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

object DependencyGraph {
  private val host = Host("http://127.0.0.1:3000")

  fun initialize(applicationContext: Context): List<Module> {
    val modules = mutableListOf<Module>()
    modules += module {
      singletons(applicationContext)
      viewModels()
    }

    return modules
  }

  private fun Module.viewModels() {
    viewModel {
      MainViewModel(
        sharedPrefs = get(),
        googleServicesChecker = get(),
        teshPushMessageSender = get()
      )
    }
  }

  private fun Module.singletons(applicationContext: Context) {
    single { applicationContext }
    single { applicationContext.getSharedPreferences("kpnc", Context.MODE_PRIVATE) }
    single { Moshi.Builder().build() }
    single { OkHttpClient.Builder().build() }
    single { host }

    single<GoogleServicesChecker> { GoogleServicesCheckerImpl(applicationContext = get()) }
    single<TokenUpdater> {
      TokenUpdaterImpl(
        sharedPrefs = get(),
        host = host,
        moshi = get(),
        okHttpClient = get()
      )
    }
    single<MessageProcessor> { MessageProcessorImpl() }
    single<TeshPushMessageSender> {
      TeshPushMessageSenderImpl(
        host = get(),
        moshi = get(),
        sharedPrefs = get(),
        okHttpClient = get(),
        tokenUpdater = get()
      )
    }
    single<KPNCFirebaseServiceDelegate> {
      KPNCFirebaseServiceDelegateImpl(
        sharedPrefs = get(),
        tokenUpdater = get(),
        messageProcessor = get()
      )
    }
  }
}