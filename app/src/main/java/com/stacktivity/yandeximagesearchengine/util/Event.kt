package com.stacktivity.yandeximagesearchengine.util

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
class Event<out T, O>(
    private val content: T,
    val isRepeatEvent: Boolean = false,
    private val onResult: ((result: O?) -> Unit)? = null
) {

    var hasBeenHandled = false
        private set

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content

    fun setResult(result: O?) {
        onResult?.let { it(result) }
    }
}