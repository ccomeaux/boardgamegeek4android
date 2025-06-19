package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.model.Status
import com.boardgamegeek.ui.compose.EmptyContent
import com.boardgamegeek.ui.compose.GeekListCommentList
import com.boardgamegeek.ui.compose.LoadingIndicator
import com.boardgamegeek.ui.viewmodel.GeekListViewModel

class GeekListCommentsFragment : Fragment(R.layout.fragment_compose_view) {
    private val viewModel by activityViewModels<GeekListViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val composeView = view.findViewById<ComposeView>(R.id.composeView)

        viewModel.geekList.observe(viewLifecycleOwner) {
            it?.let { (status, data, _) ->
                composeView.setContent {
                    val contentPadding = PaddingValues(
                        horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                        vertical = dimensionResource(R.dimen.material_margin_vertical),
                    )
                    if (data?.comments.isNullOrEmpty()) {
                        EmptyContent(
                            R.string.empty_comments,
                            painterResource(R.drawable.ic_twotone_comment_48),
                            Modifier.padding(contentPadding)
                        )
                    } else {
                        GeekListCommentList(
                            data?.comments.orEmpty(),
                            contentPadding = contentPadding
                        )
                    }
                    if (status == Status.ERROR) {
                        // TODO show error
                    }
                    if (status == Status.REFRESHING) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LoadingIndicator(
                                Modifier
                                    .align(Alignment.Center)
                                    .padding(dimensionResource(R.dimen.padding_extra))
                            )
                        }
                    }
                }
            }
        }
    }
}
