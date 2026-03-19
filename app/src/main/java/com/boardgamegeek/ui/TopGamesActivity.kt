package com.boardgamegeek.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.extensions.linkBgg
import com.boardgamegeek.extensions.notifyLoggedPlay
import com.boardgamegeek.extensions.shareGames
import com.boardgamegeek.extensions.toggle
import com.boardgamegeek.model.Status
import com.boardgamegeek.model.TopGame
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.TopGamesViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TopGamesActivity : BaseActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Top Games")
            }
        }
        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val snackbarHostState = remember { SnackbarHostState() }
            val selectedIds = remember { mutableStateSetOf<Int>() }
            val inSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }
            var showQuickLogPlayDialog by remember { mutableStateOf(false) }

            val viewModel by viewModels<TopGamesViewModel>()
            val topGames by viewModel.topGames.observeAsState()
            val loggedPlayResult by viewModel.loggedPlayResult.observeAsState()
            val errorMessage by viewModel.errorMessage.observeAsState()

            fun nameFromId(id: Int): String? =
                topGames?.data?.find { result ->
                    result.id == id
                }?.name

            fun thumbnailFromId(id: Int): String =
                topGames?.data?.find { result ->
                    result.id == id
                }?.thumbnailUrl.orEmpty()

            LaunchedEffect(loggedPlayResult) {
                loggedPlayResult?.getContentIfNotHandled()?.let {
                    context.notifyLoggedPlay(it)
                }
            }
            LaunchedEffect(errorMessage) {
                errorMessage?.getContentIfNotHandled()?.let {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(it)
                    }
                }
            }

            BggAppTheme {
                Drawer(
                    drawerState = drawerState,
                    selectedItem = DrawerItem.TopGames,
                ) {
                    Scaffold(
                        topBar = {
                            if (inSelectionMode) {
                                MultiSelectionTopAppBar(
                                    selectedCount = selectedIds.size,
                                    Authenticator.isSignedIn(LocalContext.current),
                                    onClear = { selectedIds.clear() },
                                    onLogPlay = {
                                        selectedIds.firstOrNull()?.let { gameId ->
                                            nameFromId(gameId)?.let { gameName ->
                                                LogPlayActivity.logPlay(context, gameId, gameName, thumbnailFromId(gameId))
                                            }
                                        }
                                        selectedIds.clear()
                                    },
                                    onLogPlayWizard = {
                                        selectedIds.firstOrNull()?.let { gameId ->
                                            nameFromId(gameId)?.let { gameName ->
                                                NewPlayActivity.start(context, gameId, gameName)
                                            }
                                        }
                                        selectedIds.clear()
                                    },
                                    onQuickLogPlay = {
                                        showQuickLogPlayDialog = true
                                    },
                                    onShare = {
                                        shareGames(selectedIds.map { it to nameFromId(it).orEmpty() }, "Top Games")
                                        selectedIds.clear()
                                    },
                                    onView = {
                                        selectedIds.firstOrNull()?.let { gameId ->
                                            context.linkBgg(gameId)
                                        }
                                        selectedIds.clear()
                                    }
                                )
                            } else {
                                TopAppBar(
                                    title = { Text(stringResource(R.string.title_top_games)) },
                                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState()),
                                    navigationIcon = {
                                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                            Icon(
                                                painterResource(R.drawable.menu_24px),
                                                contentDescription = stringResource(R.string.navigation_drawer)
                                            )
                                        }
                                    }
                                )
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                    ) { contentPadding ->
                        TopGamesContent(
                            status = topGames?.status ?: Status.REFRESHING,
                            message = topGames?.message.orEmpty(),
                            data = topGames?.data,
                            padding = contentPadding,
                            selectedIds = selectedIds,
                            onClick = { game ->
                                if (inSelectionMode)
                                    selectedIds.toggle(game.id)
                                else
                                    GameActivity.start(this, game.id, game.name)
                            },
                            onLongClick = {
                                selectedIds.add(it)
                            }
                        )
                    }
                }
                if (showQuickLogPlayDialog) {
                    val message = pluralStringResource(R.plurals.msg_logging_plays, selectedIds.size)
                    AlertDialog(
                        onDismissRequest = { showQuickLogPlayDialog = false },
                        title = { Text(stringResource(R.string.are_you_sure_title)) },
                        text = { Text(stringResource(R.string.are_you_sure_log_quick_plays)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(message)
                                    }
                                    for (gameId in selectedIds) {
                                        nameFromId(gameId)?.let { gameName ->
                                            viewModel.logQuickPlay(gameId, gameName)
                                        }
                                    }
                                    selectedIds.clear()
                                    showQuickLogPlayDialog = false
                                }
                            ) {
                                Text(stringResource(R.string.yes))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showQuickLogPlayDialog = false }) {
                                Text(stringResource(R.string.no))
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiSelectionTopAppBar(
    selectedCount: Int,
    isAuthenticated: Boolean,
    onClear: () -> Unit = {},
    onLogPlay: () -> Unit = {},
    onLogPlayWizard: () -> Unit = {},
    onQuickLogPlay: () -> Unit = {},
    onShare: () -> Unit = {},
    onView: () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(pluralStringResource(R.plurals.msg_games_selected, selectedCount, selectedCount))
        },
        navigationIcon = {
            IconButton(onClick = { onClear() }) {
                Icon(painterResource(R.drawable.arrow_back_24px), contentDescription = stringResource(R.string.up))
            }
        },
        actions = {
            if (isAuthenticated) {
                if (selectedCount == 1) {
                    LogPlayAppBarExpandableActions(onLogPlay, onLogPlayWizard, onQuickLogPlay)
                } else {
                    LogPlayQuickAppBarAction(onQuickLogPlay)
                }
            }
            ShareAppBarAction(onShare)
            if (selectedCount == 1) {
                ViewAppBarAction(onView)
            }
        }
    )
}

