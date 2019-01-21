package jtycz.simple.mesh.connector.ui.mesh

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jtycz.simple.mesh.connector.MainActivity
import jtycz.simple.mesh.connector.R
import jtycz.simple.mesh.connector.protos.Mesh
import jtycz.simple.mesh.connector.utils.DeviceCommunicator
import jtycz.simple.mesh.connector.security.Security
import kotlinx.android.synthetic.main.network_scanning_layout.*
import kotlinx.coroutines.*

class MeshScanningFragment:Fragment(), MeshAdapter.OnMeshNetworkClickedListener {

    // dispatches execution into Android main thread
    val uiDispatcher: CoroutineDispatcher = Dispatchers.Main
    val uiScope = CoroutineScope(Dispatchers.Main)
    // represent a pool of shared threads as coroutine dispatcher
    val bgDispatcher: CoroutineDispatcher = Dispatchers.IO

    companion object {
        fun newInstance() = MeshScanningFragment()
    }

    private lateinit var viewModel: MeshScanningViewModel
    private var adapter:MeshAdapter = MeshAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.network_scanning_layout, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?){
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MeshScanningViewModel::class.java)

        val linearLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL,false)
        adapter.listener = this@MeshScanningFragment
        adapter.networks = viewModel.meshNetworks
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = adapter

        if(viewModel.connectedBluetoothDevice == null){
            viewModel.connectedBluetoothDevice = (activity as MainActivity).connectedBluetoothDevice
        }

        if(viewModel.connectedBluetoothDevice != null){
            uiScope.launch {
                val deviceCommunicator = DeviceCommunicator.buildCommunicator(viewModel.connectedBluetoothDevice!!, Security())
                val result = withContext(bgDispatcher){

                    viewModel.getMeshNetworks(deviceCommunicator!!)
                }

                val networks = result.value?.networksList
                if(networks != null){
                    adapter.networks = networks
                }

            }
        }
    }

    override fun onNetworkClicked(scanResult: Mesh.NetworkInfo) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}