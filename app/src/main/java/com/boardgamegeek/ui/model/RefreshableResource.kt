package com.boardgamegeek.ui.model

enum class Status {
    SUCCESS,
    ERROR,
    REFRESHING
}

class RefreshableResource<out T> private constructor(val status: Status, val data: T?, val message: String? = null) {
    companion object {
        fun <T> success(data: T?): RefreshableResource<T> {
            return RefreshableResource(Status.SUCCESS, data)
        }

        fun <T> error(msg: String, data: T? = null): RefreshableResource<T> {
            return RefreshableResource(Status.ERROR, data, msg)
        }

        fun <T> error(t: Throwable?, data: T? = null): RefreshableResource<T> {
            return RefreshableResource(Status.ERROR, data, t?.localizedMessage ?: "")
        }

        fun <T> refreshing(data: T? = null): RefreshableResource<T> {
            return RefreshableResource(Status.REFRESHING, data)
        }
    }
}