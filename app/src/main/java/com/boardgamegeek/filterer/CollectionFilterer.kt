package com.boardgamegeek.filterer

import android.content.Context
import android.support.annotation.StringRes

abstract class CollectionFilterer(protected val context: Context) {

    @get:StringRes
    abstract val typeResourceId: Int

    val type: Int
        get() = context.getString(typeResourceId, CollectionFiltererFactory.TYPE_UNKNOWN).toIntOrNull()
                ?: CollectionFiltererFactory.TYPE_UNKNOWN

    abstract fun getDisplayText(): String

    open fun getDescription(): String = getDisplayText()

    open fun getColumns(): Array<String>? = null

    abstract fun getSelection(): String

    abstract fun getSelectionArgs(): Array<String>?

    open fun getHaving(): String? = null

    val isValid: Boolean
        get() = getDisplayText().isNotEmpty() &&
                (getSelection().isNotEmpty() || !getHaving().isNullOrEmpty())

    abstract fun setData(data: String)

    abstract fun flatten(): String

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
