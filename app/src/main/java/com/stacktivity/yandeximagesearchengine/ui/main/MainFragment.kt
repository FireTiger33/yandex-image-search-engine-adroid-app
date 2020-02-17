package com.stacktivity.yandeximagesearchengine.ui.main

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.PAGE_SIZE
import kotlinx.android.synthetic.main.main_fragment.*

class MainFragment : Fragment() {
    private lateinit var viewModel: MainViewModel
    private var layoutManager: LinearLayoutManager = LinearLayoutManager(activity)

    companion object {
        private var INSTANCE: MainFragment? = null
        fun getInstance() = INSTANCE
                ?: MainFragment().also {
                    INSTANCE = it
                }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = MainViewModel.getInstance()

        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupImageList()
        setupObservers()
    }

    private fun setupImageList() {
        val size = Point()
        activity!!.windowManager.defaultDisplay.getSize(size)
        val masImageWidth = size.x
        image_list_rv.layoutManager = layoutManager
        image_list_rv.adapter = viewModel.getImageItemListAdapter(masImageWidth)
        image_list_rv.addOnScrollListener(getImageScrollListener())
    }

    private fun setupObservers() {
        viewModel.dataLoading.observe(viewLifecycleOwner, Observer {
            progress_bar.visibility =
                if (it /*&& viewModel.empty.value != false*/) View.VISIBLE
                else View.GONE
        })

        viewModel.newQueryIsLoaded.observe(viewLifecycleOwner, Observer {
            if (it && layoutManager.itemCount > 0) {
                image_list_rv.scrollToPosition(0)
            }
        })
    }

    private fun getImageScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val itemCount = layoutManager.itemCount
                val lastVisibleItemPosition: Int = layoutManager.findLastVisibleItemPosition()

                progress_bar.visibility =
                    if (viewModel.dataLoading.value != false && lastVisibleItemPosition + 1 == layoutManager.itemCount) View.VISIBLE
                    else View.GONE

                if (itemCount - lastVisibleItemPosition <= PAGE_SIZE * 0.4) {
                    viewModel.fetchImagesOnNextPage()
                }
            }
        }
    }

    fun showSearchResult(query: String) {
        viewModel.fetchImagesOnQuery(query)
    }
}