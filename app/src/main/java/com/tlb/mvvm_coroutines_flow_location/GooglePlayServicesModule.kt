package com.tlb.mvvm_coroutines_flow_location

import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val googlePlayServicesModule = module {
    single {
        LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(3)
            fastestInterval = TimeUnit.SECONDS.toMillis(1)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }
    single {
        LocationSettingsRequest
            .Builder()
            .addLocationRequest(get())
            .build()
    }
    single { LocationServices.getFusedLocationProviderClient(androidApplication()) }
}