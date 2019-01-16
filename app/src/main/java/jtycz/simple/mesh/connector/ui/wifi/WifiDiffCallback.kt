package jtycz.simple.mesh.connector.ui.wifi

import android.net.wifi.ScanResult
import androidx.recyclerview.widget.DiffUtil
import jtycz.simple.mesh.connector.protos.WifiNew

class WifiDiffCallback(private val newList:MutableList<WifiNew.ScanNetworksReply.Network>, private val oldList:MutableList<WifiNew.ScanNetworksReply.Network>): DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return newList[0].ssid == oldList[0].ssid
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return newList[0] == oldList[0]
    }

}