package com.boardgamegeek.filterer

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.entities.CollectionItemEntity

abstract class CollectionFilterer(protected val context: Context) {

    @get:StringRes
    abstract val typeResourceId: Int

    val type: Int
        get() = context.getString(typeResourceId, CollectionFiltererFactory.TYPE_UNKNOWN).toIntOrNull()
                ?: CollectionFiltererFactory.TYPE_UNKNOWN

    abstract fun toShortDescription(): String

    open fun toLongDescription(): String = toShortDescription()

    open fun filter(item: CollectionItemEntity): Boolean = true

    val isValid: Boolean
        get() = toShortDescription().isNotEmpty()

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
