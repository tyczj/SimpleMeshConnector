package jtycz.simple.mesh.connector.ui.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.protobuf.ByteString
import jtycz.simple.mesh.connector.R
import jtycz.simple.mesh.connector.protos.WifiNew
import kotlinx.android.synthetic.main.wifi_scanning_layout.*
import java.nio.charset.Charset

class WifiScanningFragment: Fragment(), WifiAdapter.OnNetworkClickedListener {


    companion object {
        fun newInstance() = WifiScanningFragment()
    }

    private lateinit var viewModel: WifiScanningViewModel
    private lateinit var wifiManager:WifiManager
    private var adapter:WifiAdapter = WifiAdapter()

    private val wifiSecurityTypes = arrayOf("WEP", "PSK", "EAP")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.wifi_scanning_layout, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(WifiScanningViewModel::class.java)

        wifiManager = context!!.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context!!.registerReceiver(wifiScanReceiver, intentFilter)

        val linearLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL,false)
        adapter.listener = this@WifiScanningFragment
        adapter.networks = viewModel.wifiNetworks
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = adapter
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                //Get all found networks
                val results:MutableList<ScanResult> = wifiManager.scanResults
            }
        }
    }

    override fun onNetworkClicked(scanResult: ScanResult) {
        //TODO join network
        viewModel.selectedNetwork = scanResult

    }

    private fun joinNetwork(scanResult: ScanResult){

        val networkBuilder = WifiNew.ScanNetworksReply.Network.newBuilder()
        networkBuilder.bssid = ByteString.copyFrom(scanResult.BSSID,Charset.forName("UTF-8"))
        networkBuilder.ssid = scanResult.SSID

        if(isSecureConnection(scanResult)){
            networkBuilder.security =
        }
//        val credentials = if (network.security == Security.NO_SECURITY) {
//            Credentials.newBuilder()
//                .setType(CredentialsType.NO_CREDENTIALS)
//                .build()
//        } else {
//            Credentials.newBuilder()
//                .setType(CredentialsType.PASSWORD)
//                .setPassword(password)
//                .build()
//        }
//
//        val response = sendRequest(
//            JoinNewNetworkRequest.newBuilder()
//                .setSsid(network.ssid)
//                .setSecurity(network.security)
//                .setCredentials(credentials)
//                .build()
//        )
    }

    private fun isSecureConnection(scanResult: ScanResult):Boolean{
        for (securityType in wifiSecurityTypes) {
            if (scanResult.capabilities.contains(securityType)) {
                return true
            }
        }
        return false
    }
}