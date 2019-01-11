package jtycz.simple.mesh.connector.ui.wifi

import android.net.wifi.ScanResult
import androidx.recyclerview.widget.DiffUtil

class WifiDiffCallback(private val newList:MutableList<ScanResult>,private val oldList:MutableList<ScanResult>): DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return newList[0].SSID == oldList[0].SSID
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return newList[0] == oldList[0]
    }

}