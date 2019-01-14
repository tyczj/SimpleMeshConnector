package jtycz.simple.mesh.connector

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import jtycz.simple.mesh.connector.ui.barcode.BarcodeScanningActivity
import jtycz.simple.mesh.connector.ui.bluetooth.BluetoothScanningFragment
import jtycz.simple.mesh.connector.bluetooth.BluetoothUtils
import jtycz.simple.mesh.connector.bluetooth.ConnectedBluetoothDevice
import jtycz.simple.mesh.connector.ui.main.MainFragment
import jtycz.simple.mesh.connector.ui.wifi.WifiScanningFragment

class MainActivity : AppCompatActivity(), BluetoothScanningFragment.OnBluetoothConnected {

    private val BARCODE_REQUEST_CODE = 1
    private var bluetoothFragment: BluetoothScanningFragment? = null

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
            val deviceName = BluetoothUtils.getDeviceName(barcode)
            bluetoothFragment = BluetoothScanningFragment.newInstance()
            val bundle = Bundle()
            bundle.putString("deviceName",deviceName)
            bluetoothFragment!!.arguments = bundle
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, bluetoothFragment!!)
                .commitNow()
        }
    }

    override fun onBluetoothConnected(connectedBluetoothDevice: ConnectedBluetoothDevice) {
        when{
            connectedBluetoothDevice.deviceName.contains("Argon") ->{
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, WifiScanningFragment.newInstance())
                    .commitNow()
            }
        }
    }

}
