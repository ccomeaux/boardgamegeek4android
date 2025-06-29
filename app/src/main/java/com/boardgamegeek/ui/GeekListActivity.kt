package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
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
            val animatedImageProgress = animateFloatAsState(imageProgress.value, animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec)
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
                                    SuccessfulGeekListContent(contentPadding, it, animatedImageProgress.value, markupConverter)
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
                .padding(contentPadding)
                .fillMaxSize()
        )
    }

    @Composable
    private fun SuccessfulGeekListContent(
        contentPadding: PaddingValues,
        geekList: GeekList,
        imageProgress: Float,
        markupConverter: XmlApiMarkupConverter
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            var selectedDestination by rememberSaveable { mutableIntStateOf(0) }

            val descriptionScrollState: ScrollState = rememberScrollState()
            val itemListState: LazyListState = rememberLazyListState()
            val emptyItemListScrollState: ScrollState = rememberScrollState()
            val commentListState: LazyListState = rememberLazyListState()
            val emptyCommentListScrollState: ScrollState = rememberScrollState()

            val paddingValues = PaddingValues(
                horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                vertical = dimensionResource(R.dimen.material_margin_vertical)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(paddingValues)
            ) {
                GeekListHeader(geekList, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            GeekListTabRow(
                selectedDestination = selectedDestination,
                onClick = { newDestination -> selectedDestination = newDestination },
            )
            AnimatedContent(
                targetState = selectedDestination,
                transitionSpec = {
                    val slideDirection =
                        if (targetState > initialState) AnimatedContentTransitionScope.SlideDirection.Start
                        else AnimatedContentTransitionScope.SlideDirection.End
                    slideIntoContainer(towards = slideDirection) togetherWith slideOutOfContainer(towards = slideDirection)
                }
            ) { targetState ->
                when (targetState) {
                    GeekListTab.Description.ordinal -> {
                        GeekListDescriptionContent(
                            geekList,
                            modifier = Modifier.padding(paddingValues),
                            scrollState = descriptionScrollState,
                            markupConverter,
                        )
                    }
                    GeekListTab.Items.ordinal -> GeekListItemListContent(
                        geekList,
                        contentPadding = paddingValues,
                        imageProgress = imageProgress,
                        lazyListState = itemListState,
                        scrollState = emptyItemListScrollState,
                    )
                    GeekListTab.Comments.ordinal -> GeekListCommentContent(
                        geekList.comments,
                        contentPadding = paddingValues,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeekListTopAppBar(
    geekListTitle: String,
    onBack: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    MediumTopAppBar(
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

@OptIn(ExperimentalMaterial3Api::class)
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

enum class GeekListTab(@StringRes val resId: Int) {
    Description(R.string.title_description),
    Items(R.string.title_items),
    Comments(R.string.title_comments),
}

@Composable
fun GeekListTabRow(selectedDestination: Int, modifier: Modifier = Modifier, onClick: (Int) -> Unit) {
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
fun GeekListDescriptionContent(
    geekList: GeekList,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    markupConverter: XmlApiMarkupConverter? = null
) {
    if (geekList.description.isEmpty()) {
        EmptyContent(
            R.string.empty_geeklist_description,
            Icons.Filled.Description,
            modifier = modifier
                .padding(top = 16.dp)
                .fillMaxSize(),
            scrollState = scrollState,
        )
    } else {
        Text(
            text = AnnotatedString.fromHtml(markupConverter?.toHtml(geekList.description) ?: geekList.description),
            modifier = modifier
                .fillMaxSize()
                .padding(top = 16.dp)
                .verticalScroll(scrollState)
        )
    }
}

@Composable
fun GeekListItemListContent(
    geekList: GeekList?, // TODO don't accept nulls
    imageProgress: Float,
    contentPadding: PaddingValues,
    lazyListState: LazyListState = rememberLazyListState(),
    scrollState: ScrollState = rememberScrollState(),
) {
    val geekListItems = geekList?.items.orEmpty()
    if (geekListItems.isEmpty()) {
        EmptyContent(
            R.string.empty_geeklist,
            Icons.AutoMirrored.Filled.List,
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            scrollState = scrollState,
        )
    } else {
        val context = LocalContext.current
        Box {
            LazyColumn(state = lazyListState, contentPadding = contentPadding) {
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
                        modifier = Modifier.padding(vertical = dimensionResource(R.dimen.material_margin_vertical))
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

@Preview
@Composable
private fun GeekListItemListContentPreview() {
    BggAppTheme {
        GeekListItemListContent(null, 0.6f, PaddingValues(16.dp))
    }
}

@Composable
fun GeekListItemListItem(order: Int, geekListItem: GeekListItem, geekList: GeekList?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val iconModifier = Modifier
        .size(18.dp)
        .padding(end = 8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = order.toString(),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(end = 16.dp)
                .widthIn(min = 40.dp)
                .wrapContentWidth(Alignment.End)
        )
        AsyncImage(
            model = geekListItem.thumbnailUrls?.first(), // TODO iterate through thumbnails?
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.thumbnail_image_empty),
            error = painterResource(id = R.drawable.thumbnail_image_empty),
            modifier = Modifier.size(56.dp)
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = geekListItem.objectName,
                style = MaterialTheme.typography.titleMedium,
            )
            if (geekListItem.username != geekList?.username) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        modifier = iconModifier,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

class GeekListPreviewParameterProvider : PreviewParameterProvider<Pair<GeekListItem, Int>> {
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
fun GeekListCommentContent(
    comments: List<GeekListComment>,
    contentPadding: PaddingValues,
    lazyListState: LazyListState = rememberLazyListState(),
    scrollState: ScrollState
) {
    if (comments.isEmpty()) {
        EmptyContent(
            R.string.empty_comments,
            painterResource(R.drawable.ic_twotone_comment_48),
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
            scrollState = scrollState,
        )
    } else {
        GeekListCommentList(
            comments,
            contentPadding = contentPadding,
            state = lazyListState,
        )
    }
}
