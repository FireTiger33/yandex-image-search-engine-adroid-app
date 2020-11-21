package com.stacktivity.yandeximagesearchengine.util.prefetcher

import android.content.Context
import android.util.SparseIntArray
import android.widget.FrameLayout
import androidx.recyclerview.widget.ALLOW_THREAD_GAP_WORK
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.viewType
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

internal class ViewHolderCreator(
    context: Context,
    private val holderConsumer: (holder: RecyclerView.ViewHolder, creationTimeNs: Long) -> Unit
) {

    private val fakeParent by lazy { FrameLayout(context) }
    private val createdOutsideChannel = Channel<ViewType>(1)
    private val enqueueChannel = Channel<ViewHolderWrapper>(1)
    private val createItemChannel = Channel<ViewHolderWrapper>(1)
    private val itemsCreated = SparseIntArray()
    private val itemsQueued = SparseIntArray()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun prepare() {
        coroutineScope.launch {
            for (e in createdOutsideChannel) {
                createdOutside(e.viewType)
            }
        }

        coroutineScope.launch {
            for (e in enqueueChannel) {
                enqueueBatch(e.holderCreator, e.viewType, e.itemsCount)
            }
        }

        coroutineScope.launch {
            for (e in createItemChannel) {
                createItem(e.holderCreator, e.viewType)
            }
        }
    }

    fun clear() {
        cancelChannels()
        itemsQueued.clear()
        itemsCreated.clear()
        coroutineScope.cancel()
    }

    fun setPrefetchBound(holderCreator: HolderCreator, viewType: Int, count: Int) {
        coroutineScope.launch {
            enqueueChannel.send(ViewHolderWrapper(holderCreator, viewType, count))
        }
    }

    fun itemCreatedOutside(viewType: Int) {
        coroutineScope.launch {
            createdOutsideChannel.send(ViewType(viewType))
        }
    }

    private fun createdOutside(viewType: Int) {
        itemsCreated.put(viewType, itemsCreated[viewType] + 1)
    }

    private fun enqueueBatch(holderCreator: HolderCreator, viewType: Int, count: Int) {
        if (itemsQueued[viewType] >= count) return
        itemsQueued.put(viewType, count)

        val created = itemsCreated[viewType]
        if (created >= count) return

        enqueueItemCreation(holderCreator, viewType)
    }

    private suspend fun createItem(holderCreator: HolderCreator, viewType: Int) {
        val created = itemsCreated[viewType] + 1
        val queued = itemsQueued[viewType]
        if (created > queued) return

        val holder: RecyclerView.ViewHolder
        val start: Long
        val end: Long

        try {
            start = nanoTimeIfNeed()
            holder = holderCreator(fakeParent, viewType)
            end = nanoTimeIfNeed()
        } catch (e: Exception) {
            return
        }

        holder.viewType = viewType
        itemsCreated.put(viewType, created)

        withContext(Dispatchers.Main) {
            holderConsumer(holder, end - start)
        }
        if (created < queued) enqueueItemCreation(holderCreator, viewType)
    }

    private fun enqueueItemCreation(holderCreator: HolderCreator, viewType: Int) {
        coroutineScope.launch {
            createItemChannel.send(ViewHolderWrapper(holderCreator, viewType))
        }
    }

    private fun cancelChannels() {
        createItemChannel.cancel()
        enqueueChannel.cancel()
        createdOutsideChannel.cancel()
    }

    private fun nanoTimeIfNeed() = if (ALLOW_THREAD_GAP_WORK) System.nanoTime() else 0L

    private class ViewHolderWrapper(
        val holderCreator: HolderCreator,
        val viewType: Int,
        val itemsCount: Int = 0
    )
}

private inline class ViewType(val viewType: Int)
