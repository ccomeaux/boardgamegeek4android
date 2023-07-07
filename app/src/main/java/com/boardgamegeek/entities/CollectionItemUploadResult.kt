package com.boardgamegeek.entities

data class CollectionItemUploadResult private constructor(val item: CollectionItemForUploadEntity, val status: Status) {
    enum class Status {
        NEW,
        UPDATE,
        DELETE,
    }

    companion object {
        fun delete(item: CollectionItemForUploadEntity) = CollectionItemUploadResult(item, Status.DELETE)
        fun insert(item: CollectionItemForUploadEntity) = CollectionItemUploadResult(item, Status.NEW)
        fun update(item: CollectionItemForUploadEntity) = CollectionItemUploadResult(item, Status.UPDATE)
    }
}