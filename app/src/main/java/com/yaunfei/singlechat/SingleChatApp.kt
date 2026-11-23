package com.yaunfei.singlechat

import android.app.Application
import com.yaunfei.singlechat.core.di.ApplicationScope
import com.yaunfei.singlechat.core.network.BackendClient
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class SingleChatApp : Application() {

    @Inject lateinit var backend: BackendClient

    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // 启动即回源一次，拿到最新的 BFF 地址（隧道轮换后无需用户操作）。
        appScope.launch { runCatching { backend.refreshRemoteConfig() } }
    }
}
