@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)

package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.TaskStackBuilder
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.palette.graphics.Palette
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.useExistingImageAsPlaceholder
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Forum
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.game.GameDetailListItem
import com.boardgamegeek.ui.game.GameDetailPersonListItem
import com.boardgamegeek.ui.game.GameDetailThingListItem
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.dialog.GameUsersDialogFragment
import com.boardgamegeek.ui.game.*
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.ForumsViewModel
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class GameActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        if (gameId == BggContract.INVALID_ID) {
            Timber.w("Received an invalid game ID.")
            finish()
        }

        val gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()
        val imageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL).orEmpty()
        val thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL).orEmpty()
        val wasOpenedFromShortcut = intent.getBooleanExtra(KEY_FROM_SHORTCUT, false)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Game")
                param(FirebaseAnalytics.Param.ITEM_ID, gameId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, gameName)
            }
        }

        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()

            val viewModel: GameViewModel = viewModel()
            viewModel.setId(gameId)

            val snackbarHostState = remember { SnackbarHostState() }
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            val isRefreshing by viewModel.gameIsRefreshing.observeAsState(true)
            val itemsAreRefreshing by viewModel.itemsAreRefreshing.observeAsState(false)
            val playsAreRefreshing by viewModel.playsAreRefreshing.observeAsState(false)
            val partialPlaySyncTimeStamp by viewModel.partialPlaySyncTimeStamp.observeAsState(0L)

            val errorMessage by viewModel.errorMessage.observeAsState()
            val loggedPlayResult by viewModel.loggedPlayResult.observeAsState()

            val username by viewModel.username.observeAsState()
            val syncPlaysPreference by viewModel.syncPlaysPreference.observeAsState()
            val syncCollectionPreference by viewModel.syncCollectionPreference.observeAsState()
            val logPlayPreference by viewModel.logPlayPreference.observeAsState(LOG_PLAY_TYPE_FORM)

            val game by viewModel.game.observeAsState()
            val subtypes by viewModel.subtypes.observeAsState(emptyList())
            val families by viewModel.families.observeAsState(emptyList())
            val playerPoll by viewModel.playerPoll.observeAsState()
            val agePoll by viewModel.agePoll.observeAsState()
            val languagePoll by viewModel.languagePoll.observeAsState()
            val designers by viewModel.designers.observeAsState(emptyList())
            val artists by viewModel.artists.observeAsState(emptyList())
            val publishers by viewModel.publishers.observeAsState(emptyList())
            val mechanics by viewModel.mechanics.observeAsState(emptyList())
            val categories by viewModel.categories.observeAsState(emptyList())
            val baseGames by viewModel.baseGames.observeAsState(emptyList())
            val expansions by viewModel.expansions.observeAsState(emptyList())
            val plays by viewModel.plays.observeAsState(emptyList())
            val colors by viewModel.playColors.observeAsState(emptyList())
            val collectionItems by viewModel.collectionItems.observeAsState(emptyList())

            val forumsViewModel: ForumsViewModel = viewModel()
            forumsViewModel.setGameId(gameId)
            val forums = forumsViewModel.forums.observeAsState(RefreshableResource.refreshing(null))

            val limit = 4
            viewModel.refreshDesignerImages(limit)
            viewModel.refreshArtistImages(limit)
            viewModel.refreshPublisherImages(limit)

            var openAddCollectionItemDialog by rememberSaveable { mutableStateOf(false) }
            var producerType by rememberSaveable { mutableStateOf(GameViewModel.ProducerType.UNKNOWN) }

            BggAppTheme {
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

                Drawer {
                    val showCollectionTab = (syncCollectionPreference?.isNotEmpty() == true && username?.isNotBlank() == true)
                    val showPlaysTab = (syncPlaysPreference == true && username?.isNotBlank() == true)
                    val tabs = GameTab.entries.toMutableList().apply {
                        if (username.isNullOrBlank() || !showCollectionTab) remove(GameTab.Collection)
                        if (username.isNullOrBlank() || !showPlaysTab) remove(GameTab.Plays)
                    }.toList()

                    val pagerState = rememberPagerState(
                        initialPage = 0,
                        pageCount = { tabs.size },
                    )
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            GameTopBar(
                                game?.name.orEmpty().ifBlank { gameName },
                                game?.isFavorite ?: false,
                                game?.maxUsers ?: 0,
                                scrollBehavior = scrollBehavior,
                                onUpClick = {
                                    val upIntent = when {
                                        Authenticator.isSignedIn(this) -> intentFor<CollectionActivity>()
                                        else -> intentFor<HotnessActivity>()
                                    }
                                    if (wasOpenedFromShortcut) {
                                        TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities()
                                    } else {
                                        this.navigateUpTo(upIntent)
                                    }
                                },
                                onOpenInBrowserClick = { linkToBgg("boardgame", gameId) },
                                onLogPlay = {
                                    LogPlayActivity.logPlay(
                                        this,
                                        gameId,
                                        gameName,
                                        game?.heroImageUrl.orEmpty().ifBlank { thumbnailUrl.ifBlank { imageUrl } },
                                        game?.customPlayerSort ?: false
                                    )
                                },
                                onLogPlayWizard = {
                                    NewPlayActivity.start(this, gameId, gameName)
                                },
                                onQuickLogPlay = {
                                    viewModel.logQuickPlay(gameId, gameName)
                                },
                                onFavoriteToggled = { game?.let { viewModel.updateFavorite(!it.isFavorite) } },
                                onViewUsers = { GameUsersDialogFragment.launch(this) },
                                onShare = { shareGame(gameId, gameName, "Game", firebaseAnalytics) },
                                onPinShortcut = { viewModel.createShortcut() }
                            )
                        },
                        floatingActionButton = {
                            GameFab(
                                tabs,
                                pagerState,
                                gameIsFavorite = game?.isFavorite ?: false,
                                onLogPlay = {
                                    when (logPlayPreference) {
                                        LOG_PLAY_TYPE_FORM -> LogPlayActivity.logPlay(
                                            this,
                                            gameId,
                                            gameName,
                                            game?.heroImageUrl.orEmpty().ifBlank { thumbnailUrl.ifBlank { imageUrl } },
                                            game?.customPlayerSort ?: false
                                        )
                                        LOG_PLAY_TYPE_QUICK -> viewModel.logQuickPlay(gameId, gameName)
                                        LOG_PLAY_TYPE_WIZARD -> NewPlayActivity.start(this, gameId, gameName)
                                    }
                                },
                                onToggleFavorite = { viewModel.updateFavorite(!(game?.isFavorite ?: false)) },
                                onAddCollection = { openAddCollectionItemDialog = true },
                            )
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                    ) { contentPadding ->
                        Column(
                            modifier = Modifier.padding(contentPadding)
                        ) {
                            Box {
                                if (isRefreshing) {
                                    BggLoadingIndicatorBox(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp)
                                    )
                                }
                                val url = game?.imageUrl.orEmpty().ifBlank {
                                    game?.thumbnailUrl.orEmpty().ifBlank {
                                        imageUrl.ifBlank {
                                            thumbnailUrl
                                        }
                                    }
                                } // TODO iterate through URLs until an image is loaded
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(url)
                                        .useExistingImageAsPlaceholder(true)
                                        .crossfade(true)
                                        .allowHardware(false)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(id = R.drawable.person_image_empty),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal))
                                        .heightIn(max = 128.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable(onClick = { ImageActivity.start(context, url) }),
                                    onSuccess = { state ->
                                        coroutineScope.launch(Dispatchers.IO) {
                                            Palette.Builder(state.result.image.toBitmap()).generate { palette ->
                                                palette?.let { viewModel.updateGameColors(it) }
                                            }
                                        }
                                    }
                                )
                            }
                            game?.let {
                                GameTabRow(
                                    tabs,
                                    selectedDestination = pagerState.currentPage,
                                    onClick = { newDestination ->
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(newDestination)
                                        }
                                    },
                                )
                                HorizontalPager(pagerState) { targetState ->
                                    val screenModifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal))
                                        .padding(top = 8.dp, bottom = 24.dp)
                                    when (targetState) {
                                        tabs.indexOf(GameTab.Info) -> {
                                            PullToRefreshBox(
                                                isRefreshing = isRefreshing,
                                                onRefresh = { viewModel.refreshGame() },
                                            ) {
                                                GameInfoScreen(
                                                    it,
                                                    subtypes,
                                                    families,
                                                    playerPoll,
                                                    agePoll,
                                                    languagePoll,
                                                    modifier = screenModifier,
                                                )
                                            }
                                        }
                                        tabs.indexOf(GameTab.Credits) -> {
                                            PullToRefreshBox(
                                                isRefreshing = isRefreshing,
                                                onRefresh = { viewModel.refreshGame() },
                                            ) {
                                                GameCreditsScreen(
                                                    it,
                                                    designers,
                                                    artists,
                                                    publishers,
                                                    mechanics,
                                                    categories,
                                                    modifier = screenModifier,
                                                    onDesignerClick = { designer ->
                                                        PersonActivity.startForDesigner(this@GameActivity, designer.id, designer.name)
                                                    },
                                                    onDesignersClick = {
                                                        producerType = GameViewModel.ProducerType.DESIGNER
                                                    },
                                                    onArtistClick = { artist ->
                                                        PersonActivity.startForArtist(this@GameActivity, artist.id, artist.name)
                                                    },
                                                    onArtistsClick = {
                                                        producerType = GameViewModel.ProducerType.ARTIST
                                                    },
                                                    onPublisherClick = { publisher ->
                                                        PersonActivity.startForPublisher(this@GameActivity, publisher.id, publisher.name)
                                                    },
                                                    onPublishersClick = {
                                                        producerType = GameViewModel.ProducerType.PUBLISHER
                                                    },
                                                    onMechanicClick = { mechanic ->
                                                        MechanicActivity.start(this@GameActivity, mechanic.id, mechanic.name)
                                                    },
                                                    onMechanicsClick = {
                                                        producerType = GameViewModel.ProducerType.MECHANIC
                                                    },
                                                    onCategoryClick = { category ->
                                                        CategoryActivity.start(this@GameActivity, category.id, category.name)
                                                    },
                                                    onCategoriesClick = {
                                                        producerType = GameViewModel.ProducerType.CATEGORY
                                                    },
                                                )
                                            }
                                        }
                                        tabs.indexOf(GameTab.Description) -> {
                                            PullToRefreshBox(
                                                isRefreshing = isRefreshing,
                                                onRefresh = { viewModel.refreshGame() },
                                            ) {
                                                DescriptionScreen(
                                                    it,
                                                    screenModifier,
                                                )
                                            }
                                        }
                                        tabs.indexOf(GameTab.Collection) -> {
                                            PullToRefreshBox(
                                                isRefreshing = itemsAreRefreshing,
                                                onRefresh = { viewModel.refreshItems() },
                                            ) {
                                                GameCollectionScreen(
                                                    it,
                                                    collectionItems,
                                                    modifier = screenModifier,
                                                    onItemClicked = { item -> GameCollectionItemActivity.start(this@GameActivity, item) },
                                                )
                                            }
                                        }
                                        tabs.indexOf(GameTab.Plays) -> {
                                            PullToRefreshBox(
                                                isRefreshing = playsAreRefreshing,
                                                onRefresh = { viewModel.refreshPlays() },
                                            ) {
                                                GamePlaysScreen(
                                                    it,
                                                    partialPlaySyncTimeStamp.coerceAtLeast(it.updatedPlays),
                                                    plays,
                                                    colors,
                                                    modifier = screenModifier,
                                                    isRefreshing = playsAreRefreshing,
                                                    onPlaysClick = {
                                                        GamePlaysActivity.start(
                                                            this@GameActivity,
                                                            it.id,
                                                            it.name,
                                                            it.heroImageUrl,
                                                            it.thumbnailUrl,
                                                            it.customPlayerSort,
                                                            it.iconColor,
                                                        )
                                                    },
                                                    onPlayClick = { play ->
                                                        PlayActivity.start(this@GameActivity, play.internalId)
                                                    },
                                                    onStatsClick = {
                                                        GamePlayStatsActivity.start(this@GameActivity, it.id, it.name, it.iconColor)
                                                    },
                                                    onColorsClick = {
                                                        GameColorsActivity.start(this@GameActivity, it.id, it.name, it.iconColor)
                                                    },
                                                )
                                            }
                                        }
                                        tabs.indexOf(GameTab.LinkedItems) -> {
                                            PullToRefreshBox(
                                                isRefreshing = isRefreshing,
                                                onRefresh = { viewModel.refreshGame() },
                                            ) {
                                                GameLinkedItemsScreen(
                                                    it,
                                                    baseGames,
                                                    expansions,
                                                    modifier = screenModifier,
                                                    onItemClick = { item -> start(context, item.id, item.name, item.thumbnailUrl) },
                                                    onMoreClick = { _, type ->
                                                        producerType = type
                                                    }
                                                )
                                            }
                                        }
                                        tabs.indexOf(GameTab.Forums) -> {
                                            ForumsContent(
                                                forums.value.data,
                                                PaddingValues(0.dp),
                                            ) { forum, header ->
                                                ForumActivity.start(
                                                    this@GameActivity,
                                                    forum.id,
                                                    forum.title,
                                                    gameId,
                                                    gameName,
                                                    Forum.Type.GAME,
                                                    header
                                                )
                                            }
                                        }
                                        tabs.indexOf(GameTab.Links) -> {
                                            GameLinksScreen(
                                                it.id,
                                                it.name,
                                                iconColor = it.iconColor,
                                                modifier = screenModifier,
                                            )
                                        }
                                    }
                                }
                                if (openAddCollectionItemDialog) {
                                    AddCollectionItemDialog(
                                        onConfirmation = { selectedStatuses, wishlistPriority ->
                                            openAddCollectionItemDialog = false
                                            viewModel.addCollectionItem(selectedStatuses, wishlistPriority)
                                        },
                                        onDismissRequest = { openAddCollectionItemDialog = false },
                                    )
                                }
                                if (producerType != GameViewModel.ProducerType.UNKNOWN) {
                                    GameDetailListBottomSheet(
                                        producerType,
                                        designers,
                                        artists,
                                        publishers,
                                        mechanics,
                                        categories,
                                        expansions,
                                        baseGames,
                                    ) {
                                        producerType = GameViewModel.ProducerType.UNKNOWN
                                    }
                                }
                            } ?: EmptyFullSizeScrollableContent(
                                stringResource(R.string.empty_game),
                                painterResource(R.drawable.game_24px),
                                modifier = Modifier.padding(contentPadding),
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_THUMBNAIL_URL = "THUMBNAIL_URL"
        private const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        private const val KEY_FROM_SHORTCUT = "FROM_SHORTCUT"

        fun start(context: Context, gameId: Int, gameName: String, thumbnailUrl: String = "", heroImageUrl: String = "") {
            val intent = createIntent(context, gameId, gameName, thumbnailUrl, heroImageUrl) ?: return
            context.startActivity(intent)
        }

        fun startUp(context: Context, gameId: Int, gameName: String, thumbnailUrl: String = "", heroImageUrl: String = thumbnailUrl) {
            val intent = createIntent(context, gameId, gameName, thumbnailUrl, heroImageUrl) ?: return
            context.startActivity(intent.clearTask().clearTop())
        }

        fun createIntent(context: Context, gameId: Int, gameName: String, thumbnailUrl: String = "", heroImageUrl: String = ""): Intent? {
            if (gameId == BggContract.INVALID_ID) return null
            return context.intentFor<GameActivity>(
                KEY_GAME_ID to gameId,
                KEY_GAME_NAME to gameName,
                KEY_THUMBNAIL_URL to thumbnailUrl,
                KEY_HERO_IMAGE_URL to heroImageUrl,
            )
        }

        fun createShortcutInfo(context: Context, gameId: Int, gameName: String, bitmap: Bitmap? = null): ShortcutInfoCompat? {
            if (gameId != BggContract.INVALID_ID &&
                gameName.isNotBlank() &&
                ShortcutManagerCompat.isRequestPinShortcutSupported(context)
            ) {
                val intent = createIntent(context, gameId, gameName)
                intent?.let {
                    intent.action = Intent.ACTION_VIEW
                    intent.putExtra(KEY_FROM_SHORTCUT, true).clearTop().newTask()
                    val builder = ShortcutInfoCompat.Builder(context, "game-$gameId")
                        .setShortLabel(gameName.toShortLabel())
                        .setLongLabel(gameName.toLongLabel())
                        .setIntent(intent)
                    if (bitmap != null) {
                        builder.setIcon(IconCompat.createWithAdaptiveBitmap(bitmap))
                    } else {
                        builder.setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_foreground))
                    }
                    return builder.build()
                }
            }
            return null
        }
    }
}

