package com.tlb.mvvm_coroutines_flow_location

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest

class MainViewModel(
    locationRequest: LocationRequest,
    locationProvider: FusedLocationProviderClient
): LocationViewModel(
    locationRequest,
    locationProvider
)