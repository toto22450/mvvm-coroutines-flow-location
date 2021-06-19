package com.tlb.mvvm_coroutines_flow_location

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MainApplication)
            modules(
                googlePlayServicesModule,
                viewModelModule,
            )
        }
    }
}