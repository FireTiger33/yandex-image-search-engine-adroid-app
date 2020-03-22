@file:Suppress("DEPRECATION")

package com.stacktivity.yandeximagesearchengine.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.util.concurrent.ConcurrentHashMap

class NetworkStateReceiver private constructor(context: Context) : BroadcastReceiver() {

    private var cm: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkListeners: ConcurrentHashMap<String, MutableSet<NetworkListener>> = ConcurrentHashMap()
    private val networkListenersConf: HashMap<String, Int> = hashMapOf()

    abstract class NetworkListener {
        lateinit var poolTag: String
        var isRunning = false
        abstract fun onNetworkIsConnected(onSuccess: () -> Unit)
        abstract fun onNetworkIsDisconnected()
        abstract fun onCancel()
    }

    private interface Listener {
        fun onSuccess(tag: String)
    }

    companion object {
        val tag = NetworkStateReceiver::class.java.simpleName
        private var INSTANCE: NetworkStateReceiver? = null

        fun register(context: Context) = INSTANCE
            ?: NetworkStateReceiver(context).also {
                INSTANCE = it
            }

        fun getInstance(): NetworkStateReceiver {
            return INSTANCE?: throw NullPointerException("Receiver is not registered")
        }

        fun networkIsConnected(networkStateReceiver: NetworkStateReceiver): Boolean {
            val info: NetworkInfo? = networkStateReceiver.cm.activeNetworkInfo
            return info != null && info.isConnected
        }
    }

    init {
        context.applicationContext.registerReceiver(this,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    /**
     * Creates a new listener pool with the specified [tag].
     * If it already exists, the pool will be cleared.
     *
     * @param tag      - unique tag
     * @param maxQueue - max count listeners in a pool
     */
    fun addNewPoolListeners(tag: String, maxQueue: Int) {
        networkListeners[tag] = LinkedHashSet()
        networkListenersConf[tag] = maxQueue
    }

    fun changeMaxQueueForPool(tag: String, newMaxQueue: Int) {
        if (networkListenersConf.containsKey(tag)) {
            networkListenersConf[tag] = newMaxQueue
        } else {
            throw IllegalStateException("Pool $tag does not exist")
        }
    }

    fun addListener(tag: String, listener: NetworkListener) {
        if (networkListeners.containsKey(tag)) {
            listener.poolTag = tag
            /*networkListeners[tag]!!.let {
                if (networkListenersConf[tag] != 0 && networkListenersConf[tag] == it.size) {
                    it.remove(it.elementAt(1))
                }
            }*/

            networkListeners[tag]!!.add(listener)

            if (networkListeners[tag]!!.size <= networkListenersConf[tag]!!) {
                newListener().onSuccess(tag)
            }
        } else {
            throw IllegalArgumentException("Pool $tag does not exist")
        }
    }

    fun removeListener(tag: String, listener: NetworkListener) {
        if (networkListeners.containsKey(tag)) {
            listener.onCancel()
            networkListeners[tag]!!.remove(listener)
        } else {
            throw IllegalStateException("Pool $tag does not exist")
        }
    }

    fun removeListenersPool(tag: String) {
        networkListeners[tag]?.let {
            it.forEach { listener ->
                listener.onCancel()
            }
            it.clear()
        }
    }

    fun removeAllListeners() {
        networkListeners.forEach { pool ->
            removeListenersPool(pool.key)
        }
    }

    fun getListenersCountByTag(tag: String): Int? = networkListeners[tag]?.size

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            Log.d("NetworkStateReceiver", "call listeners, count: ${networkListeners.size}")
            val poolTags = networkListeners.keys

            if (networkIsConnected(this)) {
                poolTags.forEach { tag ->
                    newListener().onSuccess(tag)
                }
            } else {
                poolTags.forEach { tag ->
                    networkListeners[tag]?.forEach { listener ->
                        listener.onNetworkIsDisconnected()
                    }
                }
            }
        }
    }

    private fun newListener(): Listener {
        return object : Listener {
            override fun onSuccess(tag: String) {
                if (networkListeners[tag]?.isEmpty() != false) {
                    return
                }

                networkListeners[tag]!!.last().let {
                    if (!it.isRunning) {
                        it.isRunning = true
                        it.onNetworkIsConnected {
                            // on success work
                            removeListener(tag, it)
                            if (networkListeners[tag]!!.size > 0) {
                                onSuccess(tag)
                            }
                        }
                    }
                }
            }
        }
    }

}