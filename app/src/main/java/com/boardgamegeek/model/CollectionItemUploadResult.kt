package com.boardgamegeek.model

class CollectionItemUploadResult private constructor(val item: CollectionItem, val status: Status) {
    enum class Status {
        NEW,
        UPDATE,
        DELETE,
    }

    companion object {
        fun delete(item: CollectionItem) = CollectionItemUploadResult(item, Status.DELETE)
        fun insert(item: CollectionItem) = CollectionItemUploadResult(item, Status.NEW)
        fun update(item: CollectionItem) = CollectionItemUploadResult(item, Status.UPDATE)
    }
}