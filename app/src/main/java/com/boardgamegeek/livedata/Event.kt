package com.boardgamegeek.livedata

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
open class Event<out T>(private val content: T) {

    private var hasBeenHandled = AtomicBoolean(false)

    /**
     * Returns the content and prevents its use again. Returns null if it has already been handled.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled.compareAndSet(false, true)) content else null
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    @Suppress("unused")
    fun peekContent(): T = content
}
