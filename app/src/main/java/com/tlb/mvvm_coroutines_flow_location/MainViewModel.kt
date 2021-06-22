package com.tlb.mvvm_coroutines_flow_location

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest

class MainViewModel(
    locationRequest: LocationRequest,
    locationSettingsRequest: LocationSettingsRequest,
    locationProvider: FusedLocationProviderClient
): LocationViewModel(
    locationRequest,
    locationSettingsRequest,
    locationProvider
)