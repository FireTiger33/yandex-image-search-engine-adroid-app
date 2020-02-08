package com.stacktivity.yandeximagesearchengine.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

fun showKeyboard(context: Context, editTextView: View) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(editTextView, InputMethodManager.SHOW_IMPLICIT)
}

fun hideKeyboard(context: Context, editTextView: View) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(editTextView.windowToken, 0)
}