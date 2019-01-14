package jtycz.simple.mesh.connector.utils

import com.google.protobuf.AbstractMessage
import jtycz.simple.mesh.connector.bluetooth.InboundFrame
import jtycz.simple.mesh.connector.bluetooth.MAX_FRAME_SIZE
import jtycz.simple.mesh.connector.bluetooth.OutboundFrame
import jtycz.simple.mesh.connector.protos.Extensions
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

class DeviceRequestUtil {

    class DeviceRequest(
        val requestId: Short,
        val messageType: Short,
        val payloadData: ByteArray
    ) {

        init {
            // ID "0" is reserved, shouldn't be used by a client
            require(requestId != 0.toShort())
        }
    }


    class DeviceResponse(
        val requestId: Short,
        val resultCode: Int,
        val payloadData: ByteArray
    )


}

class RequestWriter(
    private val sink: (OutboundFrame) -> Unit
) {

    private val buffer = ByteBuffer.allocate(MAX_FRAME_SIZE).order(ByteOrder.LITTLE_ENDIAN)

    @Synchronized
    fun writeRequest(request: DeviceRequestUtil.DeviceRequest) {
        buffer.clear()

        buffer.putShort(request.requestId)
        buffer.putShort(request.messageType)
        buffer.putShort(0)  // for the "reserved" field
        buffer.put(request.payloadData)

        buffer.flip()

        sink(
            OutboundFrame(
            buffer.readByteArray(),
            request.payloadData.size
        )
        )
    }

}


class ResponseReader(
    private val sink: (DeviceRequestUtil.DeviceResponse) -> Unit
) {

    private val buffer = ByteBuffer.allocate(MAX_FRAME_SIZE).order(ByteOrder.LITTLE_ENDIAN)

    fun receiveResponseFrame(frame: InboundFrame) {
        buffer.clear()
        buffer.put(frame.frameData)
        buffer.flip()

        val requestId = buffer.readUint16LE().toShort()
        val result = buffer.int
        val payload = buffer.readByteArray()

        val response = DeviceRequestUtil.DeviceResponse(requestId, result, payload)
        sink(response)
    }

}

private var requestIdGenerator = AtomicInteger()

internal fun AbstractMessage.asRequest(): DeviceRequestUtil.DeviceRequest {
    val requestId = requestIdGenerator.incrementAndGet().toShort()
    return DeviceRequestUtil.DeviceRequest(
        requestId,
        // get type ID from the proto message descriptor
        this.descriptorForType.options.getExtension(Extensions.typeId).toShort(),
        this.toByteArray()
    )
}

fun ByteBuffer.readByteArray(bytesToRead: Int): ByteArray {
    val copyTarget = ByteArray(bytesToRead)
    this.get(copyTarget, 0, copyTarget.size)
    return copyTarget
}

fun ByteBuffer.readByteArray(): ByteArray {
    val copyTarget = ByteArray(this.remaining())
    this.get(copyTarget, 0, copyTarget.size)
    return copyTarget
}

fun ByteBuffer.readUint16LE(): Int {
    return ByteMath.readUint16LE(this)
}

fun ByteBuffer.writeUint16LE(value: Int): ByteBuffer {
    ByteMath.writeUint16LE(this, value)
    return this
}

fun ByteBuffer.putUntilFull(other: ByteBuffer) {
    // there's method call overhead here, but given the context, I'm not too concerned.
    while (this.hasRemaining() && other.hasRemaining()) {
        this.put(other.get())
    }
}
