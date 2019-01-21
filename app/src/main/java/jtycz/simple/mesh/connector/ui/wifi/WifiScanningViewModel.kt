package jtycz.simple.mesh.connector.ui.wifi

import androidx.lifecycle.ViewModel
import jtycz.simple.mesh.connector.bluetooth.ConnectedBluetoothDevice
import jtycz.simple.mesh.connector.protos.Common
import jtycz.simple.mesh.connector.protos.WifiNew
import jtycz.simple.mesh.connector.utils.DeviceCommunicator
import jtycz.simple.mesh.connector.utils.DeviceRequestUtil
import jtycz.simple.mesh.connector.utils.asRequest
import jtycz.simple.mesh.connector.utils.Result
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WifiScanningViewModel:ViewModel() {

    var wifiNetworks:MutableList<WifiNew.ScanNetworksReply.Network> = mutableListOf()
    var selectedNetwork: WifiNew.ScanNetworksReply.Network? = null
    var connectedBluetoothDevice:ConnectedBluetoothDevice? = null

    suspend fun getWifiNetworks(commincator:DeviceCommunicator): Result<WifiNew.ScanNetworksReply, Common.ResultCode>{

        val timeoutMills = 20000L //20 seconds
        val request = WifiNew.ScanNetworksRequest.newBuilder().build()
        val requestFrame = request.asRequest()
        val response = withTimeoutOrNull(timeoutMills) {
            suspendCoroutine { continuation: Continuation<DeviceRequestUtil.DeviceResponse?> ->
                commincator.doSendRequest(requestFrame) { continuation.resume(it) }
            }
        }

        return commincator.buildResult(response) { r -> WifiNew.ScanNetworksReply.parseFrom(r.payloadData) }
    }
}