package com.tlb.mvvm_coroutines_flow_location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class LocationViewModel(
    private val locationRequest: LocationRequest,
    private val locationSettingsRequest: LocationSettingsRequest,
    private val locationProvider: FusedLocationProviderClient
): ViewModel() {
    private val _gpsStatusFlow = MutableStateFlow<GpsStatus>(GpsStatus.NotChecked)
    val gpsStatusFlow = _gpsStatusFlow.asStateFlow()

    private val _permissionStatusFlow = MutableStateFlow<PermissionStatus>(PermissionStatus.NotChecked)
    val permissionStatusFlow = _permissionStatusFlow.asStateFlow()

    @SuppressLint("MissingPermission")
    val locationFlow = callbackFlow<Location> {
        permissionStatusFlow
            .takeWhile { it !is PermissionStatus.Granted }
            .collect()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                trySend(result.lastLocation)
            }
        }

        locationProvider.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )

        awaitClose {
            locationProvider.removeLocationUpdates(callback)
        }
    }

    private val permissions = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun checkGps(activity: Activity) {
        val task = LocationServices
            .getSettingsClient(activity)
            .checkLocationSettings(locationSettingsRequest)

        task.addOnSuccessListener {
            emitGpsStatus(GpsStatus.Enabled)
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    emitGpsStatus(GpsStatus.Prompting)
                    exception.startResolutionForResult(
                        activity,
                        RequestCodes.REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // ignore it
                }
            }
        }
    }

    fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int) {
        when(requestCode) {
            RequestCodes.REQUEST_CHECK_SETTINGS -> emitGpsStatus(
                if (resultCode == Activity.RESULT_OK) {
                    GpsStatus.Enabled
                } else {
                    GpsStatus.Denied
                }
            )
            RequestCodes.REQUEST_LOCATION_PERMISSIONS_SETTINGS ->
                checkLocationPermissions(activity)
        }
    }

    private fun emitGpsStatus(
        gpsStatus: GpsStatus
    ) = viewModelScope.launch {
        _gpsStatusFlow.emit(gpsStatus)
    }

    fun checkLocationPermissions(activity: Activity) {
        val missingPermissions = permissions.map { permission ->
            ContextCompat.checkSelfPermission(
                activity,
                permission
            )
        }.any { it == PackageManager.PERMISSION_DENIED }

        if (missingPermissions) {
            if (activity.shouldShowRequestPermissionRationale(
                    permissions.first())) {
                emitPermissionStatus(PermissionStatus.Rationale)
            } else {
                requestPermissions(activity)
            }
        } else {
            emitPermissionStatus(PermissionStatus.Granted)
        }
    }

    fun requestPermissions(activity: Activity) {
        emitPermissionStatus(PermissionStatus.Prompting)
        activity.requestPermissions(
            permissions.toTypedArray(),
            RequestCodes.REQUEST_LOCATION_PERMISSIONS
        )
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == RequestCodes.REQUEST_LOCATION_PERMISSIONS
            && this.permissions.zip(permissions.toList()).all { (x, y) -> x == y }) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                emitPermissionStatus(PermissionStatus.Granted)
            } else {
                emitPermissionStatus(PermissionStatus.Denied)
            }
        }
    }

    fun openLocationPermissionSetting(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        emitPermissionStatus(PermissionStatus.Settings)
        activity.startActivityForResult(intent, RequestCodes.REQUEST_LOCATION_PERMISSIONS_SETTINGS)
    }

    private fun emitPermissionStatus(
        permissionStatus: PermissionStatus
    ) = viewModelScope.launch {
        _permissionStatusFlow.emit(permissionStatus)
    }
}