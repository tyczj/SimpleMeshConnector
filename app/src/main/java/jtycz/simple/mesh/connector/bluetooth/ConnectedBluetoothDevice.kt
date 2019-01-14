package jtycz.simple.mesh.connector.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel

data class ConnectedBluetoothDevice(val deviceName:String,
                                    val deviceSecret:String,
                                    val gatt: BluetoothGatt,
                                    val writeCharacteristic:BluetoothGattCharacteristic,
                                    val packetSendChannel: SendChannel<ByteArray>,
                                    val packetReceiveChannel: Channel<ByteArray>
)