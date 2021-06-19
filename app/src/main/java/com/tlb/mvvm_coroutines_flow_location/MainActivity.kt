package com.tlb.mvvm_coroutines_flow_location

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tlb.mvvm_coroutines_flow_location.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModel()
    private lateinit var binding: ActivityMainBinding
    private var dialog: AlertDialog? = null

    init {
        lifecycleScope.launchWhenCreated {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.gpsStatusFlow.collect(::onGpsStatus) }
                launch { viewModel.permissionStatusFlow.collect(::onPermissionStatus) }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.refresh.setOnClickListener {
            lifecycleScope.launch {
                val location = viewModel.locationFlow.first()
                binding.oneShotLocation.text = "${location.latitude}/${location.longitude}"
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.onActivityResult(this, requestCode, resultCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        viewModel.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun subscribeToLocation() = lifecycleScope.launch {
        viewModel
            .locationFlow
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .collect { location ->
                binding.realTimeLocation.text = "${location.latitude}/${location.longitude}"
            }
    }


    private fun onGpsStatus(gpsStatus: GpsStatus) {
        when(gpsStatus) {
            is GpsStatus.NotChecked -> viewModel.checkGps(this)
            is GpsStatus.Prompting -> dialog?.cancel()
            is GpsStatus.Denied -> MaterialAlertDialogBuilder(this)
                .setTitle("Location setting denied")
                .setMessage("This application need the location setting to be enabled to work properly, please enable it.")
                .setPositiveButton("Settings") { dialog, _ ->
                    viewModel.checkGps(this)
                    dialog.dismiss()
                }
                .show()
                .also { dialog = it }
            is GpsStatus.Enabled -> viewModel.checkLocationPermissions(this)
        }
    }

    private fun onPermissionStatus(
        permissionStatus: PermissionStatus
    ) {
        when(permissionStatus) {
            is PermissionStatus.NotChecked -> return // Do nothing
            is PermissionStatus.Rationale -> MaterialAlertDialogBuilder(this)
                .setTitle("Location Permission needed")
                .setMessage("This application needs location permission to run properly")
                .setPositiveButton("Ok") { dialog, _ ->
                    viewModel.requestPermissions(this)
                    dialog.dismiss()
                }
                .show()
                .also { dialog = it }
            is PermissionStatus.Granted -> MaterialAlertDialogBuilder(this)
                .setTitle("Location Permission granted")
                .setMessage("It's all good :)")
                .setPositiveButton("Ok") { dialog, _ -> dialog.dismiss() }
                .show()
                .also {
                    subscribeToLocation()
                    dialog = it
                }
            is PermissionStatus.Settings,
            is PermissionStatus.Prompting -> dialog?.cancel()
            is PermissionStatus.Denied -> MaterialAlertDialogBuilder(this)
                .setTitle("Location Permission refused")
                .setMessage("This application needs location permission to run properly, please grant it in your application settings.")
                .setPositiveButton("Settings") { dialog, _ ->
                    viewModel.openLocationPermissionSetting(this)
                    dialog.dismiss()
                }
                .show()
                .also { dialog = it }
        }
    }
}