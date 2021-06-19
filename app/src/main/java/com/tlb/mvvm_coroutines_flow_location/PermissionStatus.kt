package com.tlb.mvvm_coroutines_flow_location

sealed class PermissionStatus {
    object NotChecked: PermissionStatus()
    object Rationale: PermissionStatus()
    object Granted: PermissionStatus()
    object Settings: PermissionStatus()
    object Prompting: PermissionStatus()
    object Denied: PermissionStatus()
}