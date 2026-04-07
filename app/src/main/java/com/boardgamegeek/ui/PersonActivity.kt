@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalCoilApi::class)

package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.useExistingImageAsPlaceholder
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.*
import com.boardgamegeek.model.Person.Type
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.person.*
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.ForumsViewModel
import com.boardgamegeek.ui.viewmodel.PersonViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PersonActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val id = intent.getIntExtra(KEY_PERSON_ID, BggContract.INVALID_ID)
        val name = intent.getStringExtra(KEY_PERSON_NAME).orEmpty()
        val personType = intent.getSerializableCompat(KEY_PERSON_TYPE) ?: Type.Designer

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Person")
                param(FirebaseAnalytics.Param.ITEM_ID, id.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, name)
            }
        }

        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val viewModel by viewModels<PersonViewModel>()

            when (personType) {
                Type.Artist -> viewModel.setArtistId(id)
                Type.Designer -> viewModel.setDesignerId(id)
                Type.Publisher -> viewModel.setPublisherId(id)
            }
            val details by viewModel.details.observeAsState()
            val stats by viewModel.stats.observeAsState()
            val collection by viewModel.collection.observeAsState()
            val sortBy by viewModel.collectionSort.observeAsState(CollectionItem.SortType.RATING)
            val syncCollectionPreference by viewModel.syncCollectionPreference.observeAsState()

            val forumsViewModel: ForumsViewModel = viewModel()
            forumsViewModel.setPersonId(id)
            val forums = forumsViewModel.forums.observeAsState(RefreshableResource.refreshing(null))

            LaunchedEffect(Unit) {
                viewModel.refreshIfStale()
            }
            BggAppTheme {
                Drawer {
                    val tabs = PersonTab.entries.toList()
                    val pagerState = rememberPagerState(
                        initialPage = 0,
                        pageCount = { tabs.size },
                    )
                    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            PersonTopBar(
                                name = name,
                                type = personType,
                                sortBy = sortBy,
                                scrollBehavior = scrollBehavior,
                                onUpClick = {
                                    when (personType) {
                                        Type.Designer -> startActivity(intentFor<DesignersActivity>().clearTop())
                                        Type.Artist -> startActivity(intentFor<ArtistsActivity>().clearTop())
                                        Type.Publisher -> startActivity(intentFor<PublishersActivity>().clearTop())
                                    }
                                    finish()
                                },
                                onOpenInBrowserClick = {
                                    @Suppress("SpellCheckingInspection")
                                    val path = when (personType) {
                                        Type.Designer -> "boardgamedesigner"
                                        Type.Artist -> "boardgameartist"
                                        Type.Publisher -> "boardgamepublisher"
                                    }
                                    linkToBgg(path, id)
                                },
                                onSortClick = {
                                    viewModel.sort(it)
                                },
                            )
                        },
                    ) { contentPadding ->
                        PullToRefreshBox(
                            isRefreshing = (details?.status == Status.REFRESHING),
                            onRefresh = { viewModel.refresh() },
                            modifier = Modifier.padding(contentPadding),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box {
                                    val isRefreshing = (details?.status == Status.REFRESHING)
                                    if (isRefreshing) {
                                        BggLoadingIndicatorBox(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp)
                                        )
                                    }
                                    val url = details?.data?.heroImageUrl.orEmpty().ifBlank { details?.data?.thumbnailUrl.orEmpty() }
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
                                    )
                                }
                                when (details?.status) {
                                    Status.ERROR -> {
                                        ErrorContent(
                                            text = forums.value.message.ifEmpty { stringResource(R.string.msg_error) },
                                            personType.mapToPainter(),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(contentPadding)
                                                .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal))
                                        )
                                    }
                                    else -> {
                                        details?.data?.let {
                                            PersonTabRow(
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
                                                    tabs.indexOf(PersonTab.Info) -> {
                                                        PersonInfoScreen(it, personType, screenModifier)
                                                    }
                                                    tabs.indexOf(PersonTab.Stats) -> {
                                                        PersonStatsScreen(
                                                            stats,
                                                            personType,
                                                            screenModifier,
                                                            onModifyCollectionStatus = {
                                                                viewModel.enableCollectionStatuses()
                                                            },
                                                            showCollectionStatusButton = (syncCollectionPreference?.contains(CollectionStatus.Rated) != true) &&
                                                                    ((syncCollectionPreference?.contains(CollectionStatus.Played) != true))
                                                        )
                                                    }
                                                    tabs.indexOf(PersonTab.Collection) -> {
                                                        PersonCollectionScreen(collection, personType, screenModifier)
                                                    }
                                                    tabs.indexOf(PersonTab.Forums) -> {
                                                        ForumsContent(
                                                            forums.value.data,
                                                            PaddingValues(0.dp),
                                                        ) { forum, header ->
                                                            ForumActivity.start(
                                                                this@PersonActivity,
                                                                forum.id,
                                                                forum.title,
                                                                id,
                                                                name,
                                                                Forum.Type.GAME,
                                                                header
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_PERSON_TYPE = "PERSON_TYPE"
        private const val KEY_PERSON_ID = "PERSON_ID"
        private const val KEY_PERSON_NAME = "PERSON_NAME"

        fun startForArtist(context: Context, id: Int, name: String) {
            context.startActivity(createIntent(context, id, name, Type.Artist))
        }

        fun startForDesigner(context: Context, id: Int, name: String) {
            context.startActivity(createIntent(context, id, name, Type.Designer))
        }

        fun startForPublisher(context: Context, id: Int, name: String) {
            context.startActivity(createIntent(context, id, name, Type.Publisher))
        }

        fun startUpForArtist(context: Context, id: Int, name: String) {
            context.startActivity(createIntent(context, id, name, Type.Artist).clearTask().clearTop())
        }

        fun startUpForDesigner(context: Context, id: Int, name: String) {
            context.startActivity(createIntent(context, id, name, Type.Designer).clearTask().clearTop())
        }

        fun startUpForPublisher(context: Context, id: Int, name: String) {
            context.startActivity(createIntent(context, id, name, Type.Publisher).clearTask().clearTop())
        }

        private fun createIntent(context: Context, id: Int, name: String, personType: Type): Intent {
            return context.intentFor<PersonActivity>(
                KEY_PERSON_ID to id,
                KEY_PERSON_NAME to name,
                KEY_PERSON_TYPE to personType,
            )
        }
    }
}

private enum class PersonCollectionSort(
    val type: CollectionItem.SortType,
    @param:StringRes val labelResId: Int,
) {
    Name(CollectionItem.SortType.NAME, R.string.menu_sort_name),
    Rating(CollectionItem.SortType.RATING, R.string.menu_sort_rating),
}

@Composable
private fun PersonTopBar(
    name: String,
    type: Type,
    sortBy: CollectionItem.SortType,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onUpClick: () -> Unit = {},
    onOpenInBrowserClick: () -> Unit = {},
    onSortClick: (CollectionItem.SortType) -> Unit = {},
) {
    var showSortMenu by remember { mutableStateOf(false) }
    MediumFlexibleTopAppBar(
        title = { Text(text = name.ifBlank { type.mapToDescription() }) },
        subtitle = { if (name.isNotBlank()) SubtitleWithIcon(type.mapToDescription(), type.mapToPainter()) },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            UpAppBarAction(onUpClick)
        },
        actions = {
            ViewAppBarAction(onOpenInBrowserClick)
            IconButton(onClick = { showSortMenu = true }) {
                Icon(
                    painterResource(R.drawable.sort_24px),
                    contentDescription = stringResource(R.string.menu_sort),
                )
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                PersonCollectionSort.entries.forEach {
                    DropdownMenuItem(
                        text = { Text(stringResource(it.labelResId)) },
                        leadingIcon = {
                            RadioButton(
                                selected = (it.type == sortBy),
                                onClick = null
                            )
                        },
                        onClick = {
                            showSortMenu = false
                            onSortClick(it.type)
                        }
                    )
                }
            }
        }
    )
}

private enum class PersonTab(@param:StringRes val resId: Int) {
    Info(R.string.title_info),
    Stats(R.string.title_stats),
    Collection(R.string.title_my_games),
    Forums(R.string.title_forums),
}

@Composable
private fun PersonTabRow(
    tabs: List<PersonTab>,
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

@Preview
@Composable
private fun PersonTopBarPreview() {
    BggAppTheme {
        PersonTopBar("Uwe Rosenberg", Type.Designer, CollectionItem.SortType.RATING)
    }
}