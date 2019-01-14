package jtycz.simple.mesh.connector.bluetooth

import jtycz.simple.mesh.connector.security.AesCcmDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import okio.Buffer

class InboundFrame(val frameData: ByteArray)

class BlePacket(val data: ByteArray)

class InboundFrameReader {

    val inboundFrameChannel = Channel<InboundFrame>(256)
    // Externally mutable state is less than awesome.  Patches welcome.
    var cryptoDelegate: AesCcmDelegate? = null
    // Externally mutable state is less than awesome.  Patches welcome.
    // Number of header bytes beyond just the "size" field
    var extraHeaderBytes: Int = 0

    private var inProgressFrame: InProgressFrame? = null

    @Synchronized
    fun receivePacket(blePacket: BlePacket) {
        try {
            if (inProgressFrame == null) {
                inProgressFrame = InProgressFrame(extraHeaderBytes)
            }

            val bytesForNextFrame = inProgressFrame!!.writePacket(blePacket.data)

            if (inProgressFrame!!.isComplete) {
                handleCompleteFrame()
            }

            if (bytesForNextFrame != null) {
                receivePacket(BlePacket(bytesForNextFrame))
            }

        } catch (ex: Exception) {
            inProgressFrame = null // toss out old frame to reset us for the next one
        }
    }

    private fun handleCompleteFrame() {
        val ipf = inProgressFrame!!
        val frameData = ipf.consumeFrameData()
        val payloadSize = frameData.size - extraHeaderBytes
        val additionalData = Buffer().writeShortLe(payloadSize).readByteArray()
        val completeFrame = InboundFrame(
            cryptoDelegate?.decrypt(frameData, additionalData) ?: frameData
        )
        inProgressFrame = null
        GlobalScope.launch(Dispatchers.Default) {
            inboundFrameChannel.send(completeFrame)
        }

    }
}