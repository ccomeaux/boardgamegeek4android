package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.Play
import com.boardgamegeek.ui.compose.BggLoadingIndicator
import com.boardgamegeek.ui.compose.Drawer
import com.boardgamegeek.ui.compose.EmptyContent
import com.boardgamegeek.ui.compose.ListHeader
import com.boardgamegeek.ui.compose.PlayListItem
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.PlayerPlaysViewModel
import com.boardgamegeek.util.XmlApiMarkupConverter
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class PlayerPlaysActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val playerName = intent.getStringExtra(KEY_PLAYER_NAME).orEmpty()

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerPlays")
                param(FirebaseAnalytics.Param.ITEM_NAME, playerName)
            }
        }

        setContent {
            val viewModel by viewModels<PlayerPlaysViewModel>()
            val plays by viewModel.plays.observeAsState()
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            viewModel.setPlayerName(playerName)

            BggAppTheme {
                Drawer {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            PlayerPlaysTopBar(
                                playerName = playerName,
                                playCount = plays?.values?.sumOf { list -> list.sumOf { play -> play.quantity } } ?: 0,
                                scrollBehavior = scrollBehavior,
                                onUpClick = {
                                    BuddyActivity.startUp(this, null, playerName)
                                    finish()
                                },
                            )
                        }
                    ) { contentPadding ->
                        PlayerPlaysScreen(
                            plays,
                            contentPadding = contentPadding,
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_PLAYER_NAME = "PLAYER_NAME"

        fun start(context: Context, playerName: String?) {
            context.startActivity<PlayerPlaysActivity>(
                KEY_PLAYER_NAME to playerName,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerPlaysTopBar(
    playerName: String,
    playCount: Int,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onUpClick: () -> Unit = {},
) {
    MediumFlexibleTopAppBar(
        title = { Text(playerName) },
        subtitle = {
            if (playCount > 0) {
                Text(pluralStringResource(R.plurals.plays_suffix, playCount, playCount))
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = { onUpClick() }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(R.string.up))
            }
        },
    )
}

@Composable
private fun PlayerPlaysScreen(
    plays: Map<String, List<Play>>?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    when {
        plays == null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                BggLoadingIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .padding(dimensionResource(R.dimen.padding_extra))
                )
            }
        }
        plays.isEmpty() -> {
            EmptyContent(
                stringResource(R.string.empty_plays_player),
                Icons.Outlined.Event,
                modifier
                    .padding(contentPadding)
                    .fillMaxSize()
            )
        }
        else -> {
            val context = LocalContext.current
            val markupConverter = XmlApiMarkupConverter(context)
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                plays.forEach { (headerText, plays) ->
                    stickyHeader {
                        ListHeader(headerText)
                    }
                    items(
                        items = plays,
                        key = { it.internalId },
                    ) {
                        PlayListItem(
                            it,
                            showGameName = true,
                            markupConverter = markupConverter,
                        ) {
                            PlayActivity.start(context, it.internalId)
                        }
                    }
                }
            }
        }
    }
}
