package com.github.k1rakishou.kpnc.domain

interface TokenUpdater {
  fun reset()
  fun updateToken(token: String)
  fun awaitUntilTokenUpdated()
}