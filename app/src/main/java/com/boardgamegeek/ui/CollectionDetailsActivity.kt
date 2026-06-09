package com.boardgamegeek.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.LOG_PLAY_TYPE_FORM
import com.boardgamegeek.extensions.LOG_PLAY_TYPE_QUICK
import com.boardgamegeek.extensions.LOG_PLAY_TYPE_WIZARD
import com.boardgamegeek.extensions.notifyLoggedPlay
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.ui.collectionshelf.*
import com.boardgamegeek.ui.compose.Drawer
import com.boardgamegeek.ui.compose.DrawerItem
import com.boardgamegeek.ui.compose.MenuAppBarAction
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CollectionDetailsActivity : BaseActivity() {
    private val viewModel by viewModels<CollectionDetailsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "CollectionShelves")
            }
        }

        setContent {
            viewModel.refresh()

            val coroutineScope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val snackbarHostState = remember { SnackbarHostState() }
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            val errorMessage by viewModel.errorMessage.observeAsState()
            val loggedPlayResult by viewModel.loggedPlayResult.observeAsState()

            LaunchedEffect(errorMessage) {
                errorMessage?.getContentIfNotHandled()?.let { message ->
                    if (message.isNotBlank()) {
                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                    }
                }
            }
            LaunchedEffect(loggedPlayResult) {
                loggedPlayResult?.getContentIfNotHandled()?.let { result ->
                    notifyLoggedPlay(result)
                }
            }

            BggAppTheme {
                Drawer(
                    selectedItem = DrawerItem.CollectionShelves,
                    drawerState = drawerState,
                ) {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            CollectionShelvesTopBar(
                                scrollBehavior = scrollBehavior,
                                onMenuClick = { coroutineScope.launch { drawerState.open() } },
                            )
                        }
                    ) { contentPadding ->
                        val syncStatuses by viewModel.syncCollectionStatuses.observeAsState()
                        val logPlayPreference by viewModel.logPlayPreference.observeAsState(LOG_PLAY_TYPE_FORM)

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding)
                        ) {
                            val pagerState = rememberPagerState(
                                initialPage = ShelfTab.Browse.ordinal,
                                pageCount = { ShelfTab.entries.size },
                            )
                            ShelfTabRow(
                                pagerState.currentPage,
                            ) { newDestination ->
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(newDestination)
                                }
                            }
                            val screenModifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(top = 8.dp, bottom = 24.dp)
                            HorizontalPager(
                                state = pagerState,
                            ) { page ->
                                when (page) {
                                    ShelfTab.Browse.ordinal -> {
                                        val recentlyViewedItems by viewModel.recentlyViewedItems.observeAsState()
                                        val friendlessFavorites by viewModel.friendlessFavoriteItems.observeAsState()
                                        val friendless by viewModel.friendless.observeAsState()
                                        val underratedItems by viewModel.underratedItems.observeAsState()

                                        BrowseScreen(recentlyViewedItems, friendlessFavorites, friendless, underratedItems, screenModifier)
                                    }
                                    ShelfTab.Own.ordinal -> {
                                        val growthRate by viewModel.growthRate.observeAsState()
                                        val utilization by viewModel.utilization.observeAsState()
                                        val ownedItems by viewModel.ownedGames.observeAsState()
                                        val ownedExpansions by viewModel.ownedExpansions.observeAsState()
                                        val ownedAccessories by viewModel.ownedAccessories.observeAsState()
                                        val recentlyAcquired by viewModel.recentlyAcquired.observeAsState()
                                        val hawt by viewModel.hawtItems.observeAsState()

                                        OwnScreen(
                                            growthRate,
                                            utilization,
                                            ownedItems,
                                            ownedExpansions,
                                            ownedAccessories,
                                            recentlyAcquired,
                                            hawt,
                                            screenModifier,
                                        )
                                    }
                                    ShelfTab.Play.ordinal -> {
                                        val wantToPlay by viewModel.wantToPlayItems.observeAsState()
                                        val recentlyPlayedGames by viewModel.recentlyPlayedGames.observeAsState()
                                        val friendlessShouldPlayGames by viewModel.friendlessShouldPlayGames.observeAsState()
                                        val shelfOfOpportunityItems by viewModel.shelfOfOpportunityItems.observeAsState()
                                        val shelfOfNewOpportunityItems by viewModel.shelfOfNewOpportunityItems.observeAsState()

                                        val playerCountType by viewModel.playerCountType.observeAsState()
                                        val playerCount by viewModel.playerCount.observeAsState()

                                        PlayScreen(
                                            playerCountType,
                                            playerCount,
                                            syncStatuses,
                                            wantToPlay,
                                            recentlyPlayedGames,
                                            friendlessShouldPlayGames,
                                            shelfOfOpportunityItems,
                                            shelfOfNewOpportunityItems,
                                            screenModifier,
                                            onLogPlay = { item -> playGame(logPlayPreference, item) },
                                            onFilterPlayerCountType = { viewModel.filterPlayerCountType(it) },
                                            onFilterPlayerCount = { viewModel.filterPlayerCount(it) },
                                            onRemoveStatus = { internalId, status -> viewModel.removeStatus(internalId, status) },
                                        )
                                    }
                                    ShelfTab.Acquire.ordinal -> {
                                        val preordered by viewModel.preordered.observeAsState()
                                        val wishlist by viewModel.wishlist.observeAsState()
                                        val wantToBuy by viewModel.wantToBuy.observeAsState()
                                        val wantInTrade by viewModel.wantInTrade.observeAsState()
                                        val favoriteUnownedItems by viewModel.favoriteUnownedItems.observeAsState()
                                        val playedButUnownedItems by viewModel.playedButUnownedItems.observeAsState()
                                        val hawtUnownedItems by viewModel.hawtUnownedItems.observeAsState()

                                        val collectionAcquireStats by viewModel.collectionAcquireStats.observeAsState()
                                        val acquiredFromList by viewModel.acquiredFrom.observeAsState()

                                        AcquireScreen(
                                            collectionAcquireStats,
                                            syncStatuses,
                                            preordered,
                                            wishlist,
                                            wantToBuy,
                                            wantInTrade,
                                            favoriteUnownedItems,
                                            playedButUnownedItems,
                                            hawtUnownedItems,
                                            screenModifier,
                                            acquiredFromList.orEmpty(),
                                            onRemoveStatus = { internalId, status -> viewModel.removeStatus(internalId, status) },
                                            onAcquire = { internalId, info -> viewModel.markAsAcquired(internalId, info) },
                                        )
                                    }
                                    ShelfTab.Divest.ordinal -> {
                                        val owned by viewModel.ownedGames.observeAsState()
                                        val ownedCount by remember { derivedStateOf { owned?.second ?: 0 } }
                                        val regretFactor by viewModel.regretFactor.observeAsState()
                                        val forTrade by viewModel.forTrade.observeAsState()
                                        val forTradeCount by remember { derivedStateOf { forTrade?.second ?: 0 } }
                                        val forTradeRatio by remember { derivedStateOf { if (ownedCount == 0) 0.0 else (forTradeCount.toDouble() / ownedCount) } }
                                        val forTradeWithoutCondition by viewModel.forTradeWithoutCondition.observeAsState()
                                        val previouslyOwned by viewModel.previouslyOwned.observeAsState()
                                        val previouslyOwnedCount by remember { derivedStateOf { previouslyOwned?.second ?: 0 } }
                                        val previouslyOwnedRatio by remember { derivedStateOf { if (ownedCount == 0) 0.0 else (previouslyOwnedCount.toDouble() / ownedCount) } }
                                        val whyOwnItems by viewModel.whyOwnItems.observeAsState()

                                        DivestScreen(
                                            syncStatuses,
                                            ownedCount,
                                            previouslyOwnedRatio,
                                            forTradeRatio,
                                            regretFactor,
                                            forTrade,
                                            forTradeWithoutCondition,
                                            previouslyOwned,
                                            whyOwnItems,
                                            screenModifier,
                                            onRemoveStatus = { internalId, status -> viewModel.removeStatus(internalId, status) },
                                            onOfferForTrade = { internalId -> viewModel.addStatus(internalId, CollectionStatus.ForTrade) },
                                            onTrade = { viewModel.markAsTraded(it) },
                                            onAddTradeCondition = { internalId, text -> viewModel.updateCondition(internalId, text) }
                                        )
                                    }
                                    ShelfTab.Analyze.ordinal -> {
                                        val stats by viewModel.collectionAnalyzeStats.observeAsState()
                                        val ratableItems by viewModel.ratableItems.observeAsState()
                                        val commentableItems by viewModel.commentableItems.observeAsState()

                                        AnalyzeScreen(
                                            stats,
                                            ratableItems,
                                            commentableItems,
                                            screenModifier,
                                            { internalId, rating -> viewModel.updateRating(internalId, rating) },
                                            { internalId, comment -> viewModel.updateComment(internalId, comment) },
                                        )
                                    }
                                    ShelfTab.Credits.ordinal -> {
                                        CreditsFlowRow(screenModifier)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun playGame(logPlayPreference: String?, item: CollectionItem) {
        when (logPlayPreference) {
            null,
            LOG_PLAY_TYPE_FORM -> LogPlayActivity.logPlay(
                this,
                item.gameId,
                item.gameName,
                item.robustHeroImageUrl,
                item.arePlayersCustomSorted,
            )
            LOG_PLAY_TYPE_QUICK -> viewModel.logQuickPlay(item.gameId, item.gameName)
            LOG_PLAY_TYPE_WIZARD -> NewPlayActivity.start(this, item.gameId, item.gameName)
        }
    }
}

private enum class ShelfTab(@param:StringRes val resId: Int, @param:DrawableRes val iconResId: Int) {
    Browse(R.string.title_browse, R.drawable.collection_24px),
    Own(R.string.title_own, R.drawable.person_24px),
    Play(R.string.title_play, R.drawable.plays_24px),
    Acquire(R.string.title_acquire, R.drawable.shopping_cart_24px),
    Divest(R.string.title_divest, R.drawable.send_24px),
    Analyze(R.string.title_analyze, R.drawable.play_stats_24px),
    Credits(R.string.title_credits, R.drawable.publisher_24px)
}

@Composable
private fun ShelfTabRow(selectedDestination: Int, modifier: Modifier = Modifier, onClick: (Int) -> Unit) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedDestination,
        modifier = modifier,
    ) {
        ShelfTab.entries.forEachIndexed { index, tab ->
            Tab(
                selected = selectedDestination == index,
                onClick = { onClick(index) },
                text = {
                    Text(
                        text = stringResource(tab.resId),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                icon = { Icon(painterResource(tab.iconResId), null) },
            )
        }
    }
}

@Composable
private fun CollectionShelvesTopBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onMenuClick: () -> Unit = {},
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.title_collection_shelves)) },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = { MenuAppBarAction { onMenuClick() } },
    )
}

@Preview
@Composable
private fun CollectionShelvesTopBarPreview() {
    CollectionShelvesTopBar()
}

