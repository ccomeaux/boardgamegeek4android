package com.boardgamegeek.entities

data class CollectionItemUploadResult private constructor(val status: Status) {
    enum class Status {
        NEW,
        UPDATE,
        DELETE,
    }

    companion object {
        fun delete() = CollectionItemUploadResult(Status.DELETE)
        fun insert() = CollectionItemUploadResult(Status.NEW)
        fun update() = CollectionItemUploadResult(Status.UPDATE)
    }
}