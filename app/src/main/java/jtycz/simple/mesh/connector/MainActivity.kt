package jtycz.simple.mesh.connector

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import io.particle.mesh.bluetooth.connecting.ConnectionState
import jtycz.simple.mesh.connector.bluetooth.BLELiveDataCallbacks
import jtycz.simple.mesh.connector.bluetooth.BTCharacteristicWriter
import jtycz.simple.mesh.connector.ui.barcode.BarcodeScanningActivity
import jtycz.simple.mesh.connector.ui.bluetooth.BluetoothScanningFragment
import jtycz.simple.mesh.connector.bluetooth.BluetoothUtils
import jtycz.simple.mesh.connector.bluetooth.ConnectedBluetoothDevice
import jtycz.simple.mesh.connector.protos.Common
import jtycz.simple.mesh.connector.protos.Mesh
import jtycz.simple.mesh.connector.security.Security
import jtycz.simple.mesh.connector.ui.main.MainFragment
import jtycz.simple.mesh.connector.ui.mesh.MeshScanningFragment
import jtycz.simple.mesh.connector.ui.wifi.WifiScanningFragment
import jtycz.simple.mesh.connector.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity(), BluetoothScanningFragment.OnBluetoothConnected {

    private val BARCODE_REQUEST_CODE = 1
    private var bluetoothFragment: BluetoothScanningFragment? = null
    var connectedBluetoothDevice:ConnectedBluetoothDevice? = null
    val receiveChannel = Channel<ByteArray>(256)
    private lateinit var bluetoothAdapter:BluetoothAdapter
    private lateinit var deviceName:String
    private lateinit var deviceSecret:String
    private var bluetoothCallback:BLELiveDataCallbacks? = null
    private var bluetoothWriteChannel:BTCharacteristicWriter? = null
    private val lifecycleOwner = SimpleLifecycleOwner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
        startBarcodeScanningActivity()
    }

    private fun startBarcodeScanningActivity(){
        val intent = Intent(this, BarcodeScanningActivity::class.java)
        startActivityForResult(intent,BARCODE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == BARCODE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            //Go and connect to device
            val barcode = data!!.getStringExtra("barcode")
            val serialNumber = BluetoothUtils.getDeviceSerialnumber(barcode)
            deviceName = BluetoothUtils.getDeviceName(serialNumber)
            deviceSecret = BluetoothUtils.getDeviceSecret(barcode)

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            val scanFilters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(BluetoothUtils.BT_SETUP_SERVICE_ID)).build())
            val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()
            bluetoothAdapter.bluetoothLeScanner.startScan(scanFilters,scanSettings,scanCallback)