@Composable
private fun GameDetailListBottomSheet(
    producerType: GameViewModel.ProducerType,
    designers: List<GameDetail>,
    artists: List<GameDetail>,
    publishers: List<GameDetail>,
    mechanics: List<GameDetail>,
    categories: List<GameDetail>,
    expansions: List<GameDetail>,
    baseGames: List<GameDetail>,
    onDismissRequest: () -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        val title = when (producerType) {
            GameViewModel.ProducerType.DESIGNER -> stringResource(R.string.title_designers)
            GameViewModel.ProducerType.ARTIST -> stringResource(R.string.title_artists)
            GameViewModel.ProducerType.PUBLISHER -> stringResource(R.string.title_publishers)
            GameViewModel.ProducerType.MECHANIC -> stringResource(R.string.title_mechanics)
            GameViewModel.ProducerType.CATEGORY -> stringResource(R.string.title_categories)
            GameViewModel.ProducerType.EXPANSION -> stringResource(R.string.expansions)
            GameViewModel.ProducerType.BASE_GAME -> stringResource(R.string.base_games)
            else -> ""
        }
        val items = when (producerType) {
            GameViewModel.ProducerType.DESIGNER -> designers
            GameViewModel.ProducerType.ARTIST -> artists
            GameViewModel.ProducerType.PUBLISHER -> publishers
            GameViewModel.ProducerType.MECHANIC -> mechanics
            GameViewModel.ProducerType.CATEGORY -> categories
            GameViewModel.ProducerType.EXPANSION -> expansions
            GameViewModel.ProducerType.BASE_GAME -> baseGames
            else -> emptyList()
        }
        val context = LocalContext.current
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal))
        )
        LazyColumn {
            items(
                items = items,
                key = { item -> item.id }
            ) { gameDetail ->
                when (producerType) {
                    GameViewModel.ProducerType.DESIGNER ->
                        GameDetailPersonListItem(gameDetail) {
                            PersonActivity.startForDesigner(context, gameDetail.id, gameDetail.name)
                        }
                    GameViewModel.ProducerType.ARTIST ->
                        GameDetailPersonListItem(gameDetail) {
                            PersonActivity.startForArtist(context, gameDetail.id, gameDetail.name)
                        }
                    GameViewModel.ProducerType.PUBLISHER ->
                        GameDetailThingListItem(gameDetail) {
                            PersonActivity.startForPublisher(context, gameDetail.id, gameDetail.name)
                        }
                    GameViewModel.ProducerType.MECHANIC ->
                        GameDetailListItem(gameDetail) {
                            MechanicActivity.start(context, gameDetail.id, gameDetail.name)
                        }
                    GameViewModel.ProducerType.CATEGORY ->
                        GameDetailListItem(gameDetail) {
                            CategoryActivity.start(context, gameDetail.id, gameDetail.name)
                        }
                    GameViewModel.ProducerType.EXPANSION,
                    GameViewModel.ProducerType.BASE_GAME ->
                        GameDetailThingListItem(gameDetail) {
                            GameActivity.start(context, gameDetail.id, gameDetail.name)
                        }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun GameTopBar(
    gameName: String,
    isFavorite: Boolean,
    maxUsers: Int,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onUpClick: () -> Unit = {},
    onOpenInBrowserClick: () -> Unit = {},
    onLogPlay: () -> Unit = {},
    onLogPlayWizard: () -> Unit = {},
    onQuickLogPlay: () -> Unit = {},
    onViewUsers: () -> Unit = {},
    onFavoriteToggled: () -> Unit = {},
    onShare: () -> Unit = {},
    onPinShortcut: () -> Unit = {},
) {
    MediumTopAppBar(
        title = { Text(text = gameName.ifBlank { stringResource(R.string.title_game) }) },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = { UpAppBarAction(onUpClick) },
        actions = {
            ViewAppBarAction(onOpenInBrowserClick)
            var isOverflowMenuExpanded by remember { mutableStateOf(false) }
            OverflowAppBarAction { isOverflowMenuExpanded = !isOverflowMenuExpanded }
            DropdownMenu(
                expanded = isOverflowMenuExpanded,
                onDismissRequest = { isOverflowMenuExpanded = false }
            ) {
                var isPlayLoggingExpanded by remember { mutableStateOf(false) }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_log_play)) },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.log_play_24px),
                            contentDescription = stringResource(R.string.menu_log_play)
                        )
                    },
                    onClick = { isPlayLoggingExpanded = true },
                )
                // TODO support sub menus better
                DropdownMenu(
                    expanded = isPlayLoggingExpanded,
                    onDismissRequest = { isPlayLoggingExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_log_play_short)) },
                        onClick = {
                            onLogPlay()
                            isPlayLoggingExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_log_play_wizard_short)) },
                        onClick = {
                            onLogPlayWizard()
                            isPlayLoggingExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_log_play_quick_short)) },
                        onClick = {
                            onQuickLogPlay()
                            isPlayLoggingExpanded = false
                        }
                    )
                }
                val favoriteText = if (isFavorite) R.string.menu_unfavorite else R.string.menu_favorite
                DropdownMenuItem(
                    text = { Text(stringResource(favoriteText)) },
                    leadingIcon = {
                        Icon(
                            painterResource(if (isFavorite) R.drawable.favorite_filled_24px else R.drawable.favorite_hollow_24px),
                            contentDescription = stringResource(favoriteText)
                        )
                    },
                    onClick = {
                        onFavoriteToggled()
                        isOverflowMenuExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_users)) },
                    enabled = maxUsers > 0,
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.users_24px),
                            contentDescription = stringResource(R.string.menu_users)
                        )
                    },
                    onClick = {
                        onViewUsers()
                        isOverflowMenuExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_share)) },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.share_24px),
                            contentDescription = stringResource(R.string.menu_share)
                        )
                    },
                    onClick = {
                        onShare()
                        isOverflowMenuExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_create_shortcut)) },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.pin_shortcut_24px),
                            contentDescription = stringResource(R.string.menu_create_shortcut)
                        )
                    },
                    onClick = {
                        onPinShortcut()
                        isOverflowMenuExpanded = false
                    },
                )
            }
        }
    )
}

