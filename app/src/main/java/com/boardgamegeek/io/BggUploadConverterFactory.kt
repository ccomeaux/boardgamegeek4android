package com.boardgamegeek.io

import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.boardgamegeek.io.model.CollectionPostResponse
import com.boardgamegeek.io.model.PlayPostResponse
import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class BggUploadConverterFactory private constructor() : Converter.Factory() {
    override fun responseBodyConverter(type: Type, annotations: Array<out Annotation>, retrofit: Retrofit): Converter<ResponseBody, *>? {
        return when (type) {
            PlayPostResponse::class.java -> PlayEntityConverter()
            CollectionPostResponse::class.java -> CollectionEntityConverter()
            else -> null
        }
    }

    companion object {
        fun create(): Converter.Factory = BggUploadConverterFactory()
    }
}

class PlayEntityConverter : Converter<ResponseBody, Any> {
    private val gson = Gson()

    override fun convert(value: ResponseBody): Any? {
        // <div class='messagebox error'>
        //     You can't delete this play
        // </div>

        // <div class='messagebox error'>
        //     Play does not exist.
        // </div>

        val content = value.string().trim()
        return if (content.startsWith(ERROR_DIV)) {
            PlayPostResponse(
                error = HtmlCompat.fromHtml(content, FROM_HTML_MODE_LEGACY).toString().trim(),
                success = false
            )
        } else {
            gson.fromJson(content, PlayPostResponse::class.java)
        }
    }

    companion object {
        private const val ERROR_DIV = "<div class='messagebox error'>"
    }
}

class CollectionEntityConverter : Converter<ResponseBody, Any> {
    override fun convert(value: ResponseBody): Any? {
        // <div class='messagebox error'>
        //   Invalid action
        // </div>
        // <div class='messagebox error'>
        //   You must <a href="/login">login</a> to use the collection utilities.
        // </div>

        // DELETE = empty content

        // STATUS example
        // :FROM:
        // <div class='owned'>Owned</div>
        // <div class='wishlist'>Wishlist(3)
        // <br>&nbsp;(Like&nbsp;to&nbsp;have)
        // </div>
        // :TO:
        // Owned

        // PRIVATE INFO
        //	<table>
        //	<tr>
        //	<td nowrap width='100'>Quantity:</td>
        //	<td nowrap>1</td>
        //	</tr>
        //	<tr>
        //	<td width='100'>Comments:</td>
        //	<td>Private</td>
        //	</tr>
        //	</table>

        // HAS PARTS:
        // returns the text entered

        // RATING:
        //        private const val N_A_SPAN = "<span>N/A</span>"
        //        private const val RATING_DIV = "<div class='ratingtext'>"
        //        const val INVALID_RATING = -1.0
        //        rating = when {
        //            content.contains(CollectionRatingUploadTask.N_A_SPAN) -> CollectionRatingUploadTask.INVALID_RATING
        //            content.contains(CollectionRatingUploadTask.RATING_DIV) -> {
        //                var index = content.indexOf(CollectionRatingUploadTask.RATING_DIV) + CollectionRatingUploadTask.RATING_DIV.length
        //                var message = content.substring(index)
        //                index = message.indexOf("<")
        //                if (index > 0) {
        //                    message = message.substring(0, index)
        //                }
        //                message.trim().toDoubleOrNull() ?: CollectionRatingUploadTask.INVALID_RATING
        //            }
        //            else -> CollectionRatingUploadTask.INVALID_RATING
        //        }

        val content = value.string().trim()
        val html = HtmlCompat.fromHtml(content, FROM_HTML_MODE_LEGACY).toString().trim()
        return if (content.startsWith(ERROR_DIV)) {
            CollectionPostResponse(error = html)
        } else {
            CollectionPostResponse()
        }
    }

    companion object {
        private const val ERROR_DIV = "<div class='messagebox error'>"
    }
}
