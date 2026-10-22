package com.studyapp.decaycharge

import android.app.Application
import com.studyapp.decaycharge.data.local.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class DecayChargeApplication : Application() {
    @Inject
    lateinit var seeder: DatabaseSeeder

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            seeder.seedIfNeeded()
        }
    }
}
