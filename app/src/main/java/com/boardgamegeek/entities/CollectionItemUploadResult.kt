package com.boardgamegeek.entities

data class CollectionItemUploadResult private constructor(val item: CollectionItemEntity, val status: Status) {
    enum class Status {
        NEW,
        UPDATE,
        DELETE,
    }

    companion object {
        fun delete(item: CollectionItemEntity) = CollectionItemUploadResult(item, Status.DELETE)
        fun insert(item: CollectionItemEntity) = CollectionItemUploadResult(item, Status.NEW)
        fun update(item: CollectionItemEntity) = CollectionItemUploadResult(item, Status.UPDATE)
    }
}