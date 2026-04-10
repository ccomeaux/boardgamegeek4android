@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Play
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.viewmodel.GamePlaysViewModel
import com.boardgamegeek.util.XmlApiMarkupConverter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class GamePlaysActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        val gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()
        val heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL).orEmpty()
        val thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL).orEmpty()
        val arePlayersCustomSorted = intent.getBooleanExtra(KEY_CUSTOM_PLAYER_SORT, false)

        @ColorInt
        val iconColor = intent.getIntExtra(KEY_ICON_COLOR, Color.TRANSPARENT)

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            val viewModel by viewModels<GamePlaysViewModel>()

            val plays by viewModel.plays.observeAsState()
            val isRefreshing by viewModel.isRefreshing.observeAsState(false)
            val refreshTimestamp by viewModel.refreshTimestamp.observeAsState(123456789L)
            val errorMessage by viewModel.errorMessage.observeAsState()
            val loggedPlayResult by viewModel.loggedPlayResult.observeAsState()

            viewModel.setGame(gameId)

            LaunchedEffect(errorMessage) {
                errorMessage?.getContentIfNotHandled()?.let {
                    coroutineScope.launch {
                        if (it.isNotBlank()) snackbarHostState.showSnackbar(it)
                    }
                }
            }
            LaunchedEffect(loggedPlayResult) {
                loggedPlayResult?.getContentIfNotHandled()?.let {
                    notifyLoggedPlay(it)
                }
            }

            Scaffold(
                topBar = {
                    GamePlaysTopBar(
                        gameName,
                        playCount = plays?.values?.sumOf { list -> list.sumOf { play -> play.quantity } } ?: 0,
                        refreshTimestamp = refreshTimestamp,
                        onUpClick = {
                            GameActivity.startUp(this, gameId, gameName, thumbnailUrl, heroImageUrl)
                            finish()
                        }
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    Fab(
                        stringResource(R.string.menu_log_play),
                        painterResource(R.drawable.log_play_24px),
                        iconColor,
                        onClick = {
                            when (preferences().logPlayPreference()) {
                                LOG_PLAY_TYPE_FORM -> LogPlayActivity.logPlay(
                                    this,
                                    gameId,
                                    gameName,
                                    heroImageUrl,
                                    arePlayersCustomSorted
                                )
                                LOG_PLAY_TYPE_QUICK -> viewModel.logQuickPlay(gameId, gameName)
                                LOG_PLAY_TYPE_WIZARD -> NewPlayActivity.start(this, gameId, gameName)
                            }
                        }
                    )
                }
            ) { contentPadding ->
                PlaysScreen(
                    plays,
                    isRefreshing = isRefreshing,
                    contentPadding,
                    onRefresh = { viewModel.refresh() },
                )
            }
        }
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        private const val KEY_THUMBNAIL_URL = "THUMBNAIL_URL"
        private const val KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT"
        private const val KEY_ICON_COLOR = "ICON_COLOR"

        fun start(
            context: Context,
            gameId: Int,
            gameName: String,
            heroImageUrl: String,
            thumbnailUrl: String,
            arePlayersCustomSorted: Boolean,
            @ColorInt iconColor: Int
        ) {
            context.startActivity(createIntent(context, gameId, gameName, heroImageUrl, thumbnailUrl, arePlayersCustomSorted, iconColor))
        }

        fun createIntent(
            context: Context,
            gameId: Int,
            gameName: String,
            heroImageUrl: String,
            thumbnailUrl: String = heroImageUrl,
            arePlayersCustomSorted: Boolean = false,
            @ColorInt iconColor: Int = Color.TRANSPARENT,
        ): Intent {
            return context.intentFor<GamePlaysActivity>(
                KEY_GAME_ID to gameId,
                KEY_GAME_NAME to gameName,
                KEY_HERO_IMAGE_URL to heroImageUrl,
                KEY_THUMBNAIL_URL to thumbnailUrl,
                KEY_CUSTOM_PLAYER_SORT to arePlayersCustomSorted,
                KEY_ICON_COLOR to iconColor,
            )
        }
    }
}

@Composable
private fun GamePlaysTopBar(
    gameName: String,
    playCount: Int,
    refreshTimestamp: Long,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onUpClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var relativeTimestamp by remember { mutableStateOf("") }
    LaunchedEffect(refreshTimestamp) {
        while (true) {
            relativeTimestamp = refreshTimestamp.formatTimestamp(context, includeTime = true).toString()
            delay(30.seconds)
        }
    }
    MediumFlexibleTopAppBar(
        title = { Text(gameName) },
        subtitle = {
            if (playCount > 0) {
                Text(pluralStringResource(R.plurals.plays_updated_suffix, playCount, playCount, relativeTimestamp))
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            UpAppBarAction(onUpClick)
        },
    )
}

@Composable
private fun Fab(
    text: String,
    iconPainter: Painter,
    color: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = androidx.compose.ui.graphics.Color(color.addAlphaToColor()),
            contentColor = androidx.compose.ui.graphics.Color(color.getTextColor().addAlphaToColor()),
        ) {
            Icon(
                iconPainter,
                contentDescription = text,
            )
        }
    }
}

@Composable
private fun PlaysScreen(
    plays: Map<String, List<Play>>?,
    isRefreshing: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onRefresh: () -> Unit,
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
                R.string.empty_plays_game,
                painterResource(R.drawable.plays_24px),
                padding = contentPadding,
            )
        }
        else -> {
            val context = LocalContext.current
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                val markupConverter = XmlApiMarkupConverter(context)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    //contentPadding = contentPadding,
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
                                showGameName = false,
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
}
