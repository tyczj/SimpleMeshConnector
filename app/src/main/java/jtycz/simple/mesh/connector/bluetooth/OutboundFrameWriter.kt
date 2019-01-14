package jtycz.simple.mesh.connector.bluetooth

import jtycz.simple.mesh.connector.security.AesCcmDelegate
import jtycz.simple.mesh.connector.utils.readByteArray
import jtycz.simple.mesh.connector.utils.writeUint16LE
import okio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal const val MAX_FRAME_SIZE = 10240

class OutboundFrame(val frameData: ByteArray, val payloadSize: Int)

class OutboundFrameWriter(private val byteSink: (ByteArray) -> Unit) {

    // Externally mutable state is less than awesome.  Patches welcome.
    var cryptoDelegate: AesCcmDelegate? = null

    private val buffer = ByteBuffer.allocate(MAX_FRAME_SIZE).order(ByteOrder.LITTLE_ENDIAN)

    @Synchronized
    fun writeFrame(frame: OutboundFrame) {
        buffer.clear()

        buffer.writeUint16LE(frame.payloadSize)

        val fml = Buffer()
        fml.writeShortLe(frame.payloadSize)
        val additionalData = fml.readByteArray()

        val data = cryptoDelegate?.encrypt(frame.frameData, additionalData) ?: frame.frameData
        buffer.put(data)
        buffer.flip()

        val finalFrame = buffer.readByteArray()
        byteSink(finalFrame)
    }

}