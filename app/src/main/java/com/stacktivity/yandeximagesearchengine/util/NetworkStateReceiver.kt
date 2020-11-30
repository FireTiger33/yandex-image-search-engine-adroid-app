package com.stacktivity.yandeximagesearchengine.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.*
import java.lang.IllegalStateException
import java.lang.Runnable
import java.net.URL
import java.util.Deque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.collections.HashMap
import kotlin.coroutines.suspendCoroutine

/**
 * NetworkStateReceiver v1.1
 * Kotlin Author: Larin Grigoriy <https://github.com/FireTiger33>
 * created March 22, 2020
 * last updated November 30, 2020
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
 * To full use this class, you need to implement your own descendant class [NetworkListener]
 * and register this receiver in your code by calling [NetworkStateReceiver.register].
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
class NetworkStateReceiver private constructor(context: Context) {

    // test host for checking Internet connection
    var testHost get() = NetworkStateChecker.testHost
        set(value) { NetworkStateChecker.testHost = value }

    var networkIsConnected: Boolean = false
        private set

    private val networkListeners: ConcurrentHashMap<String, Deque<NetworkListener>> =
        ConcurrentHashMap()
    private val runningNetworkListeners: ConcurrentHashMap<String, Deque<NetworkListener>> =
        ConcurrentHashMap()
    private val networkListenersConf: HashMap<String, Int> = hashMapOf()

    init {
        registerNetworkCallback(context)
        addNewPoolListeners(tag, 3)  // pool listeners for post method
    }

    abstract class NetworkListener {
        lateinit var poolTag: String
            internal set
        var isRunning = false
            internal set
        abstract fun onNetworkIsConnected(onSuccess: () -> Unit)
        open fun onNetworkIsDisconnected() { }
        open fun onCancel() { }
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
    }

    private object NetworkStateChecker {

        var testHost = "https://www.ya.ru"
            internal set(value) {
                field = value
                restart()
            }

        private val networkCheckScope = CoroutineScope(Dispatchers.IO)
        private var isStarted = false

        fun start() {
            if (!isStarted) {
                isStarted = true
                networkCheckScope.launch {
                    while (true) {
                        getInstance().applyNetworkStatus(networkAccessAvailable())
                        delay(3000)
                    }
                }
            }
        }

        fun stop() {
            networkCheckScope.coroutineContext.cancelChildren()
            isStarted = false
        }

        fun restart() {
            stop()
            start()
        }

        private suspend fun connectToTestHostAsync() = coroutineScope {
            async(Dispatchers.IO) {
                URL(testHost).openConnection().apply {
                    connectTimeout = 3000
                    readTimeout = 1000
                }.connect()
            }
        }

        private suspend fun networkAccessAvailable() = suspendCoroutine<Boolean> {
            networkCheckScope.launch {
                try {
                    connectToTestHostAsync().await()
                    it.resumeWith(Result.success(true))
                } catch (e: Exception) {
                    it.resumeWith(Result.success(false))
                }
            }
        }
    }


    /**
     * if you have Internet access, it starts [task],
     * else [task] added to the default pool listeners.
     *
     * Do not use this method to load a large amount of data,
     * due to the limitation of simultaneous tasks (by default, 3).
     * To change it, use [NetworkStateReceiver.changeMaxQueueForPool]
     * with [NetworkStateReceiver.tag]
     */
    fun post(task: Runnable) {
        if (networkIsConnected) {
            task.run()
        } else {
            addListener(tag, object : NetworkListener() {
                override fun onNetworkIsConnected(onSuccess: () -> Unit) {
                    task.run()
                    onSuccess()
                }
            })
        }
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

            if (runningNetworkListeners[tag]!!.size < networkListenersConf[tag]!!
                && networkIsConnected) {
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

    private fun applyNetworkStatus(isConnected: Boolean) {
        if (networkIsConnected != isConnected) {
            networkIsConnected = isConnected

            if (isConnected) {
                notifyNetworkIsConnected()
            } else {
                notifyNetworkIsDisconnected()
            }
        }
    }

    private fun notifyNetworkIsConnected() {
        networkListeners.keys.forEach { tag ->
            // start work
            runningNetworkListeners[tag]?.forEach {
                it.onNetworkIsConnected {
                    onListenerWorkComplete(it)
                }
            }
            startNextListener(tag)
        }
    }

    private fun notifyNetworkIsDisconnected() {
        networkListeners.keys.forEach { tag ->
            runningNetworkListeners[tag]?.forEach { listener ->
                listener.onNetworkIsDisconnected()
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

    private fun registerNetworkCallback(context: Context) {
        val cm: ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        cm.registerNetworkCallback(request, getNetworkCallbackListener())
    }

    private fun getNetworkCallbackListener(): ConnectivityManager.NetworkCallback {
        return object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                NetworkStateChecker.start()
                applyNetworkStatus(true)
            }

            override fun onLost(network: Network) {
                NetworkStateChecker.stop()
                applyNetworkStatus(false)
            }

            override fun onUnavailable() {
                NetworkStateChecker.stop()
                applyNetworkStatus(false)
                // TODO notify that Internet is not expected and show reload btn
            }

            override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                applyNetworkStatus(!blocked)
                if (blocked) NetworkStateChecker.stop()
                else NetworkStateChecker.start()
                // todo notify
            }
        }
    }
}