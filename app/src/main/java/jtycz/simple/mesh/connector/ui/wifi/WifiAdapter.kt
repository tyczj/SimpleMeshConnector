package jtycz.simple.mesh.connector.ui.wifi

import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import jtycz.simple.mesh.connector.R
import jtycz.simple.mesh.connector.protos.WifiNew

class WifiAdapter: RecyclerView.Adapter<WifiAdapter.ViewHolder>() {

    interface OnNetworkClickedListener{
        fun onNetworkClicked(scanResult: WifiNew.ScanNetworksReply.Network)
    }

    var listener:OnNetworkClickedListener? = null

    var networks:MutableList<WifiNew.ScanNetworksReply.Network> = mutableListOf()
        set(value) {
            val result:DiffUtil.DiffResult = DiffUtil.calculateDiff(WifiDiffCallback(value,networks),true)
            result.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.wifi_scan_result_row_layout, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return networks.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.ssid.text = networks[position].ssid
    }

    inner class ViewHolder(view: View):RecyclerView.ViewHolder(view),View.OnClickListener{

        var ssid: TextView = view.findViewById(R.id.ssid)

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            listener?.onNetworkClicked(networks[adapterPosition])
        }
    }
}