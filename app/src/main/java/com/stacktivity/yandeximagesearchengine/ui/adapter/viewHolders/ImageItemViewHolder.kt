package com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.DrawableContainer
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R.color
import com.stacktivity.yandeximagesearchengine.base.BaseImageViewHolder
import com.stacktivity.yandeximagesearchengine.data.ColorPixel
import com.stacktivity.yandeximagesearchengine.util.getColor
import com.stacktivity.yandeximagesearchengine.data.ImageData
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.util.BitmapUtils
import com.stacktivity.yandeximagesearchengine.util.ViewUtils.calculateViewHeight
import com.stacktivity.yandeximagesearchengine.util.image.ImageProvider
import com.stacktivity.yandeximagesearchengine.util.image.ImageObserver
import kotlinx.android.synthetic.main.image_with_progress_bar.view.*
import kotlinx.android.synthetic.main.item_image_list.view.*
import pl.droidsonroids.gif.GifDrawable

internal class ImageItemViewHolder(
    itemView: View,
    private val eventListener: EventListener,
    private val imageProvider: ImageProvider<ImageItem>
) : BaseImageViewHolder(itemView) {
    var maxImageWidth: Int = 0
    val innerRecyclerView: RecyclerView
        get() = itemView.other_image_list_rv

    interface EventListener {
        fun onLoadFailed(itemNum: Int, isVisible: Boolean)
        fun onSelectResolutionButtonClicked(button: View, data: List<ImageData>)
    }

    private abstract class CustomImageObserver : ImageObserver() {
        var requiredToShow = true
        override fun onGifResult(drawable: GifDrawable, width: Int, height: Int) {}
        override fun onException(e: Throwable) {}
    }

    companion object {
        val tag: String = ImageItemViewHolder::class.java.simpleName
    }

    lateinit var item: ImageItem
        private set
    val itemNum: Int
        get() = item.itemNum
    private var imageObserver: CustomImageObserver? = null
    private var previewImageObserver: CustomImageObserver? = null

    fun bind(item: ImageItem) {
        this.item = item
        reset()
        prepareImageView(item.dups[0].width, item.dups[0].height)
        bindTextViews()
        bindButtons(item.dups[0])
        showProgressBar()

        previewImageObserver = getPreviewImageObserver()
        imageObserver = getImageObserver()
        imageProvider.getImage(
            item = item,
            previewImageObserver = previewImageObserver,
            imageObserver = imageObserver!!
        )
    }

    private fun showProgressBar() {
        itemView.image_load_progress_bar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        itemView.image_load_progress_bar.visibility = View.GONE
    }

    fun showAdditionalProgressBar() {
        itemView.progress_bar.visibility = View.VISIBLE
    }

    fun hideAdditionalProgressBar() {
        itemView.progress_bar.visibility = View.GONE
    }

    private fun reset() {
        innerRecyclerView.adapter?.notifyDataSetChanged()
        previewImageObserver?.requiredToShow = false
        imageObserver?.requiredToShow = false
        itemView.imageView.setImageResource(color.colorImagePreview)
    }

    private fun getImageObserver(): CustomImageObserver {
        return object : CustomImageObserver() {
            override fun onBitmapResult(bitmap: Bitmap) {
                previewImageObserver?.requiredToShow = false
                if (requiredToShow) {
                    if (bitmap.width < maxImageWidth) {
                        applyBitmapToView(bitmap)
                    } else {
                        applyBitmapToView(
                            Bitmap.createScaledBitmap(
                                bitmap,
                                maxImageWidth,
                                calculateViewHeight(maxImageWidth, bitmap.width, bitmap.height),
                                false
                            )
                        )
                    }
                    hideProgressBar()
                }
            }

            override fun onGifResult(drawable: GifDrawable, width: Int, height: Int) {
                previewImageObserver?.requiredToShow = false
                if (requiredToShow) {
                    applyGifToView(drawable, width, height)
                    hideProgressBar()
                }
            }

            override fun onException(e: Throwable) {
                Log.d(tag, "load failed: $itemNum")
                eventListener.onLoadFailed(itemNum, requiredToShow)
            }
        }
    }

    private fun applyThumbToView(thumb: Bitmap) {
        itemView.imageView.setImageBitmap(BitmapUtils.blur(thumb))
        itemView.imageView.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY)
    }

    private fun applyBitmapToView(imageBitmap: Bitmap) {
        prepareImageView(imageBitmap.width, imageBitmap.height)
        itemView.imageView.setImageBitmap(imageBitmap)
        itemView.imageView.clearColorFilter()
    }

    private fun applyGifToView(drawable: GifDrawable, gifWidth: Int, gifHeight: Int) {
        prepareImageView(gifWidth, gifHeight)
        itemView.imageView.setImageDrawable(drawable)
        itemView.imageView.clearColorFilter()
    }

    private fun getPreviewImageObserver(): CustomImageObserver {
        return object : CustomImageObserver() {
            override fun onBitmapResult(bitmap: Bitmap) {
                if (item.colorSpace == null) {
                    BitmapUtils.getDominantColors(bitmap, 2) {
                        val resColorSpace = it.filter(this@ImageItemViewHolder::filterUsedColors)
                        item.colorSpace = resColorSpace
                        bindButtonsColor()
                    }
                }
                if (requiredToShow) {
                    applyThumbToView(bitmap)
                }
            }
        }
    }

    private fun prepareImageView(imageWidth: Int, imageHeight: Int) {
        val imageViewWidth: Int = maxImageWidth -
                (itemView.image_container.parent as ViewGroup).run {
                    paddingLeft + paddingRight
                } * 2
        val calcImageViewHeight = calculateViewHeight(imageViewWidth, imageWidth, imageHeight)
        itemView.imageView.run {
            clearAnimation()
            layoutParams.height = calcImageViewHeight
            requestLayout()
        }
        itemView.image_container.run {
            layoutParams.height = calcImageViewHeight
            requestLayout()
        }
    }

    private fun bindTextViews() {
        itemView.title.text = item.title
    }

    private fun bindButtons(data: ImageData) {
        itemView.btn_image_resolution.run {
            text = data.baseToString()

            setOnClickListener {
                eventListener.onSelectResolutionButtonClicked(it, item.dups)
            }
        }

        itemView.link_source.setOnClickListener {
            val uri = Uri.parse(item.sourceSite)
            it.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        bindButtonsColor()
    }

    private fun bindButtonsColor() {
        Log.d("ImageItemViewHOlder", "bind colors: ${item.colorSpace}, ${item.colorSpace?.size ?: 0 < 1}")

        if (item.colorSpace == null || item.colorSpace?.size ?: 0 < 1) return

        val colors = item.colorSpace!!

        itemView.run {
            btn_image_resolution.apply {
                val containerState = background.constantState as DrawableContainer.DrawableContainerState
                (containerState.getChild(1) as GradientDrawable).setColor(colors[0].toInt())
                (containerState.getChild(0) as GradientDrawable).setColor(colors[0].dark(0.85f).toInt())
            }
        }
    }

    private fun filterUsedColors(colorPixel: ColorPixel): Boolean {
        val color1 = ColorPixel.from(getColor(color.backgroundColor))
        val color2 = ColorPixel.from(itemView.btn_image_resolution.textColors.defaultColor)
        return colorPixel.euclidean(color1) > 100 && colorPixel.euclidean(color2) > 100
    }
}