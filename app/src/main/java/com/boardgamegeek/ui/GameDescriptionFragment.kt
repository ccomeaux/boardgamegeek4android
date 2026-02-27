package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentComposeViewBinding
import com.boardgamegeek.databinding.FragmentNestedComposeViewBinding
import com.boardgamegeek.extensions.setWebViewText
import com.boardgamegeek.ui.compose.BggLoadingIndicatorBox
import com.boardgamegeek.ui.compose.EmptyContent
import com.boardgamegeek.ui.compose.GameFooter
import com.boardgamegeek.ui.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameDescriptionFragment : Fragment() {
    private var _binding: FragmentComposeViewBinding? = null
    private val binding get() = _binding!!

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentComposeViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.setContent {
            val viewModel by activityViewModels<GameViewModel>()
            val g by viewModel.game.observeAsState()
            val isRefreshing by viewModel.gameIsRefreshing.observeAsState(true)

            if (isRefreshing)
                BggLoadingIndicatorBox()

            val padding = PaddingValues(
                horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                vertical = dimensionResource(R.dimen.material_margin_vertical),
            )

            g?.let {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(padding)
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        factory = { context ->
                            WebView(context).apply {
                                setWebViewText(it.description)
                            }
                        }
                    )
                    GameFooter(
                        it.updated,
                        it.id,
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            } ?: EmptyContent(
                stringResource(R.string.empty_game),
                painterResource(R.drawable.game_24px),
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
