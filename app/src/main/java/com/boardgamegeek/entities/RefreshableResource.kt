package com.boardgamegeek.entities

enum class Status {
    SUCCESS,
    ERROR,
    REFRESHING
}

data class RefreshableResource<out T>(val status: Status, val data: T?, val message: String = "") {
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

        fun <T> map(source: RefreshableResource<*>, data: T? = null): RefreshableResource<T> {
            return RefreshableResource(source.status, data, source.message)
        }
    }
}