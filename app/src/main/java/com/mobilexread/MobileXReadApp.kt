package com.mobilexread

import android.app.Application
import com.mobilexread.llm.GemmaInference
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MobileXReadApp : Application() {

    @Inject lateinit var gemmaInference: GemmaInference

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Pre-warm the model in background so it's ready before the user shares a tweet
        appScope.launch {
            runCatching { gemmaInference.loadModel() }
        }
    }
}
