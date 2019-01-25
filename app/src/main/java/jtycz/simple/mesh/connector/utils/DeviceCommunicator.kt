package jtycz.simple.mesh.connector.utils

import android.util.SparseArray
import com.google.protobuf.GeneratedMessageV3
import jtycz.simple.mesh.connector.bluetooth.*
import jtycz.simple.mesh.connector.protos.Common
import jtycz.simple.mesh.connector.security.Security
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DeviceCommunicator private constructor(requestWriter: RequestWriter) {

    private val requestCallbacks = SparseArray<(DeviceRequestUtil.DeviceResponse?) -> Unit>()
    val requestWriter = requestWriter


    companion object {
        private val FULL_PROTOCOL_HEADER_SIZE = 6
        private val AES_CCM_MAC_SIZE = 8

        suspend fun buildCommunicator(connectedBluetoothDevice: ConnectedBluetoothDevice,security: Security):DeviceCommunicator?{
            val packetSplitter = PacketSplitter({ packet ->
                connectedBluetoothDevice.packetSendChannel.offer(packet)
            })
            val frameWriter = OutboundFrameWriter { packetSplitter.splitIntoPackets(it) }
            val frameReader = InboundFrameReader()
            GlobalScope.launch(Dispatchers.Default) {
                for (packet in connectedBluetoothDevice.closablePacketReceiveChannel) {
                    frameReader.receivePacket(BlePacket(packet))
                }
            }

            val cryptoDelegate = security.createCryptoDelegate(connectedBluetoothDevice.deviceSecret,frameWriter,frameReader) ?: return null

            frameReader.cryptoDelegate = cryptoDelegate
            frameWriter.cryptoDelegate = cryptoDelegate

            // now that we've passed the security handshake, set the correct number of bytes to assume
            // for the message headers
            frameReader.extraHeaderBytes = DeviceCommunicator.FULL_PROTOCOL_HEADER_SIZE + AES_CCM_MAC_SIZE

            val requestWriter = RequestWriter { frameWriter.writeFrame(it) }
            val communicator = DeviceCommunicator(requestWriter)
            val responseReader = ResponseReader { communicator.receiveResponse(it) }
            GlobalScope.launch(Dispatchers.Default) {
                for (inboundFrame in frameReader.inboundFrameChannel) {
                    responseReader.receiveResponseFrame(inboundFrame)
                }
            }

            return communicator
        }
    }

    fun doSendRequest(request: DeviceRequestUtil.DeviceRequest, continuationCallback: (DeviceRequestUtil.DeviceResponse?) -> Unit) {
        val requestCallback = { frame: DeviceRequestUtil.DeviceResponse? -> continuationCallback(frame) }
        synchronized(requestCallbacks) {
            requestCallbacks.put(request.requestId.toInt(), requestCallback)
        }
        requestWriter.writeRequest(request)
    }

    fun receiveResponse(responseFrame: DeviceRequestUtil.DeviceResponse) {
        val callback = requestCallbacks[responseFrame.requestId.toInt()]
        if (callback != null) {
            callback(responseFrame)
        } else {
        }
    }

    fun <V : GeneratedMessageV3> buildResult(response: DeviceRequestUtil.DeviceResponse?, successTransformer: (DeviceRequestUtil.DeviceResponse) -> V
    ): Result<V, Common.ResultCode> {
        if (response == null) {
            return Result.Absent()
        }

        return if (response.resultCode == 0) {
            val transformed = successTransformer(response)
            Result.Present(transformed)
        } else {
            val code = response.resultCode.toResultCode()
            Result.Error(code)
        }
    }
}