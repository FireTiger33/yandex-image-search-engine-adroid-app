package com.stacktivity.yandeximagesearchengine.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar

/**
 * Contains tools for animated show/hide ToolBar
 */
class ToolbarDemonstrator {
    companion object {
        private var toolbarHeight: Int = 0
        private var mVaActionBar: ValueAnimator? = null

        /**
         * @param toolbar [Toolbar] that was passed to the [androidx.appcompat.app.AppCompatActivity].setSupportActionBar()
         * @param actionBar [ActionBar] obtained from method [androidx.appcompat.app.AppCompatActivity].getSupportActionBar()
         * @param animateDuration the duration of animation in milliseconds
         */
        fun hideActionBar(toolbar: Toolbar, actionBar: ActionBar, animateDuration: Int) {
            if (!actionBar.isShowing) {
                return
            }

            if (toolbarHeight == 0) {
                toolbarHeight = toolbar.height
            }

            if (mVaActionBar != null && mVaActionBar!!.isRunning) {
                return
            }
            val actionBarVa = ValueAnimator.ofInt(toolbarHeight, 0)
            actionBarVa.addUpdateListener { animation ->
                toolbar.layoutParams.height = (animation.animatedValue as Int)
                toolbar.requestLayout()
            }
            actionBarVa.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    actionBar.hide()
                }
            })
            actionBarVa.duration = animateDuration.toLong()
            actionBarVa.start()

            mVaActionBar = actionBarVa
        }


        /**
         * @param toolbar [Toolbar] that was passed to the [androidx.appcompat.app.AppCompatActivity].setSupportActionBar()
         * @param actionBar [ActionBar] obtained from method [androidx.appcompat.app.AppCompatActivity].getSupportActionBar()
         * @param animateDuration the duration of animation in milliseconds
         */
        fun showActionBar(toolbar: Toolbar, actionBar: ActionBar, animateDuration: Int) {
            if (actionBar.isShowing) {
                return
            }

            if (mVaActionBar != null && mVaActionBar!!.isRunning) {
                return
            }

            val actionBarVa = ValueAnimator.ofInt(0, toolbarHeight)
            actionBarVa.addUpdateListener { animation ->
                toolbar.layoutParams.height = (animation.animatedValue as Int)
                toolbar.requestLayout()
            }
            actionBarVa.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    actionBar.show()
                }
            })
            actionBarVa.duration = animateDuration.toLong()
            actionBarVa.start()
            mVaActionBar = actionBarVa
        }
    }
}
