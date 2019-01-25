package io.particle.mesh.bluetooth.connecting

import android.bluetooth.BluetoothProfile
import jtycz.simple.mesh.connector.utils.buildIntValueMap


enum class ConnectionState(val intValue: Int)  {

    DISCONNECTED(BluetoothProfile.STATE_DISCONNECTED),
    CONNECTING(BluetoothProfile.STATE_CONNECTING),
    CONNECTED(BluetoothProfile.STATE_CONNECTED),
    DISCONNECTING(BluetoothProfile.STATE_DISCONNECTING),
    UNKNOWN(0x10101); // made up value that doesn't conflict with/overload any others


    companion object {

        private val intValueMap = buildIntValueMap(values()) { state -> state.intValue }

        fun fromIntValue(intValue: Int): ConnectionState {
            return intValueMap.get(intValue, UNKNOWN)
        }
    }

}


