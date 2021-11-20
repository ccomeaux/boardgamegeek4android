package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.entities.ArticleEntity
import com.boardgamegeek.extensions.setWebViewText
import com.boardgamegeek.extensions.toFormattedString
import kotlinx.android.synthetic.main.fragment_article.*

class ArticleFragment : Fragment(R.layout.fragment_article) {
    private var article = ArticleEntity()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        article = arguments?.getParcelable(KEY_ARTICLE) ?: ArticleEntity()

        usernameView.text = article.username
        postDateView.timestamp = article.postTicks
        if (article.numberOfEdits > 0) {
            editDateView.format = resources.getQuantityString(R.plurals.edit_timestamp, article.numberOfEdits)
            editDateView.formatArg = article.numberOfEdits.toFormattedString()
            editDateView.timestamp = article.editTicks
            editDateView.isVisible = true
        } else {
            editDateView.isVisible = false
        }
        bodyView.setWebViewText(article.body)
    }

    companion object {
        private const val KEY_ARTICLE = "ARTICLE"
        fun newInstance(article: ArticleEntity): ArticleFragment {
            return ArticleFragment().apply {
                arguments = bundleOf(KEY_ARTICLE to article)
            }
        }
    }
}
