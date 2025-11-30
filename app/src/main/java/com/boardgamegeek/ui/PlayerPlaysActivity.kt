package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.Play
import com.boardgamegeek.ui.compose.*
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
                Icon(
                    painterResource(R.drawable.arrow_back_24px),
                    contentDescription = stringResource(R.string.up)
                )
            }
        },
    )
}

@Composable
private fun PlayerPlaysScreen(
    plays: Map<String, List<Play>>?,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    when {
        plays == null -> {
            BggLoadingIndicatorBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        }
        plays.isEmpty() -> {
            EmptyFullSizeScrollableContent(
                R.string.empty_plays_player,
                painterResource(R.drawable.plays_24px),
                padding = contentPadding,
            )
        }
        else -> {
            val context = LocalContext.current
            val markupConverter = XmlApiMarkupConverter(context)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
