package com.stacktivity.yandeximagesearchengine.ui.dialog

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT

import com.stacktivity.yandeximagesearchengine.R.string.image_load_failed
import com.stacktivity.yandeximagesearchengine.R.layout.image_dialog
import com.stacktivity.yandeximagesearchengine.data.ImageData
import com.stacktivity.yandeximagesearchengine.util.getString as getResString

import com.stacktivity.yandeximagesearchengine.util.ViewUtils
import com.stacktivity.yandeximagesearchengine.util.image.ImageObserver
import com.stacktivity.yandeximagesearchengine.util.image.BitmapObserver
import com.stacktivity.yandeximagesearchengine.util.image.ImageDataDownloader
import com.stacktivity.yandeximagesearchengine.util.image.BufferedSmartBitmapDownloader
import com.stacktivity.yandeximagesearchengine.util.sendImage
import com.stacktivity.yandeximagesearchengine.util.shortToast
import com.stacktivity.yandeximagesearchengine.util.showImage
import com.stacktivity.yandeximagesearchengine.util.BitmapUtils.blur

import com.tunjid.androidx.core.delegates.fragmentArgs

import kotlinx.android.synthetic.main.image_action_buttons.view.*
import kotlinx.android.synthetic.main.image_with_progress_bar.view.*

import pl.droidsonroids.gif.GifDrawable

import java.lang.ref.WeakReference

class ImageDialog : DialogFragment() {
    private var mImageData by fragmentArgs<ImageData>()
    private var thumb by fragmentArgs<String?>()
    private var previewIsNeeded = true
    private var imageLoader: ImageDataDownloader? = null
    private var btnHeight: Int? = null

    companion object {
        val mTag: String = ImageDialog::class.java.simpleName
        fun newInstance(imageData: ImageData, thumbUrl: String? = null) = ImageDialog().apply {
            this.mImageData = imageData
            thumb = thumbUrl
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(image_dialog, container, false)

        setClickListeners(view)
        enableButtons(view, false)

        view.post {

            val viewRef = WeakReference(view)

            showProgressBar(view)

            // download image
            imageLoader = ImageDataDownloader(mImageData, mTag, getImageObserver(viewRef))
                .apply { download() }

            // download preview
            thumb?.let {
                BufferedSmartBitmapDownloader(it, mTag, getPreviewImageObserver(viewRef))
                    .download()
            }

            btnHeight = view.btn_send.height
            prepareView(mImageData.width, mImageData.height)
        }

        return view
    }

    private fun enableButtons(rootView: View, enable: Boolean) {
        rootView.btn_open.isEnabled = enable
        rootView.btn_send.isEnabled = enable
    }

    private fun setClickListeners(rootView: View) {
        rootView.btn_send.setOnClickListener {
            sendImage(BufferedSmartBitmapDownloader.getCacheFile(mImageData.url), rootView.context)
        }

        rootView.btn_open.setOnClickListener {
            showImage(BufferedSmartBitmapDownloader.getCacheFile(mImageData.url), rootView.context)
        }
    }

    override fun onStart() {
        super.onStart()
        val currentOrientation = resources.configuration.orientation
        dialog?.window?.apply {
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                setLayout(WRAP_CONTENT, MATCH_PARENT)
            } else {
                setLayout(MATCH_PARENT, WRAP_CONTENT)
            }
        }
    }

    override fun onDestroy() {
        imageLoader?.cancel()
        super.onDestroy()
    }

    private fun onImageLoadComplete(rootView: View, imageWidth: Int, imageHeight: Int) {
        prepareView(imageWidth, imageHeight, true)
        hideProgressBar(rootView)
        enableButtons(rootView, true)
    }


    /**
     * Used to adjust the size of the view,
     * taking into account size of the image being placed,
     * to use the maximum available window sizes.
     * It must be called before the image is inserted and when the first changes are applied.
     *
     * TODO Requires further development.
     */
    private fun prepareView(imageWidth: Int, imageHeight: Int, pref: Boolean = false) {
        view?.let {
            val possibleImageHeight = it.height - btnHeight!!
            val possibleImageWidth = it.width

            it.imageView.run {
                val currentOrientation = context.resources.configuration.orientation

                if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    val calcWidth =
                        ViewUtils.calculateViewWidth(possibleImageHeight, imageWidth, imageHeight)
                    val cropFactor = calcWidth / possibleImageWidth.toFloat()

                    setLayout(calcWidth, possibleImageHeight)
                    setTextSize(it, cropFactor)
                } else {
                    val calcHeight =
                        ViewUtils.calculateViewHeight(possibleImageWidth, imageWidth, imageHeight)

                    if (calcHeight > possibleImageHeight && pref) {
                        val cropFactor = calcHeight / possibleImageHeight.toFloat()
                        val resWidth = (possibleImageWidth / cropFactor).toInt()
                        setLayout(resWidth, possibleImageHeight)
                    } else {
                        setLayout(possibleImageWidth, calcHeight)
                    }
                }
            }
            it.requestLayout()
        }
    }

    private fun showProgressBar(rootView: View) {
        rootView.image_load_progress_bar?.visibility = View.VISIBLE
    }

    private fun hideProgressBar(rootView: View) {
        rootView.image_load_progress_bar?.visibility = View.GONE
    }

    private fun getPreviewImageObserver(rootView: WeakReference<View>): BitmapObserver {
        return object : BitmapObserver {
            override fun onBitmapResult(bitmap: Bitmap) {
                if (previewIsNeeded) {
                    rootView.get()?.imageView?.setImageBitmap(blur(bitmap))
                }
            }
        }
    }

    private fun getImageObserver(rootView: WeakReference<View>): ImageObserver {
        return object : ImageObserver {
            override fun onGifResult(drawable: GifDrawable, width: Int, height: Int) {
                previewIsNeeded = false
                rootView.get()?.let {
                    onImageLoadComplete(it, width, height)
                    it.imageView.setImageDrawable(drawable)
                }
            }

            override fun onBitmapResult(bitmap: Bitmap) {
                previewIsNeeded = false
                rootView.get()?.let {
                    onImageLoadComplete(it, bitmap.width, bitmap.height)
                    if (bitmap.width <= it.width) {
                        it.imageView.setImageBitmap(bitmap)
                    } else {
                        it.imageView.setImageBitmap(
                            Bitmap.createScaledBitmap(bitmap, it.width, it.height, false)
                        )
                    }
                }
            }

            override fun onException(e: Throwable) {
                shortToast(getResString(image_load_failed).format(e.message))
                dismiss()
            }

        }
    }

    private fun setTextSize(view: View, cropFactor: Float) {
        val buttonsList = listOf(view.btn_send, view.btn_open/*, view.btn_download*/)
        val minTextSize = view.context.resources.displayMetrics.scaledDensity * 8  // 8sp
        var newTextSize = buttonsList[0].textSize
        var needToChange = true

        buttonsList.forEach {
            if (it.lineCount > 1) needToChange = true
        }

        if (needToChange) {
            buttonsList.forEach {
                val optSize = ViewUtils.calculateOptimalTextSizeForSingleLine(
                    it, ((it.width - it.paddingLeft * 2) * cropFactor).toInt(),
                    minTextSize, it.textSize
                )
                newTextSize = kotlin.math.min(newTextSize, optSize)
            }

            buttonsList.forEach {
                it.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize)
            }
        }
    }
}

private fun View.setLayout(width: Int, height: Int) {
    layoutParams.width = width
    layoutParams.height = height
}