package com.stacktivity.yandeximagesearchengine.util.prefetcher

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.attachToPreventViewPoolFromClearing
import androidx.recyclerview.widget.factorInCreateTime
import androidx.recyclerview.widget.viewType

class PrefetchRecycledViewPool(context: Context) : RecyclerView.RecycledViewPool(), HolderPrefetcher {

    private val viewHolderCreator = ViewHolderCreator(context, ::putViewFromCreator)

    override fun setViewsCount(viewType: Int, count: Int, holderCreator: HolderCreator) {
        require(count > 0)
        viewHolderCreator.setPrefetchBound(holderCreator, viewType, count)
    }

    fun prepare() {
        viewHolderCreator.prepare()
        attachToPreventViewPoolFromClearing()
    }

    override fun putRecycledView(scrap: RecyclerView.ViewHolder) {
        val viewType = scrap.itemViewType
        setMaxRecycledViews(viewType, 10)
        super.putRecycledView(scrap)
    }

    override fun getRecycledView(viewType: Int): RecyclerView.ViewHolder? {
        val holder = super.getRecycledView(viewType)
        if (holder == null) {
            viewHolderCreator.itemCreatedOutside(viewType)
        }
        return holder
    }

    override fun clear() {
        viewHolderCreator.clear()
        super.clear()
    }

    private fun putViewFromCreator(scrap: RecyclerView.ViewHolder, creationTimeNs: Long) {
        factorInCreateTime(scrap.viewType, creationTimeNs)
        putRecycledView(scrap)
    }
}