package jtycz.simple.mesh.connector.ui.bluetooth

import java.util.*

class BluetoothUtils {

    companion object {
        const val ARGON_SERIAL_PREFIX1 = "ARGH"
        const val ARGON_SERIAL_PREFIX2 = "ARNH"
        const val ARGON_SERIAL_PREFIX3 = "ARNK"

        const val XENON_SERIAL_PREFIX1 = "XENH"
        const val XENON_SERIAL_PREFIX2 = "XENK"

        const val BORON_LTE_SERIAL_PREFIX1 = "B40H"
        const val BORON_LTE_SERIAL_PREFIX2 = "B40K"
        const val BORON_3G_SERIAL_PREFIX1 = "B31H"
        const val BORON_3G_SERIAL_PREFIX2 = "B31K"

        const val BLUETOOTH_ID_LENGTH = 6

        val BT_SETUP_SERVICE_ID =           UUID.fromString("6FA90001-5C4E-48A8-94F4-8030546F36FC")
        val BT_PROTOCOL_VERSION_ID =        UUID.fromString("6FA90002-5C4E-48A8-94F4-8030546F36FC")
        val BT_SETUP_READ_CHARACTERISTIC_ID = UUID.fromString("6FA90003-5C4E-48A8-94F4-8030546F36FC")
        val BT_SETUP_WRITE_CHARACTERISTIC_ID = UUID.fromString("6FA90004-5C4E-48A8-94F4-8030546F36FC")


        val NORDIC_BT_SETUP_SERVICE_ID =           UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val NORDIC_BT_SETUP_TX_CHARACTERISTIC_ID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        val NORDIC_BT_SETUP_RX_CHARACTERISTIC_ID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

        val CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    public fun getDeviceName(serialNumber:String): String {

        val deviceType = getDeviceTypeName(serialNumber)
        val lastSix = serialNumber.substring(serialNumber.length - BLUETOOTH_ID_LENGTH).toUpperCase()
        return "$deviceType-$lastSix"
    }

    private fun getDeviceTypeName(serialNumber: String): String {
        val first4 = serialNumber.substring(0, 4)
        return when (first4) {
            ARGON_SERIAL_PREFIX1,
            ARGON_SERIAL_PREFIX2,
            ARGON_SERIAL_PREFIX3 -> "Argon"
            XENON_SERIAL_PREFIX1,
            XENON_SERIAL_PREFIX2 -> "Xenon"
            BORON_LTE_SERIAL_PREFIX1,
            BORON_LTE_SERIAL_PREFIX2,
            BORON_3G_SERIAL_PREFIX1,
            BORON_3G_SERIAL_PREFIX2 -> "Boron"
            else -> "UNKNOWN"
        }
    }
}