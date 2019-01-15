package jtycz.simple.mesh.connector.ui.wifi

import android.net.wifi.ScanResult
import androidx.lifecycle.ViewModel
import jtycz.simple.mesh.connector.bluetooth.ConnectedBluetoothDevice
import jtycz.simple.mesh.connector.protos.WifiNew
import jtycz.simple.mesh.connector.security.Security
import jtycz.simple.mesh.connector.utils.DeviceCommunicator
import jtycz.simple.mesh.connector.utils.DeviceRequestUtil
import jtycz.simple.mesh.connector.utils.asRequest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WifiScanningViewModel:ViewModel() {

    var wifiNetworks:MutableList<ScanResult> = mutableListOf()
    var selectedNetwork:ScanResult? = null
    var connectedBluetoothDevice:ConnectedBluetoothDevice? = null

    suspend fun getWifiNetworks(){
        val deviceUtil = DeviceCommunicator.buildCommunicator(connectedBluetoothDevice!!, Security())
        val timeoutMills = 20000L //20 seconds
        val request = WifiNew.ScanNetworksRequest.newBuilder().build()
        val requestFrame = request.asRequest()
        val response = withTimeoutOrNull(timeoutMills) {
            suspendCoroutine { continuation: Continuation<DeviceRequestUtil.DeviceResponse?> ->
                deviceUtil?.doSendRequest(requestFrame) { continuation.resume(it) }
            }
        }
    }
}