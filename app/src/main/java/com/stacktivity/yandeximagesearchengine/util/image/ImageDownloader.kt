package com.stacktivity.yandeximagesearchengine.util.image

import android.graphics.Bitmap
import com.stacktivity.yandeximagesearchengine.util.Downloader
import com.stacktivity.yandeximagesearchengine.util.NetworkStateReceiver.NetworkListener
import com.stacktivity.yandeximagesearchengine.util.SmartTask
import pl.droidsonroids.gif.GifDrawable


/**
 * Used as a base for classes that load images.
 *
 * When you try to [download] an image with the same taskId again,
 * priority of the already running task will be increased,
 * as well as a new [observer] will be applied.
 */
abstract class AbstractSmartBitmapDownloader(
    taskId: Any,
    poolTag: String,
    var observer: BitmapObserver,
) : SmartTask(taskId, poolTag), BitmapObserver {
    lateinit var task: NetworkListener


    override fun onBitmapResult(bitmap: Bitmap) {
        onTaskCompleted()
        observer.onBitmapResult(bitmap)
    }

    override fun onException(e: Throwable) {
        onTaskCompleted()
        observer.onException(e)
    }

    protected abstract fun onStartDownload(): NetworkListener

    open fun download() {
        getStartedTaskById()?.let {
            // task with [taskId] is already running
            it as AbstractSmartBitmapDownloader
            it.observer = observer
            increaseTaskPriority(it.task)
        } ?: run {
            task = onStartDownload()
            onTaskStarted()
        }
    }

    open fun cancel() {
        if (this::task.isInitialized) {
            Downloader.stopAndDeleteTask(task)
            onTaskCompleted()
        }
    }

    private fun increaseTaskPriority(task: NetworkListener) {
        Downloader.increaseTaskPriority(task)
    }

    protected open fun downloadAsync(
        url: String,
        imageFactory: Downloader.Observer
    ): NetworkListener {
        return Downloader.downloadAsync(poolTag, url, imageFactory, 3000)
    }
}

abstract class AbstractSmartImageDownloader(
    taskId: Any,
    poolTag: String,
    observer: ImageObserver,
) : AbstractSmartBitmapDownloader(taskId, poolTag, observer), ImageObserver {
    var imageObserver: ImageObserver
        set(value) { observer = value }
        get() = observer as ImageObserver

    override fun onGifResult(drawable: GifDrawable, width: Int, height: Int) {
        onTaskCompleted()
        imageObserver.onGifResult(drawable, width, height)
    }
}

/**
 * Implementation of the default image loader,
 * which uses a link to an image as a unique task identifier
 *
 * @see AbstractSmartBitmapDownloader
 */
class SmartBitmapDownloader(
    val url: String,
    poolTag: String,
    bitmapObserver: BitmapObserver,
) : AbstractSmartBitmapDownloader(url, poolTag, bitmapObserver) {
    override fun onStartDownload(): NetworkListener = downloadAsync(url, BitmapFactory(this))

    companion object {
        fun removeAllTasksByPoolTag(poolTag: String) {
            clearTasksByPoolTag(poolTag)
            Downloader.stopAndDeleteAllTasksByTag(poolTag)
        }
    }
}

class SmartImageDownloader(
    val url: String,
    poolTag: String,
    observer: ImageObserver,
) : AbstractSmartImageDownloader(url, poolTag, observer) {
    override fun onStartDownload(): NetworkListener = downloadAsync(url, ImageFactory(this))

    companion object {
        fun removeAllTasksByPoolTag(poolTag: String) {
            SmartBitmapDownloader.removeAllTasksByPoolTag(poolTag)
        }
    }
}