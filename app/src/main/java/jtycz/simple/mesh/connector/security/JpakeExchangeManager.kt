package io.particle.mesh.setup.connection.security

import jtycz.simple.mesh.connector.bluetooth.InboundFrameReader
import jtycz.simple.mesh.connector.bluetooth.OutboundFrame
import jtycz.simple.mesh.connector.bluetooth.OutboundFrameWriter
import jtycz.simple.mesh.connector.security.ECJPake
import jtycz.simple.mesh.connector.security.Role
import java.io.IOException
import java.security.MessageDigest
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


class JpakeExchangeMessageTransceiver(
    private val frameWriter: OutboundFrameWriter,
    private val frameReader: InboundFrameReader
) {

    fun send(jpakeMessage: ByteArray) {
        frameWriter.writeFrame(OutboundFrame(jpakeMessage, jpakeMessage.size))
    }

    suspend fun receive(): ByteArray {
        val jpakeFrame = frameReader.inboundFrameChannel.receive()
        return jpakeFrame.frameData
    }

}



// NOTE: this class assumes it's in the *client* role.
// Some of the details below would have to change to support the server role
// e.g.: order of message exchange
class JpakeExchangeManager(
    private val jpakeImpl: ECJPake,
    private val msgTransceiver: JpakeExchangeMessageTransceiver
) {

    private var clientRoundOne: ByteArray? = null
    private var clientRoundTwo: ByteArray? = null
    private var serverRoundOne: ByteArray? = null
    private var serverRoundTwo: ByteArray? = null
    private var sharedSecret: ByteArray? = null

    /** Perform the JPAKE exchange (including confirmation) and return the shared secret. */
    @Throws(IOException::class)
    suspend fun performJpakeExchange(): ByteArray {
        try {
            sharedSecret = doPerformExchange()
            confirmSharedSecret()
            return sharedSecret!!

        } finally {
            clientRoundOne = null
            clientRoundTwo = null
            serverRoundOne = null
            serverRoundTwo = null
            sharedSecret = null
        }
    }

    private suspend fun doPerformExchange(): ByteArray {
        clientRoundOne = jpakeImpl.createLocalRoundOne()
        msgTransceiver.send(clientRoundOne!!)

        serverRoundOne = msgTransceiver.receive()
        serverRoundTwo = msgTransceiver.receive()

        jpakeImpl.receiveRemoteRoundOne(serverRoundOne!!)
        jpakeImpl.receiveRemoteRoundTwo(serverRoundTwo!!)

        clientRoundTwo = jpakeImpl.createLocalRoundTwo()
        msgTransceiver.send(clientRoundTwo!!)
        return jpakeImpl.calculateSharedSecret()
    }

    private suspend fun confirmSharedSecret() {

        val clientConfirmation = generateClientConfirmationData()
        msgTransceiver.send(clientConfirmation)
        val serverConfirmation = msgTransceiver.receive()
        val finalClientConfirmation = generateFinalConfirmation(clientConfirmation)

        if (Arrays.equals(serverConfirmation, finalClientConfirmation)) {
        } else {
            throw IOException("Cannot connect: local key confirmation data does not match remote!")
        }
    }

    private fun generateClientConfirmationData(): ByteArray {
        val roundsHash = shaHash(
                clientRoundOne!!,
                serverRoundOne!!,
                serverRoundTwo!!,
                clientRoundTwo!!
        )

        return hmacWithJpakeTagInKey(roundsHash, Role.CLIENT, Role.SERVER)
    }

    private fun generateFinalConfirmation(clientConfirmation: ByteArray): ByteArray {
        val confirmationData = shaHash(
                clientRoundOne!!,
                serverRoundOne!!,
                serverRoundTwo!!,
                clientRoundTwo!!,
                clientConfirmation
        )
        return hmacWithJpakeTagInKey(confirmationData, Role.SERVER, Role.CLIENT)
    }

    private fun shaHash(vararg byteArrays: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        for (array in byteArrays) {
            digest.update(array)
        }
        return digest.digest()
    }

    private fun hmacWithJpakeTagInKey(includeInHmac: ByteArray, role1: Role, role2: Role): ByteArray {
        val macKeyMaterial = shaHash(sharedSecret!!, "JPAKE_KC".toByteArray())
        val macKey = SecretKeySpec(macKeyMaterial, HMAC_SHA256)
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(macKey)
        mac.update("KC_1_U")
        mac.update(role1.stringValue)
        mac.update(role2.stringValue)
        mac.update(includeInHmac)
        return mac.doFinal()
    }
}

private fun Mac.update(str: String) {
    this.update(str.toByteArray())
}

private const val HMAC_SHA256 = "HmacSHA256"
