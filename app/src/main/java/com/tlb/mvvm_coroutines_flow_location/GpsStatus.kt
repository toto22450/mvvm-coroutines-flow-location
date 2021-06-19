package com.tlb.mvvm_coroutines_flow_location

sealed class GpsStatus {
    object NotChecked: GpsStatus()
    object Prompting: GpsStatus()
    object Denied: GpsStatus()
    object Enabled: GpsStatus()
}