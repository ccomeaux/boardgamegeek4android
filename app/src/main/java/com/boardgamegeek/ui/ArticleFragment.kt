package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentArticleBinding
import com.boardgamegeek.entities.ArticleEntity
import com.boardgamegeek.extensions.setWebViewText
import com.boardgamegeek.extensions.toFormattedString

class ArticleFragment : Fragment() {
    private var _binding: FragmentArticleBinding? = null
    private val binding get() = _binding!!

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentArticleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val article = arguments?.getParcelable(KEY_ARTICLE) ?: ArticleEntity()

        binding.usernameView.text = article.username
        binding.postDateView.timestamp = article.postTicks
        if (article.numberOfEdits > 0) {
            binding.editDateView.format = resources.getQuantityString(R.plurals.edit_timestamp, article.numberOfEdits)
            binding.editDateView.formatArg = article.numberOfEdits.toFormattedString()
            binding.editDateView.timestamp = article.editTicks
            binding.editDateView.isVisible = true
        } else {
            binding.editDateView.isVisible = false
        }
        binding.bodyView.setWebViewText(article.body)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
