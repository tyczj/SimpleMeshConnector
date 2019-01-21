package jtycz.simple.mesh.connector.ui.mesh

import androidx.lifecycle.ViewModel
import jtycz.simple.mesh.connector.bluetooth.ConnectedBluetoothDevice
import jtycz.simple.mesh.connector.protos.Common
import jtycz.simple.mesh.connector.protos.Mesh
import jtycz.simple.mesh.connector.utils.DeviceCommunicator
import jtycz.simple.mesh.connector.utils.DeviceRequestUtil
import jtycz.simple.mesh.connector.utils.asRequest
import jtycz.simple.mesh.connector.utils.Result
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MeshScanningViewModel: ViewModel(){

    val meshNetworks:MutableList<Mesh.NetworkInfo> = mutableListOf()
    var selectedMeshNetwork:Mesh.NetworkInfo? = null
    var connectedBluetoothDevice: ConnectedBluetoothDevice? = null

    suspend fun getMeshNetworks(communicator: DeviceCommunicator): Result<Mesh.ScanNetworksReply, Common.ResultCode>{
        val timeoutMills = 20000L
        val request = Mesh.ScanNetworksRequest.newBuilder().build()
        val requestFrame = request.asRequest()
        val response = withTimeoutOrNull(timeoutMills) {
            suspendCoroutine { continuation: Continuation<DeviceRequestUtil.DeviceResponse?> ->
                communicator.doSendRequest(requestFrame) { continuation.resume(it) }
            }
        }
        return communicator.buildResult(response) { r -> Mesh.ScanNetworksReply.parseFrom(r.payloadData) }
    }
}