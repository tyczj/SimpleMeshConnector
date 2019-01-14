package jtycz.simple.mesh.connector.bluetooth

import jtycz.simple.mesh.connector.utils.readByteArray
import java.nio.ByteBuffer

class PacketSplitter(private val outputTarget: (ByteArray) -> Unit) {

    private val buffer = ByteBuffer.allocate(10240)

    @Synchronized
    fun splitIntoPackets(bytes: ByteArray) {

        val mtuSize: Int = 20

        buffer.clear()
        buffer.put(bytes)
        buffer.flip()

        while (buffer.hasRemaining()) {
            val packet = if (buffer.remaining() >= mtuSize) {
                buffer.readByteArray(mtuSize)
            } else {
                buffer.readByteArray()
            }
            outputTarget(packet)
        }
    }
}