//            bluetoothFragment = BluetoothScanningFragment.newInstance()
//            val bundle = Bundle()
//            bundle.putString("deviceName",deviceName)
//            bundle.putString("deviceSecret",deviceSecret)
//            bluetoothFragment!!.arguments = bundle
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.container, bluetoothFragment!!)
//                .commitNow()
        }
    }

    override fun onBluetoothConnected(connectedBluetoothDevice: ConnectedBluetoothDevice) {
//        runOnUiThread(object:Runnable{
//            override fun run() {
//                this@MainActivity.connectedBluetoothDevice = connectedBluetoothDevice
//                when{
//                    connectedBluetoothDevice.deviceName.contains("Argon") ->{
//                        supportFragmentManager.beginTransaction()
//                            .replace(R.id.container, WifiScanningFragment.newInstance())
//                            .commitNow()
//                    }
//                    connectedBluetoothDevice.deviceName.contains("Xenon") ->{
//                        supportFragmentManager.beginTransaction()
//                            .replace(R.id.container, MeshScanningFragment.newInstance())
//                            .commitNow()
//                    }
//                }
//            }
//        })

    }

    //Call back for the Bluetooth LE scanner when it finds devices
    private val scanCallback = object : ScanCallback(){

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if(result?.device?.name == deviceName){
                //Found
                bluetoothAdapter.bluetoothLeScanner.stopScan(this)
                connectToGatt(result.device)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            if(results == null){
                return
            }
            for(scanResult in results){
                if(scanResult.device?.name == deviceName){
                    //Found
                    bluetoothAdapter.bluetoothLeScanner.stopScan(this)
                    connectToGatt(scanResult.device)
                }
            }
        }
    }

    private fun connectToGatt(device:BluetoothDevice){
        lifecycleOwner.setNewState(Lifecycle.State.RESUMED)
        bluetoothAdapter.cancelDiscovery()
        val bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.address)
        bluetoothCallback = BLELiveDataCallbacks()

        val gatt = bluetoothDevice.connectGatt(this,false,bluetoothCallback)

        bluetoothCallback!!.connectionStateChangedLD.observe(lifecycleOwner, Observer {
            if(it == ConnectionState.CONNECTED){
                gatt.discoverServices()
            }
        })

        bluetoothCallback!!.onServicesDiscoveredLD.observe(lifecycleOwner, Observer { _ ->
            val service = gatt.services.firstOrNull{it.uuid == BluetoothUtils.BT_SETUP_SERVICE_ID }
            val readCharacteristic = service?.characteristics?.firstOrNull { it.uuid == BluetoothUtils.BT_SETUP_READ_CHARACTERISTIC_ID }
            val writeCharacteristic = service?.characteristics?.firstOrNull {it.uuid == BluetoothUtils.BT_SETUP_WRITE_CHARACTERISTIC_ID }
            gatt?.setCharacteristicNotification(readCharacteristic,true)
            val descriptor = readCharacteristic?.getDescriptor(BluetoothUtils.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID)
            bluetoothWriteChannel = BTCharacteristicWriter(gatt!!,writeCharacteristic!!,bluetoothCallback!!.onCharacteristicWriteCompleteLD)
            descriptor?.let {
                if(it.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)){
                    gatt.writeDescriptor(descriptor)

                    val messageWriteChannel = Channel<ByteArray>(Channel.UNLIMITED)
                    GlobalScope.launch(Dispatchers.Default) {
                        for (packet in messageWriteChannel) {
                            bluetoothWriteChannel?.writeToCharacteristic(packet)
                        }
                    }

                    connectedBluetoothDevice = ConnectedBluetoothDevice(
                        deviceName,
                        deviceSecret,
                        gatt,
                        bluetoothCallback!!.connectionStateChangedLD,
                        messageWriteChannel,
                        bluetoothCallback!!.readOrChangedReceiveChannel as Channel<ByteArray>)
//                    onBluetoothConnected(connectedDevice)

                    GlobalScope.launch(Dispatchers.Default) {

                        for (i in 0..10){
//                            val device = withTimeoutOrNull(15000){
//                                DeviceCommunicator.buildCommunicator(connectedBluetoothDevice!!, Security())
//                            }

                            val deviceComm = DeviceCommunicator.buildCommunicator(connectedBluetoothDevice!!, Security())

                            if (deviceComm != null){
                                val results = getMeshNetworks(deviceComm)
                                val networks = results.value?.networksList
                                break
                            }
                        }

                        Log.d("MainActivity","Connected?")
                    }
                }
            }
        })
    }

    private val gattCallback = object : BluetoothGattCallback(){
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {

            val service = gatt?.services?.firstOrNull{it.uuid == BluetoothUtils.BT_SETUP_SERVICE_ID }
            val readCharacteristic = service?.characteristics?.firstOrNull { it.uuid == BluetoothUtils.BT_SETUP_READ_CHARACTERISTIC_ID }
            val writeCharacteristic = service?.characteristics?.firstOrNull {it.uuid == BluetoothUtils.BT_SETUP_WRITE_CHARACTERISTIC_ID }
            gatt?.setCharacteristicNotification(readCharacteristic,true)
            val descriptor = readCharacteristic?.getDescriptor(BluetoothUtils.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID)
            bluetoothWriteChannel = BTCharacteristicWriter(gatt!!,writeCharacteristic!!,bluetoothCallback!!.onCharacteristicWriteCompleteLD)
            descriptor?.let {
                if(it.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)){
                    gatt.writeDescriptor(descriptor)

                    val messageWriteChannel = Channel<ByteArray>(Channel.UNLIMITED)
                    GlobalScope.launch(Dispatchers.Default) {
                        for (packet in messageWriteChannel) {
                            bluetoothWriteChannel?.writeToCharacteristic(packet)
                        }
                    }

                    connectedBluetoothDevice = ConnectedBluetoothDevice(
                        deviceName,
                        deviceSecret,
                        gatt,
                        bluetoothCallback!!.connectionStateChangedLD,
                        messageWriteChannel,
                        bluetoothCallback!!.readOrChangedReceiveChannel as Channel<ByteArray>)

                    GlobalScope.launch(Dispatchers.Default) {

                        for (i in 0..10){

                            val device = DeviceCommunicator.buildCommunicator(connectedBluetoothDevice!!, Security())

                            if (device != null){
                                val results = getMeshNetworks(device)
                                val networks = results.value?.networksList
                                break
                            }
                        }

                        Log.d("MainActivity","Connected?")
                    }
                }
            }
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if(newState == BluetoothProfile.STATE_CONNECTED){

                gatt?.discoverServices()
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
           if(!receiveChannel.isClosedForReceive){
               receiveChannel.offer(characteristic!!.value)
           }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            if(!receiveChannel.isClosedForReceive){
                receiveChannel.offer(characteristic!!.value)
            }
        }
    }

    suspend fun getMeshNetworks(communicator: DeviceCommunicator): Result<Mesh.ScanNetworksReply, Common.ResultCode> {
        val timeoutMills = 20000L
        val request = Mesh.ScanNetworksRequest.newBuilder().build()
        val requestFrame = request.asRequest()
        val response = withTimeoutOrNull(timeoutMills) {
            suspendCoroutine { continuation: Continuation<DeviceRequestUtil.DeviceResponse?> ->
                communicator.doSendRequest(requestFrame) { continuation.resume(it) }
            }
        }
        return communicator.buildResult(response) { r -> Mesh.ScanNetworksReply.parseFrom(r.payloadData) }
    }

}
