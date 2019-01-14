package jtycz.simple.mesh.connector.bluetooth

import jtycz.simple.mesh.connector.utils.putUntilFull
import jtycz.simple.mesh.connector.utils.readByteArray
import jtycz.simple.mesh.connector.utils.readUint16LE
import jtycz.simple.mesh.connector.utils.writeUint16LE
import java.nio.ByteBuffer
import java.nio.ByteOrder

class InProgressFrame(private val extraHeaderBytes: Int) {

    var isComplete = false
        private set

    private var frameSize: Int? = null // Int instead of Short because it's sent as a uint16_t
    private var finalFrameData: ByteArray? = null

    private val packetBuffer = ByteBuffer.allocate(MAX_FRAME_SIZE).order(ByteOrder.LITTLE_ENDIAN)
    // note that this field gets its limit set below
    private val frameDataBuffer = ByteBuffer.allocate(MAX_FRAME_SIZE).order(ByteOrder.LITTLE_ENDIAN)

    private var isConsumed = false

    @Synchronized
    fun writePacket(packet: ByteArray): ByteArray? {  // returns the "extra" bytes
        ensureFrameIsNotReused()

        packetBuffer.put(packet)
        packetBuffer.flip()

        if (frameSize == null) {
            onFirstPacket()
        }

        frameDataBuffer.putUntilFull(packetBuffer)

        var bytesForNextFrame: ByteArray? = null
        if (!frameDataBuffer.hasRemaining()) {
            isComplete = true
            onComplete()
            bytesForNextFrame = getBytesForNextFrame()
        }
        packetBuffer.clear()

        return bytesForNextFrame
    }

    @Synchronized
    fun consumeFrameData(): ByteArray {
        require(isComplete) { "Cannot consume data, frame is not complete!" }

        ensureFrameIsNotReused()
        isConsumed = true

        return finalFrameData!!
    }

    private fun onFirstPacket() {
        // add the header bytes because, unfortunately, the size field here reflects
        // the *payload size*, not the *remaining frame size*, and the messaging stack
        // has to handle the header bytes inbetween for itself...
        val payloadSize = packetBuffer.short
        frameSize = extraHeaderBytes + payloadSize
        require(frameSize!! >= 0) { "Invalid frame size: $frameSize" }
        frameDataBuffer.limit(frameSize!!.toInt())
    }

    private fun onComplete() {
        frameDataBuffer.flip()
        finalFrameData = frameDataBuffer.readByteArray(frameSize!!.toInt())
    }

    private fun getBytesForNextFrame(): ByteArray? {
        if (packetBuffer.remaining() < 2) { // do we have a size header?
            return null
        }

        val nextFrameSize = packetBuffer.readUint16LE()
        if (nextFrameSize == 0) {
            // remainder of the frame must be zeros; bail
            return null
        }

        val remaining = packetBuffer.readByteArray()

        packetBuffer.clear()

        packetBuffer.writeUint16LE(nextFrameSize)
        packetBuffer.put(remaining)

        packetBuffer.flip()

        return packetBuffer.readByteArray()
    }

    private fun ensureFrameIsNotReused() {
        if (isConsumed) {
            throw IllegalStateException("Frame already consumed!  Instances cannot be reused!")
        }
    }
}