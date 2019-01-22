package jtycz.simple.mesh.connector.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.mesh.bluetooth.GATTStatusCode
import io.particle.mesh.bluetooth.connecting.ConnectionState
import jtycz.simple.mesh.connector.utils.castAndPost
import jtycz.simple.mesh.connector.utils.setOnMainThread
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel


typealias CharacteristicAndStatus = Pair<BluetoothGattCharacteristic, GATTStatusCode>


class BLELiveDataCallbacks : BluetoothGattCallback() {

    val connectionStateChangedLD: LiveData<ConnectionState?> = MutableLiveData()
    val onServicesDiscoveredLD: LiveData<GATTStatusCode> = MutableLiveData()
    val onCharacteristicWriteCompleteLD: LiveData<GATTStatusCode> = MutableLiveData()
    val onCharacteristicReadFailureLD: LiveData<CharacteristicAndStatus> = MutableLiveData()
    val onCharacteristicChangedFailureLD: LiveData<BluetoothGattCharacteristic> = MutableLiveData()
    val onMtuChangedLD: LiveData<Int?> = MutableLiveData()

    val readOrChangedReceiveChannel: ReceiveChannel<ByteArray>
        get() = mutableReceiveChannel
    private val mutableReceiveChannel = Channel<ByteArray>(256)


    fun closeChannel() {
        mutableReceiveChannel.close()
    }


    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        val state = ConnectionState.fromIntValue(newState)

        (connectionStateChangedLD as MutableLiveData).setOnMainThread(state)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        (onServicesDiscoveredLD as MutableLiveData).setOnMainThread(
                GATTStatusCode.fromIntValue(status)
        )
    }

    override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
    ) {
//        if (!characteristic.value.truthy()) {
//            (onCharacteristicChangedFailureLD as MutableLiveData).setOnMainThread(characteristic)
//            return
//        }
        receivePacket(characteristic.value)
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt,characteristic: BluetoothGattCharacteristic,
            statusCode: Int) {

        val status = GATTStatusCode.fromIntValue(statusCode)
//        if (!characteristic.value.truthy() || status != GATTStatusCode.SUCCESS) {
//            (onCharacteristicReadFailureLD as MutableLiveData).setOnMainThread(
//                    Pair(characteristic, status)
//            )
//            return
//        }
        receivePacket(characteristic.value)
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt,characteristic: BluetoothGattCharacteristic,
            statusCode: Int) {(
            onCharacteristicWriteCompleteLD as MutableLiveData).setOnMainThread(
                GATTStatusCode.fromIntValue(statusCode)
        )}

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        onMtuChangedLD.castAndPost(mtu)
    }

    private fun receivePacket(packet: ByteArray) {

        if (!mutableReceiveChannel.isClosedForSend) {
            mutableReceiveChannel.offer(packet)
        }
    }

}
