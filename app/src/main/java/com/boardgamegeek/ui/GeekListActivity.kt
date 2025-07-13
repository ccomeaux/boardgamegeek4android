package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.GeekListViewModel
import com.boardgamegeek.util.XmlApiMarkupConverter
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@AndroidEntryPoint
class GeekListActivity : BaseActivity() {
    private var geekListId = BggContract.INVALID_ID
    private var geekListTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        geekListId = intent.getIntExtra(KEY_ID, BggContract.INVALID_ID)
        geekListTitle = intent.getStringExtra(KEY_TITLE).orEmpty()

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekList")
                param(FirebaseAnalytics.Param.ITEM_ID, geekListId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, geekListTitle)
            }
        }

        setContent {
            val markupConverter = XmlApiMarkupConverter(this)

            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            val viewModel: GeekListViewModel = viewModel()
            val geekList = viewModel.geekList.observeAsState(RefreshableResource.refreshing(null))
            val imageProgress = viewModel.imageProgress.observeAsState(0.0f)
            val animatedImageProgress by animateFloatAsState(imageProgress.value, animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec)
            viewModel.setId(geekListId)

            BggAppTheme {
                Drawer {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            GeekListTopAppBar(
                                geekList.value.data?.title.orEmpty().ifEmpty { geekListTitle },
                                onBack = { onBackPressedDispatcher.onBackPressed() },
                                onOpenInBrowser = { linkToBgg("geeklist", geekListId) },
                                onShare = { shareGeekList() },
                                scrollBehavior = scrollBehavior,
                            )
                        }
                    ) { contentPadding ->
                        when (geekList.value.status) {
                            Status.ERROR -> {
                                ErrorContent(
                                    text = geekList.value.message.ifEmpty { stringResource(R.string.error_loading_geeklist) },
                                    imageVector = Icons.AutoMirrored.Filled.ListAlt,
                                    modifier = Modifier.padding(contentPadding)
                                )
                            }
                            Status.REFRESHING -> {
                                RefreshContent(contentPadding)
                            }
                            Status.SUCCESS -> {
                                geekList.value.data?.let {
                                    SuccessfulGeekListContent(contentPadding, it, { animatedImageProgress }, markupConverter)
                                } ?: EmptyGeekListContent(contentPadding)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyGeekListContent(contentPadding: PaddingValues) {
        EmptyContent(
            R.string.empty_geeklist,
            Icons.AutoMirrored.Filled.ListAlt,
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
        )
    }

    @Composable
    private fun SuccessfulGeekListContent(
        contentPadding: PaddingValues,
        geekList: GeekList,
        imageProgress: () -> Float,
        markupConverter: XmlApiMarkupConverter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            val descriptionScrollState: ScrollState = rememberScrollState()
            val itemListState: LazyListState = rememberLazyListState()
            val emptyItemListScrollState: ScrollState = rememberScrollState()
            val commentListState: LazyListState = rememberLazyListState()
            val emptyCommentListScrollState: ScrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(
                        horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                        vertical = dimensionResource(R.dimen.material_margin_vertical),
                    )
            ) {
                GeekListHeader(geekList, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            val pagerState = rememberPagerState(
                initialPage = GeekListTab.Description.ordinal,
                pageCount = { GeekListTab.entries.size },
            )
            val coroutineScope = rememberCoroutineScope()
            GeekListTabRow(
                selectedDestination = pagerState.currentPage,
                onClick = { newDestination ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(newDestination)
                    }
                },
            )
            HorizontalPager(
                state = pagerState,
            ) { page ->
                when (page) {
                    GeekListTab.Description.ordinal -> GeekListDescriptionContent(
                        description = geekList.description,
                        scrollState = descriptionScrollState,
                        markupConverter = markupConverter,
                    )
                    GeekListTab.Items.ordinal -> GeekListItemListContent(
                        geekList,
                        imageProgress = imageProgress(),
                        lazyListState = itemListState,
                        scrollState = emptyItemListScrollState,
                    )
                    GeekListTab.Comments.ordinal -> GeekListCommentContent(
                        geekList.comments,
                        lazyListState = commentListState,
                        scrollState = emptyCommentListScrollState,
                    )
                }
            }
        }
    }

    private fun shareGeekList() {
        val description = String.format(getString(R.string.share_geeklist_text), geekListTitle)
        val uri = createBggUri("geeklist", geekListId)
        share(getString(R.string.share_geeklist_subject), "$description\n\n$uri")

        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekList")
            param(FirebaseAnalytics.Param.ITEM_ID, geekListId.toString())
            param(FirebaseAnalytics.Param.ITEM_ID, geekListTitle)
        }
    }

    companion object {
        private const val KEY_ID = "GEEK_LIST_ID"
        private const val KEY_TITLE = "GEEK_LIST_TITLE"

        fun start(context: Context, id: Int, title: String) {
            context.startActivity(createIntent(context, id, title))
        }

        fun startUp(context: Context, id: Int, title: String) {
            context.startActivity(createIntent(context, id, title).clearTop())
        }

        private fun createIntent(context: Context, id: Int, title: String): Intent {
            return context.intentFor<GeekListActivity>(
                KEY_ID to id,
                KEY_TITLE to title,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
private fun GeekListTopAppBar(
    geekListTitle: String,
    onBack: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    MediumFlexibleTopAppBar(
        title = { Text(geekListTitle.ifEmpty { stringResource(R.string.title_geeklist) }) },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = { onBack() }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(R.string.up))
            }
        },
        actions = {
            IconButton(onClick = { onOpenInBrowser() }) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = stringResource(R.string.menu_view_in_browser))
            }
            IconButton(onClick = { onShare() }) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.menu_share))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@PreviewLightDark
@Composable
private fun GeekListTopAppBarPreview() {
    BggAppTheme {
        GeekListTopAppBar("My GeekList", {}, {}, {})
    }
}

@Composable
private fun RefreshContent(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        LoadingIndicator(
            Modifier
                .align(Alignment.Center)
                .padding(dimensionResource(R.dimen.padding_extra))
        )
    }
}

