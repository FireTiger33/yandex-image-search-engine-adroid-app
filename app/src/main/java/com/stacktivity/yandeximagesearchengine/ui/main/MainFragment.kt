package com.stacktivity.yandeximagesearchengine.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.stacktivity.yandeximagesearchengine.R

class MainFragment : Fragment() {

    companion object {
        private var INSTANCE: MainFragment? = null
        fun getInstance() = INSTANCE
                ?: MainFragment().also {
                    INSTANCE = it
                }
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = MainViewModel()
    }

}