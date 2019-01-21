package jtycz.simple.mesh.connector.ui.mesh

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import jtycz.simple.mesh.connector.R
import jtycz.simple.mesh.connector.protos.Mesh

class MeshAdapter: RecyclerView.Adapter<MeshAdapter.ViewHolder>() {

    interface OnMeshNetworkClickedListener{
        fun onNetworkClicked(scanResult: Mesh.NetworkInfo)
    }

    var listener:OnMeshNetworkClickedListener? = null
    var networks:MutableList<Mesh.NetworkInfo> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.network_scan_result_row_layout, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return networks.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.name.text = networks[position].name
    }

    inner class ViewHolder(view: View):RecyclerView.ViewHolder(view), View.OnClickListener{

        var name: TextView = view.findViewById(R.id.network_name)

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            listener?.onNetworkClicked(networks[adapterPosition])
        }
    }
}