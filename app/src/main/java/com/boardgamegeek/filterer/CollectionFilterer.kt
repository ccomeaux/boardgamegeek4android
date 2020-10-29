package com.boardgamegeek.filterer

import android.content.Context
import androidx.annotation.StringRes

abstract class CollectionFilterer(protected val context: Context) {

    @get:StringRes
    abstract val typeResourceId: Int

    val type: Int
        get() = context.getString(typeResourceId, CollectionFiltererFactory.TYPE_UNKNOWN).toIntOrNull()
                ?: CollectionFiltererFactory.TYPE_UNKNOWN

    abstract fun toShortDescription(): String

    open fun toLongDescription(): String = toShortDescription()

    open fun getColumns(): Array<String>? = null

    abstract fun getSelection(): String

    abstract fun getSelectionArgs(): Array<String>?

    val isValid: Boolean
        get() = toShortDescription().isNotEmpty() && getSelection().isNotEmpty()

    abstract fun inflate(data: String)

    abstract fun deflate(): String

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is CollectionFilterer) {
            return false
        }
        return other.type == this.type
    }

    override fun hashCode() = type

    companion object {
        const val DELIMITER = ":"
    }
}
