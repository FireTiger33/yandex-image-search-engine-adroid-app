package com.stacktivity.yandeximagesearchengine.ui.adapter

import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.data.ImageData
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.providers.ImageItemsProvider
import com.stacktivity.yandeximagesearchengine.providers.SubImagesProvider
import com.stacktivity.yandeximagesearchengine.ui.dialog.ImageDialog
import com.stacktivity.yandeximagesearchengine.util.prefetcher.PrefetchRecycledViewPool
import com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders.ImageItemViewHolder
import com.stacktivity.yandeximagesearchengine.ui.main.MainViewModel
import com.stacktivity.yandeximagesearchengine.util.getColor
import com.stacktivity.yandeximagesearchengine.util.getString
import com.stacktivity.yandeximagesearchengine.util.image.BufferedImageProvider
import com.stacktivity.yandeximagesearchengine.util.shortToast
import java.lang.ref.WeakReference

internal class ImageListAdapter(
    private val contentProvider: ImageItemsProvider,
    private val subImagesProvider: SubImagesProvider,
    private val imageLoader: BufferedImageProvider<String>,
    private val imageItemLoader: BufferedImageProvider<ImageItem>,
    private var maxImageWidth: Int
) : RecyclerView.Adapter<ImageItemViewHolder>(), ImageItemViewHolder.EventListener {

    private val selectedArray: SparseBooleanArray = SparseBooleanArray()
    private var parentView: WeakReference<ViewGroup>? = null


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        parentView = WeakReference(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_image_list, parent, false)
        val vh = ImageItemViewHolder(view, this, imageItemLoader)
        vh.innerRecyclerView.run {
            adapter = SimpleImageListAdapter(imageLoader).apply {
                setNewContentProvider(object :  // TODO
                    SimpleImageListAdapter.ContentProvider {
                    override fun getItemCount(): Int {
                        return subImagesProvider.getSubImagesCount(vh.itemNum)
                    }

                    override fun getItemOnPosition(position: Int): String {
                        return subImagesProvider.getSubImage(vh.itemNum, position)
                    }

                    override fun deleteItem(imageUrl: String): Int {
                        return subImagesProvider.deleteSubImage(vh.itemNum, imageUrl)
                    }
                })

                eventListener = object : SimpleImageListAdapter.EventListener {
                    override fun onImagesLoadFailed() {
                        toggleView(vh, false)
                    }
                }
            }
        }

        vh.itemView.setOnClickListener {
            val itemNum = vh.itemNum

            if (toggleView(vh)) {
                vh.showAdditionalProgressBar()
                subImagesProvider.loadSubImages(itemNum) { success, errorMsg ->
                    vh.hideAdditionalProgressBar()
                    if (success) {
                        // todo animate show
                    } else {
                        shortToast(getString(R.string.images_load_failed) + errorMsg)
                        toggleView(vh, false)
                    }
                }
            }
        }

        return vh
    }

    fun prefetchViewHolders(viewPool: PrefetchRecycledViewPool) {
        val currentRecycledViews = viewPool.getRecycledViewCount(0)
        if (currentRecycledViews < 8) {
            viewPool.setViewsCount(0, 8 - currentRecycledViews) { fakeParent, viewType ->
                onCreateViewHolder(fakeParent, viewType)
            }
        }
    }

    private fun toggleView(vh: ImageItemViewHolder, toShow: Boolean? = null): Boolean {
        val itemNum = vh.itemNum
        val isClicked = toShow ?: !selectedArray.get(itemNum, false)
        selectedArray.put(itemNum, isClicked)
        if (isClicked) {
            vh.itemView.setBackgroundColor(
                getColor(vh.itemView.context, R.color.itemOnClickOutSideColor)
            )
            vh.innerRecyclerView.visibility = View.VISIBLE
        } else {
            vh.itemView.background = null
            vh.innerRecyclerView.visibility = View.GONE
        }

        return isClicked
    }

    override fun onLoadFailed(itemNum: Int, isVisible: Boolean) {
        contentProvider.deleteImageItem(itemNum)?.let {
            if (isVisible) {
                notifyItemRemoved(it)
            }
        }
    }

    override fun onSelectResolutionButtonClicked(button: View, data: List<ImageData>) {
        val imageSelectionMenu = PopupMenu(parentView!!.get()!!.context, button)
        imageSelectionMenu.menu.clear()
        data.forEach { item ->
            imageSelectionMenu.menu.add(item.baseToString())
                .setOnMenuItemClickListener {
                    openImageDialog(item)
                    true
                }
        }
        imageSelectionMenu.show()
        MainViewModel.getInstance().showedMenu.value = imageSelectionMenu
    }

    fun onReloadData() {
        selectedArray.clear()
        notifyDataSetChanged()
    }

    fun onChangeScreenConfiguration(maxImageWidth: Int) {
        this.maxImageWidth = maxImageWidth
    }

    override fun getItemCount() = contentProvider.getItemCount()

    override fun onBindViewHolder(holder: ImageItemViewHolder, positionVh: Int) {
        val item = contentProvider.getItemOnPosition(positionVh)
        holder.maxImageWidth = maxImageWidth
        holder.bind(item)

        val isExpanded = toggleView(holder, selectedArray.get(item.itemNum, false))
        if (isExpanded && subImagesProvider.getSubImagesCount(item.itemNum) > 0) {
            toggleView(holder, true)
        }
    }

    private fun openImageDialog(imageData: ImageData) {
        parentView?.get()?.context?.let {
            val dialog = ImageDialog.newInstance(imageData)
            it as AppCompatActivity
            dialog.show(it.supportFragmentManager, dialog.tag)
        } ?: shortToast(R.string.unexpected_error)
    }
}