package com.boardgamegeek.util

import android.net.Uri

object HttpUtils {
    /**
     * Encodes `s` using UTF-8 using the format required by `application/x-www-form-urlencoded` MIME content type.
     */
    fun String?.encodeForUrl(): String? = Uri.encode(this, "UTF-8")
}
