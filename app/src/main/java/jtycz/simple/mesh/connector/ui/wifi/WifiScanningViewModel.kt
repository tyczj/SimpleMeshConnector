package jtycz.simple.mesh.connector.ui.wifi

import android.net.wifi.ScanResult
import androidx.lifecycle.ViewModel

class WifiScanningViewModel:ViewModel() {

    var wifiNetworks:MutableList<ScanResult> = mutableListOf()
    var selectedNetwork:ScanResult? = null
}