private enum class GeekListTab(@StringRes val resId: Int) {
    Description(R.string.title_description),
    Items(R.string.title_items),
    Comments(R.string.title_comments),
}

@Composable
private fun GeekListTabRow(selectedDestination: Int, modifier: Modifier = Modifier, onClick: (Int) -> Unit) {
    SecondaryTabRow(
        selectedTabIndex = selectedDestination,
        modifier = modifier,
    ) {
        GeekListTab.entries.forEachIndexed { index, tab ->
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
private fun GeekListTabRowPreview() {
    val geekList = GeekList(
        id = 123,
        title = "My GeekList",
        username = "ccomeaux",
        description = "This is a description",
    )
    BggAppTheme {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                GeekListHeader(geekList, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            GeekListTabRow(
                1,
                Modifier,
                onClick = { index ->
                    Timber.i("onClick = $index")
                },
            )
        }
    }
}

@Composable
private fun GeekListDescriptionContent(
    description: String,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    markupConverter: XmlApiMarkupConverter? = null,
) {
    if (description.isEmpty()) {
        EmptyContent(
            R.string.empty_geeklist_description,
            Icons.Filled.Description,
            modifier = modifier
                .fillMaxSize()
                .padding(
                    horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                    vertical = dimensionResource(R.dimen.material_margin_vertical)
                ),
            scrollState = scrollState,
        )
    } else {
        Text(
            text = AnnotatedString.fromHtml(markupConverter?.toHtml(description) ?: description),
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                    vertical = dimensionResource(R.dimen.material_margin_vertical)
                )
        )
    }
}

@Composable
private fun GeekListItemListContent(
    geekList: GeekList?, // TODO don't accept nulls
    imageProgress: Float,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    scrollState: ScrollState = rememberScrollState(),
) {
    val geekListItems = geekList?.items.orEmpty()
    if (geekListItems.isEmpty()) {
        EmptyContent(
            R.string.empty_geeklist,
            Icons.AutoMirrored.Filled.List,
            modifier = modifier
                .fillMaxSize()
                .padding(
                    horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                    vertical = dimensionResource(R.dimen.material_margin_vertical)
                ),
            scrollState = scrollState,
        )
    } else {
        val context = LocalContext.current
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(state = lazyListState, contentPadding = PaddingValues(vertical = 8.dp)) {
                itemsIndexed(geekListItems) { index, geekListItem ->
                    GeekListItemListItem(
                        index + 1,
                        geekListItem,
                        geekList,
                        onClick = {
                            if (geekListItem.objectId != BggContract.INVALID_ID) {
                                GeekListItemActivity.start(context, geekList!!, geekListItem, index + 1)
                            }
                        },
                    )
                }
            }
            if (imageProgress > 0.0f && imageProgress < 1.0f)
                LinearProgressIndicator(
                    progress = { imageProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    strokeCap = StrokeCap.Butt,
                )
        }
    }
}

@Composable
private fun GeekListItemListItem(
    order: Int,
    geekListItem: GeekListItem,
    geekList: GeekList?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = order.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(end = 16.dp)
                .width(48.dp)
                .wrapContentWidth(Alignment.End)
        )
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(geekListItem.thumbnailUrls?.first()) // TODO iterate through thumbnails?
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.thumbnail_image_empty),
            error = painterResource(id = R.drawable.thumbnail_image_empty),
            modifier = Modifier.size(56.dp)
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = geekListItem.objectName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (geekListItem.username != geekList?.username) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 8.dp),
                    )
                    Text(
                        text = geekListItem.username,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun GeekListItemListItemPreview(
    @PreviewParameter(GeekListPreviewParameterProvider::class) geekListItem: Pair<GeekListItem, Int>,
) {
    BggAppTheme {
        GeekListItemListItem(
            geekListItem.second,
            geekListItem.first,
            GeekList(
                id = 123,
                title = "My GeekList",
                username = "ccomeaux",
                description = "This is a description",
            ),
            {},
            Modifier
        )
    }
}

private class GeekListPreviewParameterProvider : PreviewParameterProvider<Pair<GeekListItem, Int>> {
    override val values = sequenceOf(
        GeekListItem(
            id = 1,
            objectId = 31,
            objectName = "Gaia Project",
            username = "ccomeaux",
            thumbnailUrls = listOf("https://cf.geekdo-images.com/PyUol9QxBnZQCJqZI6bmSA__square/img/610c2mQNSggoh45dO3leJaLBruk=/75x75/filters:strip_icc()/pic8632666.png")
        ) to 1,
        GeekListItem(
            id = 12,
            objectId = 31,
            objectName = "Gaia Project",
            username = "author",
            thumbnailUrls = listOf("https://cf.geekdo-images.com/PyUol9QxBnZQCJqZI6bmSA__square/img/610c2mQNSggoh45dO3leJaLBruk=/75x75/filters:strip_icc()/pic8632666.png")
        ) to 99,
        GeekListItem(
            id = 13,
            objectId = 31,
            objectName = "No Image",
            username = "ccomeaux",
        ) to 100,
    )
}

@Composable
private fun GeekListCommentContent(
    comments: List<GeekListComment>,
    lazyListState: LazyListState = rememberLazyListState(),
    scrollState: ScrollState
) {
    if (comments.isEmpty()) {
        EmptyContent(
            R.string.empty_comments,
            painterResource(R.drawable.ic_twotone_comment_48),
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                    vertical = dimensionResource(R.dimen.material_margin_vertical)
                ),
            scrollState = scrollState,
        )
    } else {
        GeekListCommentList(
            comments,
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
        )
    }
}
