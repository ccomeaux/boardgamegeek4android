package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.ArticleEntity
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.ArticleActivity
import com.boardgamegeek.ui.adapter.ThreadRecyclerViewAdapter.ArticleViewHolder
import com.boardgamegeek.util.XmlApi2TagHandler
import kotlinx.android.synthetic.main.row_thread_article.view.*
import kotlin.properties.Delegates

class ThreadRecyclerViewAdapter(
        private val forumId: Int,
        private val forumTitle: String,
        private val objectId: Int,
        private val objectName: String,
        private val objectType: ForumEntity.ForumType) : RecyclerView.Adapter<ArticleViewHolder>(), AutoUpdatableAdapter {

    var threadId: Int = BggContract.INVALID_ID
    var threadSubject: String = ""
    val tagHandler = XmlApi2TagHandler()

    var articles: List<ArticleEntity> by Delegates.observable(emptyList()) { _, old, new ->
        autoNotify(old, new) { o, n ->
            o == n
        }
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        return ArticleViewHolder(parent.inflate(R.layout.row_thread_article))
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        holder.bind(articles.getOrNull(position))
    }

    override fun getItemCount() = articles.size

    override fun getItemId(position: Int): Long {
        return articles.getOrNull(position)?.id?.toLong() ?: RecyclerView.NO_ID
    }

    fun getPosition(articleId: Int): Int {
        return articles.indexOfFirst { it.id == articleId }
    }

    inner class ArticleViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(article: ArticleEntity?) {
            if (article == null) return
            if (article.postTicks > 0L) {
                itemView.rowHeaderView.visibility = View.VISIBLE
                itemView.usernameView.text = article.username
                itemView.usernameView.isVisible = article.username.isNotBlank()
                itemView.postDateView.timestamp = article.postTicks
                itemView.editDateView.timestamp = article.editTicks
                itemView.editDateView.isVisible = article.editTicks != article.postTicks
                itemView.dateDivider.isVisible = article.editTicks != article.postTicks
            } else {
                itemView.rowHeaderView.visibility = View.GONE
            }
            itemView.bodyView.setTextMaybeHtml(article.body.trim(), tagHandler = tagHandler)
            itemView.viewButton.setOnClickListener { v: View ->
                ArticleActivity.start(v.context, threadId, threadSubject, forumId, forumTitle, objectId, objectName, objectType, article)
            }
        }
    }
}