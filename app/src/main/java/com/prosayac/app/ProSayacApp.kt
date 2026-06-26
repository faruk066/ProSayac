package com.prosayac.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ProSayacApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Configuration.Provider handles WorkManager initialization automatically.
        // Explicit WorkManager.initialize() is NOT needed and can cause issues.

        // Cancel any stale WorkManager jobs from previous app versions
        WorkManager.getInstance(this).cancelAllWorkByTag("upload_sync")
    }
}
