package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowThreadArticleBinding
import com.boardgamegeek.model.Article
import com.boardgamegeek.model.Forum
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.ArticleActivity
import com.boardgamegeek.ui.adapter.ThreadRecyclerViewAdapter.ArticleViewHolder
import com.boardgamegeek.util.XmlApi2TagHandler
import kotlin.properties.Delegates

class ThreadRecyclerViewAdapter(
    private val forumId: Int,
    private val forumTitle: String,
    private val objectId: Int,
    private val objectName: String,
    private val objectType: Forum.Type,
) : RecyclerView.Adapter<ArticleViewHolder>(), AutoUpdatableAdapter {

    var threadId: Int = BggContract.INVALID_ID
    var threadSubject: String = ""
    val tagHandler = XmlApi2TagHandler()

    var articles: List<Article> by Delegates.observable(emptyList()) { _, old, new ->
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

    inner class ArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = RowThreadArticleBinding.bind(itemView)

        fun bind(article: Article?) {
            article?.let {
                if (it.postTicks > 0L) {
                    binding.rowHeaderView.isVisible = true
                    binding.usernameView.text = it.username
                    binding.usernameView.isVisible = it.username.isNotBlank()
                    binding.postDateView.timestamp = it.postTicks
                    binding.editDateView.timestamp = it.editTicks
                    binding.editDateView.isVisible = it.editTicks != it.postTicks
                    binding.dateDivider.isVisible = it.editTicks != it.postTicks
                } else {
                    binding.rowHeaderView.isVisible = false
                }
                binding.bodyView.setTextMaybeHtml(it.body.trim(), tagHandler = tagHandler)
                binding.viewButton.setOnClickListener { v: View ->
                    ArticleActivity.start(v.context, threadId, threadSubject, forumId, forumTitle, objectId, objectName, objectType, it)
                }
            }
        }
    }
}
