package com.stacktivity.yandeximagesearchengine.base

import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView.ViewHolder

abstract class BaseImageViewHolder(itemView: View): ViewHolder(itemView) {
    protected var bitmapObserver: BitmapObserver = object : BitmapObserver() {
        override fun onChanged(t: Bitmap?) {}
    }

    protected abstract class BitmapObserver: Observer<Bitmap?> {
        var requiredToShow = true
    }
}