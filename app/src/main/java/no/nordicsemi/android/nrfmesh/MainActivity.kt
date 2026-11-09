package no.nordicsemi.android.nrfmesh

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import no.nordicsemi.android.common.theme.NordicActivity
import no.nordicsemi.android.common.theme.NordicTheme
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : NordicActivity() {

    @Inject
    lateinit var repository: CoreDataRepository

    // Listens to Bluetooth being turned on while the app is visible.
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                resumeAutomaticConnectivity()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Permissions don't broadcast changes, so re-check whenever the activity is resumed,
        // e.g. after a permission dialog or returning from the system settings.
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                ContextCompat.registerReceiver(
                    this@MainActivity,
                    bluetoothStateReceiver,
                    IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }

            override fun onResume(owner: LifecycleOwner) {
                resumeAutomaticConnectivity()
            }

            override fun onStop(owner: LifecycleOwner) {
                unregisterReceiver(bluetoothStateReceiver)
            }
        })
        setContent {
            NordicTheme {
                MeshApp()
            }
        }
    }

    /**
     * Restarts the automatic connectivity, if enabled, once Bluetooth is on and the required
     * permissions are granted. The call is a no-op if the connectivity is already running.
     */
    private fun resumeAutomaticConnectivity() {
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        if (bluetoothManager?.adapter?.isEnabled != true) return
        if (!hasBluetoothPermissions()) return
        repository.onBluetoothEnabled()
    }

    private fun hasBluetoothPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
