package com.boardgamegeek.mappers

import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.toMillis
import com.boardgamegeek.io.model.*
import com.boardgamegeek.util.ForumXmlApiMarkupConverter
import java.text.SimpleDateFormat
import java.util.*

fun ForumRemote.mapToModel() = Forum(
    id = this.id,
    title = this.title,
    numberOfThreads = this.numthreads,
    lastPostDateTime = this.lastpostdate.toMillis(SimpleDateFormat(LONG_DATE_PATTERN, Locale.US)),
    isHeader = this.noposting == 1,
)

fun ThreadRemote.mapToModel() = Thread(
    threadId = id,
    subject = subject.orEmpty().trim(),
    author = author.orEmpty().trim(),
    numberOfArticles = numarticles,
    lastPostDate = lastpostdate.toMillis(SimpleDateFormat(LONG_DATE_PATTERN, Locale.US)),
)

fun ForumResponse.mapToModel() = ForumThreads(
    numberOfThreads = this.numthreads.toIntOrNull() ?: 0,
    threads = this.threads.orEmpty().map { it.mapToModel() }
)

fun ArticleRemote.mapToModel(converter: ForumXmlApiMarkupConverter): Article {
    val dateFormat = SimpleDateFormat(DATE_PATTERN, Locale.US)
    return Article(
        id = this.id,
        username = this.username.orEmpty(),
        link = this.link,
        postTicks = this.postdate.toMillis(dateFormat),
        editTicks = this.editdate.toMillis(dateFormat),
        body = converter.toHtml(this.body?.trim().orEmpty()),
        numberOfEdits = this.numedits,
    )
}

fun ThreadResponse.mapToModel(converter: ForumXmlApiMarkupConverter) = ThreadArticles(
    threadId = this.id,
    subject = this.subject,
    articles = this.articles.map { it.mapToModel(converter) }
)

private const val LONG_DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss Z"

@Suppress("SpellCheckingInspection")
private const val DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssz"
