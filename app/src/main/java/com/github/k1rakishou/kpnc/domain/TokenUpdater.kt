package com.github.k1rakishou.kpnc.domain

interface TokenUpdater {
  fun reset()
  fun updateToken(userId: String?, token: String): Boolean
  fun awaitUntilTokenUpdated()
}