package jtycz.simple.mesh.connector.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import androidx.lifecycle.LiveData
import io.particle.mesh.bluetooth.connecting.ConnectionState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel

data class ConnectedBluetoothDevice(val deviceName:String,
                                    val deviceSecret:String,
                                    val gatt: BluetoothGatt,
                                    val connectionStateChangedLD: LiveData<ConnectionState?>,
                                    val packetSendChannel: SendChannel<ByteArray>,
                                    val closablePacketReceiveChannel: Channel<ByteArray>
)