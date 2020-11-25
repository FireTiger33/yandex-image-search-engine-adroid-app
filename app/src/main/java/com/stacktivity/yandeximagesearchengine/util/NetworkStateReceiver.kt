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
import java.util.Deque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.collections.HashMap

/**
 * NetworkStateReceiver v1.0
 * Kotlin Author: Larin Grigoriy <https://github.com/FireTiger33>
 * created March 22, 2020
 * last updated November 25, 2020
 *
 * This class was created to work with Internet request queues.
 * It allows you to optimize download of by placing priority on the most recent requests.
 * The number of concurrent requests is set separately for each queue.
 *
 * This will be very useful, for example, when loading an image feed.
 * When images are added to the download queue sequentially and scroll quickly,
 * images in focus will be loaded immediately,
 * instead of waiting for those that are no longer visible on the screen to load.
 *
 * To use this class, you need to implement your own descendant class [NetworkListener] and
 * register this receiver in your code by calling [NetworkStateReceiver.register],
 * passing it application context.
 *
 * If the Internet connection is lost, all listeners that are currently active
 * will be notified and will be able to correctly handle this situation
 * without reporting a failed connection to the server.
 * When the connection is restored, they will also be notified and
 * can continue or start downloading the necessary data again.
 *
 * If you are using this class in your code please add the following line:
 *
 * NetworkStateReceiver by Larin Grigoriy <https://github.com/FireTiger33>
 */
class NetworkStateReceiver private constructor(context: Context) : BroadcastReceiver() {

    private var cm: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkListeners: ConcurrentHashMap<String, Deque<NetworkListener>> = ConcurrentHashMap()
    private val runningNetworkListeners: ConcurrentHashMap<String, Deque<NetworkListener>> = ConcurrentHashMap()
    private val networkListenersConf: HashMap<String, Int> = hashMapOf()

    abstract class NetworkListener {
        lateinit var poolTag: String
            internal set
        var isRunning = false
            internal set
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
        networkListeners[tag] = ConcurrentLinkedDeque()
        runningNetworkListeners[tag] = ConcurrentLinkedDeque()
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
            it.add(listener)

            if (runningNetworkListeners[tag]!!.size < networkListenersConf[tag]!!) {
                startNextListener(tag)
            }
        } ?: run { throw IllegalArgumentException("Pool $tag does not exist") }
    }

    fun removeListener(listener: NetworkListener) {
        if (networkListeners.containsKey(listener.poolTag)) {
            if (listener.isRunning) {
                listener.onCancel()
                runningNetworkListeners[listener.poolTag]!!.remove(listener)
            } else {
                networkListeners[listener.poolTag]!!.remove(listener)
            }
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
        networkListeners[poolTag]?.pollLast()?.let {
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
        startNextListener(listenerPool)
    }

}