@Composable
private fun TopGamesContent(
    status: Status,
    message: String,
    data: List<TopGame>?,
    padding: PaddingValues = PaddingValues(),
    selectedIds: MutableSet<Int> = remember { mutableStateSetOf() },
    onClick: (TopGame) -> Unit = {},
    onLongClick: (Int) -> Unit = {},
) {
    when (status) {
        Status.REFRESHING -> {
            BggLoadingIndicatorBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
        Status.ERROR -> {
            ErrorContent(
                message,
                painterResource(R.drawable.top_24px),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
        Status.SUCCESS -> {
            if (data.isNullOrEmpty()) {
                EmptyFullSizeScrollableContent(
                    R.string.empty_top_games,
                    painterResource(R.drawable.top_24px),
                    padding = padding,
                )
            } else {
                LazyColumn(
                    contentPadding = padding
                ) {
                    itemsIndexed(
                        items = data,
                        key = { _, searchResult -> searchResult.id }
                    ) { index, game ->
                        val isSelected by remember { derivedStateOf { selectedIds.contains(game.id) } }
                        TopGameListItem(
                            topGame = game,
                            modifier = Modifier.animateItem(),
                            isSelected = isSelected,
                            onClick = { onClick(game) },
                            onLongClick = { onLongClick(game.id) },
                        )
                        if (index < data.lastIndex)
                            HorizontalDivider()
                    }
                }
            }
        }

    }
}

@Composable
private fun TopGameListItem(
    topGame: TopGame,
    modifier: Modifier = Modifier,
    onClick: (topGame: TopGame) -> Unit = {},
    onLongClick: () -> Unit = {},
    isSelected: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.threeLineHeight)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onLongClick = onLongClick,
            ) {
                onClick(topGame)
            }
            .padding(ListItemDefaults.paddingValues)
    ) {
        ListItemIndex(topGame.rank, isWide = true)
        ListItemThumbnail(topGame.thumbnailUrl)
        Column {
            ListItemPrimaryText(topGame.name)
            ListItemSecondaryText(
                topGame.yearPublished.asYear(LocalContext.current),
                modifier = modifier.padding(bottom = ListItemDefaults.verticalTextPadding),
                icon = painterResource(R.drawable.year_24px),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TopGameListItemPreview(
    @PreviewParameter(TopGamePreviewParameterProvider::class) topGame: TopGame,
) {
    BggAppTheme {
        TopGameListItem(topGame, Modifier)
    }
}

private class TopGamePreviewParameterProvider : PreviewParameterProvider<TopGame> {
    override val values = sequenceOf(
        TopGame(
            rank = 1,
            id = 99,
            name = "Spirit Island",
            thumbnailUrl = "",
            yearPublished = 2019,
        ),
        TopGame(
            rank = 22,
            id = 99,
            name = "Star Wars: the Deck Building Game",
            thumbnailUrl = "",
            yearPublished = 2023,
        ),
        TopGame(
            rank = 50,
            id = 99,
            name = "Sky Team",
            thumbnailUrl = "",
            yearPublished = 2022,
        )
    )
}
