package jtycz.simple.mesh.connector.ui.bluetooth

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.ParcelUuid
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jtycz.simple.mesh.connector.R
import jtycz.simple.mesh.connector.ui.bluetooth.BluetoothUtils.Companion.BT_SETUP_READ_CHARACTERISTIC_ID
import jtycz.simple.mesh.connector.ui.bluetooth.BluetoothUtils.Companion.BT_SETUP_SERVICE_ID
import jtycz.simple.mesh.connector.ui.bluetooth.BluetoothUtils.Companion.BT_SETUP_WRITE_CHARACTERISTIC_ID
import jtycz.simple.mesh.connector.ui.bluetooth.BluetoothUtils.Companion.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID

class BluetoothScanningFragment : androidx.fragment.app.Fragment() {

    companion object {
        fun newInstance() = BluetoothScanningFragment()
    }

    private lateinit var bluetoothAdapter:BluetoothAdapter
    private lateinit var viewModel: BluetoothScanningViewModel

    private lateinit var deviceName:String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view:View = inflater.inflate(R.layout.main_fragment, container, false)

        var bundle:Bundle? = arguments
        bundle?.let {
            deviceName = bundle.getString("deviceName")!!
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(BluetoothScanningViewModel::class.java)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        val scanFilters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(BT_SETUP_SERVICE_ID)).build())
        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()
        bluetoothAdapter.bluetoothLeScanner.startScan(scanFilters,scanSettings,scanCallback)
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
        bluetoothAdapter.cancelDiscovery()
        device.connectGatt(context,false,gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback(){
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if(newState == BluetoothProfile.STATE_CONNECTED){
                val service = gatt?.services?.firstOrNull{it.uuid == BT_SETUP_SERVICE_ID}
                val characteristic = service?.characteristics?.firstOrNull { it.uuid == BT_SETUP_READ_CHARACTERISTIC_ID }
                val writeCharacteristic = service?.characteristics?.firstOrNull {it.uuid == BT_SETUP_WRITE_CHARACTERISTIC_ID}
                gatt?.setCharacteristicNotification(characteristic,true)
                val descriptor = characteristic?.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID)
                descriptor?.let {
                    if(it.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)){
                        gatt.writeDescriptor(descriptor)
                    }
                }
            }
        }
    }
}

