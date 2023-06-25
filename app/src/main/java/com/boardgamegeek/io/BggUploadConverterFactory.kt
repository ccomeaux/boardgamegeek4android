package com.boardgamegeek.io

import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.boardgamegeek.io.model.PlayPostResponse
import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class BggUploadConverterFactory private constructor() : Converter.Factory() {
    override fun responseBodyConverter(type: Type, annotations: Array<out Annotation>, retrofit: Retrofit): Converter<ResponseBody, *>? {
        return if (type == PlayPostResponse::class.java) {
            PlayEntityConverter()
        } else null
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
