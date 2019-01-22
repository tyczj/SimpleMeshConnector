package jtycz.simple.mesh.connector.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class SimpleLifecycleOwner : LifecycleOwner {

    private val registry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = registry

    fun setNewState(newState: Lifecycle.State) {
        runOnMainThread { registry.markState(newState) }
    }

}