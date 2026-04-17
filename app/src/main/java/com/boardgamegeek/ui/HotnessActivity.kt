package com.boardgamegeek.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
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
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.HotGame
import com.boardgamegeek.model.Status
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.HotnessViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HotnessActivity : BaseActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Hotness")
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

            val viewModel by viewModels<HotnessViewModel>()
            val hotGames by viewModel.hotGames.observeAsState()
            val loggedPlayResult by viewModel.loggedPlayResult.observeAsState()
            val errorMessage by viewModel.errorMessage.observeAsState()

            fun nameFromId(id: Int): String? =
                hotGames?.data?.find { result ->
                    result.id == id
                }?.name

            fun thumbnailFromId(id: Int): String =
                hotGames?.data?.find { result ->
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
                    selectedItem = DrawerItem.Hotness,
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
                                        shareGames(selectedIds.map { it to nameFromId(it).orEmpty() }, "Hotness")
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
                                    title = { Text(stringResource(R.string.title_hotness)) },
                                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState()),
                                    navigationIcon = { MenuAppBarAction { coroutineScope.launch { drawerState.open() } } },
                                )
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                    ) { contentPadding ->
                        HotnessContent(
                            status = hotGames?.status ?: Status.REFRESHING,
                            message = hotGames?.message.orEmpty(),
                            data = hotGames?.data,
                            padding = contentPadding,
                            selectedIds = selectedIds,
                            onClick = { game ->
                                if (inSelectionMode)
                                    selectedIds.toggle(game.id)
                                else
                                    GameActivity.start(this, game.id, game.name, game.thumbnailUrl)
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
private fun HotnessContent(
    status: Status,
    message: String,
    data: List<HotGame>?,
    padding: PaddingValues = PaddingValues(),
    selectedIds: MutableSet<Int> = remember { mutableStateSetOf() },
    onClick: (HotGame) -> Unit = {},
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
                painterResource(R.drawable.hotness_24px),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
        Status.SUCCESS -> {
            if (data.isNullOrEmpty()) {
                EmptyFullSizeScrollableContent(
                    R.string.empty_hotness,
                    painterResource(R.drawable.hotness_24px),
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
                        HotGameListItem(
                            hotGame = game,
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
private fun HotGameListItem(
    hotGame: HotGame,
    modifier: Modifier = Modifier,
    onClick: (hotGame: HotGame) -> Unit = {},
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
                onClick(hotGame)
            }
            .padding(ListItemDefaults.paddingValues)
    ) {
        ListItemIndex(hotGame.rank)
        ListItemThumbnail(hotGame.thumbnailUrl)
        Column {
            ListItemPrimaryText(hotGame.name)
            ListItemSecondaryText(
                hotGame.yearPublished.asYear(LocalContext.current),
                modifier = modifier.padding(bottom = ListItemDefaults.verticalTextPadding),
                icon = painterResource(R.drawable.year_24px),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HotGameListItemPreview(
    @PreviewParameter(HotGamePreviewParameterProvider::class) hotGame: HotGame,
) {
    BggAppTheme {
        HotGameListItem(hotGame, Modifier)
    }
}

private class HotGamePreviewParameterProvider : PreviewParameterProvider<HotGame> {
    override val values = sequenceOf(
        HotGame(
            rank = 1,
            id = 99,
            name = "Spirit Island",
            yearPublished = 2019,
        ),
        HotGame(
            rank = 22,
            id = 99,
            name = "Star Wars: the Deck Building Game",
            yearPublished = 2023,
        ),
        HotGame(
            rank = 50,
            id = 99,
            name = "Sky Team",
            yearPublished = 2022,
        )
    )
}
