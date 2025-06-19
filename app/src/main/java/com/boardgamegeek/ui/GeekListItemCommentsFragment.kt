package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getParcelableArrayListCompat
import com.boardgamegeek.model.GeekListComment
import com.boardgamegeek.ui.compose.EmptyContent
import com.boardgamegeek.ui.compose.GeekListCommentList

class GeekListItemCommentsFragment : Fragment(R.layout.fragment_compose_view) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val comments = arguments?.getParcelableArrayListCompat<GeekListComment>(KEY_COMMENTS).orEmpty()

        view.findViewById<ComposeView>(R.id.composeView).setContent {
            val contentPadding = PaddingValues(
                horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                vertical = dimensionResource(R.dimen.material_margin_vertical),
            )
            if (comments.isEmpty()) {
                EmptyContent(
                    R.string.empty_comments,
                    painterResource(R.drawable.ic_twotone_comment_48),
                    Modifier.padding(contentPadding),
                )
            } else {
                GeekListCommentList(
                    comments,
                    contentPadding = contentPadding,
                )
            }
        }
    }

    companion object {
        private const val KEY_COMMENTS = "GEEK_LIST_COMMENTS"

        fun newInstance(comments: List<GeekListComment>?): GeekListItemCommentsFragment {
            return GeekListItemCommentsFragment().apply {
                arguments = bundleOf(KEY_COMMENTS to comments)
            }
        }
    }
}
