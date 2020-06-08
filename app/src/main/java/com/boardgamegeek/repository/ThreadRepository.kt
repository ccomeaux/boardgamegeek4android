package com.boardgamegeek.repository

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.ArticleEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.ThreadArticlesEntity
import com.boardgamegeek.extensions.toMillis
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.ThreadResponse
import com.boardgamegeek.livedata.NetworkLoader
import com.boardgamegeek.util.XmlApi2MarkupConverter
import retrofit2.Call
import java.text.SimpleDateFormat
import java.util.*

class ThreadRepository(val application: BggApplication) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US)
    private val converter = XmlApi2MarkupConverter(application)

    fun getThread(threadId: Int): LiveData<RefreshableResource<ThreadArticlesEntity>> {
        return object : NetworkLoader<ThreadArticlesEntity, ThreadResponse>(application) {
            override val typeDescriptionResId: Int
                get() = R.string.title_forums

            override fun createCall(): Call<ThreadResponse> {
                return Adapter.createForXml().thread(threadId)
            }

            override fun parseResult(result: ThreadResponse): ThreadArticlesEntity {
                val articles = mutableListOf<ArticleEntity>()
                result.articles.forEach {
                    articles.add(ArticleEntity(
                            it.id,
                            it.username.orEmpty(),
                            it.link,
                            it.postdate.toMillis(dateFormat),
                            it.editdate.toMillis(dateFormat),
                            converter.toHtml(it.body?.trim().orEmpty()),
                            it.numedits
                    ))
                }
                return ThreadArticlesEntity(
                        result.id,
                        result.subject,
                        articles
                )
            }

        }.asLiveData()
    }
}