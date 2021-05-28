package com.stacktivity.yandeximagesearchengine.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Used as a base class for long running tasks that only need to be run once,
 * such as downloading a file, to avoid running the same action again.
 *
 * All calls to this class are thread safe.
 *
 * For correct operation, it is required to guarantee the uniqueness of taskId.hashCode()
 *
 * @param taskId - task uniqueness marker
 * @param poolTag - used to delete only a certain range of tasks
 */
abstract class SmartTask(taskId: Any, val poolTag: String) {
    private val taskHash: Int = taskId.hashCode()

    /**
     * Call this function when the task completes.
     * Removes a task from list of running tasks
     */
    protected fun onTaskCompleted() {
        loadList[poolTag]!!.remove(taskHash)
    }

    /**
     * Call this function when the task starts running.
     * Adds a task to the list of running tasks
     */
    protected fun onTaskStarted() {
        if (!loadList.containsKey(poolTag)) loadList[poolTag] = ConcurrentHashMap()
        loadList[poolTag]!![taskHash] = this
    }

    /**
     * Used to get an instance of a task by taskId and
     * determine if the task is running.
     *
     * @return instance of a task or null if task is not running
     */
    protected fun getStartedTaskById(): SmartTask? {
        return loadList[poolTag]?.get(taskHash)
    }

    companion object {
        private val loadList = HashMap<String, ConcurrentHashMap<Int, SmartTask>>()

        internal fun clearTasksByPoolTag(poolTag: String) {
            loadList[poolTag]?.clear()
        }
    }
}