private enum class GameTab(@param:StringRes val resId: Int) {
    Info(R.string.title_info),
    Credits(R.string.title_credits),
    Description(R.string.title_description),
    Collection(R.string.title_my_games),
    Plays(R.string.title_plays),
    LinkedItems(R.string.title_linked_items),
    Forums(R.string.title_forums),
    Links(R.string.title_links),
}

@Composable
private fun GameTabRow(
    tabs: List<GameTab>,
    selectedDestination: Int,
    modifier: Modifier = Modifier,
    onClick: (Int) -> Unit = {}
) {
    SecondaryScrollableTabRow(
        selectedTabIndex = selectedDestination,
        modifier = modifier,
        edgePadding = 0.dp,
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = selectedDestination == index,
                onClick = { onClick(index) },
                text = {
                    Text(
                        text = stringResource(tab.resId),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            )
        }
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun GameTabRowPreview() {
    BggAppTheme {
        Column {
            GameTabRow(
                GameTab.entries.toList(),
                selectedDestination = 0
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview
@Composable
private fun GameTopBarPreview() {
    BggAppTheme {
        GameTopBar(
            "Ticket to Ride",
            isFavorite = true,
            10_000,
        )
    }
}

@Composable
private fun GameFab(
    tabs: List<GameTab>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    gameIsFavorite: Boolean = false,
    onLogPlay: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onAddCollection: () -> Unit = {},
) {
    val exitDuration = 100
    val enterDuration = 200
    AnimatedContent(
        targetState = pagerState.currentPage,
        transitionSpec = {
            scaleIn(animationSpec = tween(durationMillis = enterDuration, delayMillis = exitDuration))
                .togetherWith(
                    scaleOut(animationSpec = tween(durationMillis = exitDuration))
                )
        }
    ) { targetState ->
        when (targetState) {
            tabs.indexOf(GameTab.Info) ->
                Fab(
                    stringResource(R.string.menu_log_play),
                    painterResource(R.drawable.log_play_24px),
                    modifier,
                    onLogPlay,
                )
            tabs.indexOf(GameTab.Credits),
            tabs.indexOf(GameTab.Description) ->
                Fab(
                    stringResource(if (gameIsFavorite) R.string.menu_unfavorite else R.string.menu_favorite),
                    painterResource(if (gameIsFavorite) R.drawable.favorite_filled_24px else R.drawable.favorite_hollow_24px),
                    modifier,
                    onToggleFavorite,
                )
            tabs.indexOf(GameTab.Collection) ->
                Fab(
                    stringResource(R.string.add_to_collection),
                    painterResource(R.drawable.add_24px),
                    modifier,
                    onClick = onAddCollection,
                )
            tabs.indexOf(GameTab.Plays) ->
                Fab(
                    stringResource(R.string.menu_log_play),
                    painterResource(R.drawable.add_24px),
                    modifier,
                    onClick = onLogPlay,
                )
        }
    }
}

@Composable
private fun Fab(
    text: String,
    iconPainter: Painter,
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
        ) {
            Icon(
                iconPainter,
                contentDescription = text,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
private fun GameFabPreview() {
    GameFab(
        GameTab.entries.toList(),
        rememberPagerState(
            initialPage = 1,
            pageCount = { 1 },
        )
    )
}
