@file:Suppress("DEPRECATION")

package com.stacktivity.yandeximagesearchengine.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashMap

class NetworkStateReceiver private constructor(context: Context) : BroadcastReceiver() {

    private var cm: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkListeners: ConcurrentHashMap<String, Queue<NetworkListener>> = ConcurrentHashMap()
    private val runningNetworkListeners: ConcurrentHashMap<String, Queue<NetworkListener>> = ConcurrentHashMap()
    private val networkListenersConf: HashMap<String, Int> = hashMapOf()

    abstract class NetworkListener {
        lateinit var poolTag: String
        var isRunning = false
        abstract fun onNetworkIsConnected(onSuccess: () -> Unit)
        abstract fun onNetworkIsDisconnected()
        abstract fun onCancel()
    }

    companion object {
        val tag: String = NetworkStateReceiver::class.java.simpleName
        private var INSTANCE: NetworkStateReceiver? = null

        fun register(context: Context) = INSTANCE
                ?: NetworkStateReceiver(context).also {
                    INSTANCE = it
                }

        fun getInstance(): NetworkStateReceiver {
            return INSTANCE ?: throw NullPointerException("Receiver is not registered")
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
        networkListeners[tag] = ConcurrentLinkedQueue()
        runningNetworkListeners[tag] = ConcurrentLinkedQueue()
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
        networkListeners[tag]?.let {
            listener.poolTag = tag
            /*if (networkListenersConf[tag] != 0 && networkListenersConf[tag] == it.size) {
                it.remove(it.elementAt(1))
            }*/

            it.add(listener)

            if (it.size <= networkListenersConf[tag]!!) {
                startNextListener(tag)
            }
        } ?: run { throw IllegalArgumentException("Pool $tag does not exist") }
    }

    fun removeListener(tag: String, listener: NetworkListener) {
        if (networkListeners.containsKey(tag)) {
            if (listener.isRunning) {
                listener.onCancel()
                runningNetworkListeners[tag]!!.remove(listener)
            } else {
                networkListeners[tag]!!.remove(listener)
            }
        } else {
            throw IllegalStateException("Pool $tag does not exist")
        }
    }

    /**
     * Shuts down and clear queue of listeners with specified tag
     */
    fun removeListenersPool(tag: String) {
        networkListeners[tag]?.clear()
        runningNetworkListeners[tag]?.let {
            it.forEach { listener ->
                listener.onCancel()
            }
            it.clear()
        }
    }

    /**
     * Shuts down and clears queues of all connected listeners
     */
    fun removeAllListeners() {
        networkListeners.forEach { pool ->
            removeListenersPool(pool.key)
        }
    }

    fun getListenersCountByTag(tag: String): Int? = networkListeners[tag]?.size

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val poolTags = networkListeners.keys

            if (networkIsConnected(this)) {
                poolTags.forEach { tag ->
                    // start work
                    runningNetworkListeners[tag]?.forEach {
                        it.onNetworkIsConnected {
                            onListenerWorkComplete(it)
                        }
                    }
                    startNextListener(tag)
                }
            } else {
                poolTags.forEach { tag ->
                    runningNetworkListeners[tag]?.forEach { listener ->
                        listener.onNetworkIsDisconnected()
                    }
                }
            }
        }
    }

    private fun startNextListener(poolTag: String) {
        networkListeners[poolTag]?.remove()?.let {
            it.isRunning = true
            runningNetworkListeners[poolTag]?.add(it)
            it.onNetworkIsConnected {
                // on success work
                onListenerWorkComplete(it)
            }
        }
    }

    private fun onListenerWorkComplete(listener: NetworkListener) {
        val listenerPool = listener.poolTag
        runningNetworkListeners[listenerPool]?.remove(listener)
        if (networkListeners[listenerPool]!!.size > 0) {
            startNextListener(listenerPool)
        }
    }

}