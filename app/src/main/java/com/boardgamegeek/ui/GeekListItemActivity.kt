@file:OptIn(ExperimentalMaterial3Api::class)

package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.model.GeekListComment
import com.boardgamegeek.model.GeekListItem
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.GameActivity.Companion.start
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.util.XmlApiMarkupConverter
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class GeekListItemActivity : BaseActivity() {
    private var geekListId = 0
    private var geekListTitle = ""
    private var order = 0
    private var geekListItem = GeekListItem()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geekListTitle = intent.getStringExtra(KEY_TITLE).orEmpty()
        geekListId = intent.getIntExtra(KEY_ID, BggContract.INVALID_ID)
        order = intent.getIntExtra(KEY_ORDER, 0)
        geekListItem = intent.getParcelableCompat(KEY_ITEM) ?: GeekListItem()

        if (savedInstanceState == null && geekListItem.objectId != BggContract.INVALID_ID) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekListItem")
                param(FirebaseAnalytics.Param.ITEM_ID, geekListItem.objectId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, geekListItem.objectName)
            }
        }

        setContent {
            val markupConverter = XmlApiMarkupConverter(LocalContext.current)
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
            BggAppTheme {
                Drawer {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            GeekListItemTopAppBar(
                                geekListItem.objectName,
                                onUpClick = { navigateUp() },
                                onViewClick = { openInBrowser() },
                                scrollBehavior = scrollBehavior,
                            )
                        }
                    ) { contentPadding ->
                        Column(modifier = Modifier.padding(contentPadding)) {
                            val paddingValues = PaddingValues(
                                horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                                vertical = dimensionResource(R.dimen.material_margin_vertical)
                            )
                            var showImage by remember { mutableStateOf(true) }
                            AnimatedVisibility(showImage) {
                                AsyncImage(
                                    model = geekListItem.heroImageUrls?.first(), // TODO iterate through URLs?
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal))
                                        .heightIn(max = 128.dp)
                                        .clip(MaterialTheme.shapes.medium),
                                    onError = { showImage = false },
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(paddingValues)
                            ) {
                                GeekListItemHeader(
                                    geekListItem,
                                    order,
                                    geekListTitle,
                                )
                            }
                            val coroutineScope = rememberCoroutineScope()
                            val pagerState = rememberPagerState(
                                initialPage = GeekListItemTab.Description.ordinal,
                                pageCount = { GeekListItemTab.entries.size },
                            )
                            GeekListItemTabRow(
                                selectedDestination = pagerState.currentPage,
                                onClick = { newDestination ->
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(newDestination)
                                    }
                                },
                            )
                            HorizontalPager(pagerState) { targetState ->
                                when (targetState) {
                                    GeekListItemTab.Description.ordinal -> {
                                        GeekListItemDescriptionContent(
                                            geekListItem.body,
                                            markupConverter = markupConverter,
                                        )
                                    }
                                    GeekListItemTab.Comments.ordinal -> GeekListItemCommentContent(
                                        geekListItem.comments,
                                        contentPadding = paddingValues,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun navigateUp() {
        if (geekListId != BggContract.INVALID_ID) {
            GeekListActivity.startUp(this, geekListId, geekListTitle)
            finish()
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun openInBrowser() = if (geekListItem.isBoardGame) {
        if (geekListItem.objectId == BggContract.INVALID_ID || geekListItem.objectName.isBlank()) false else {
            start(this, geekListItem.objectId, geekListItem.objectName)
            true
        }
    } else {
        if (geekListItem.objectUrl.isBlank()) false else {
            link(geekListItem.objectUrl)
            true
        }
    }

    companion object {
        private const val KEY_ID = "GEEK_LIST_ID"
        private const val KEY_ORDER = "GEEK_LIST_ORDER"
        private const val KEY_TITLE = "GEEK_LIST_TITLE"
        private const val KEY_ITEM = "GEEK_LIST_ITEM"

        fun start(context: Context, geekList: GeekList, item: GeekListItem, order: Int) {
            context.startActivity<GeekListItemActivity>(
                KEY_ID to geekList.id,
                KEY_TITLE to geekList.title,
                KEY_ORDER to order,
                KEY_ITEM to item,
            )
        }
    }
}

@Composable
private fun GeekListItemTopAppBar(
    geekListTitle: String,
    onUpClick: () -> Unit,
    onViewClick: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    MediumTopAppBar(
        title = { Text(geekListTitle.ifEmpty { stringResource(R.string.title_geeklist_item) }) },
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
        actions = {
            IconButton(onClick = { onViewClick() }) {
                Icon(
                    painterResource(R.drawable.open_in_browser_24px),
                    contentDescription = stringResource(R.string.menu_view_in_browser)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun GeekListItemTopAppBarPreview() {
    BggAppTheme {
        GeekListItemTopAppBar("A GeekList Item", {}, {})
    }
}

private enum class GeekListItemTab(@param:StringRes val resId: Int) {
    Description(R.string.title_description),
    Comments(R.string.title_comments),
}

@Composable
private fun GeekListItemTabRow(selectedDestination: Int, modifier: Modifier = Modifier, onClick: (Int) -> Unit) {
    SecondaryTabRow(
        selectedTabIndex = selectedDestination,
        modifier = modifier,
    ) {
        GeekListItemTab.entries.forEachIndexed { index, tab ->
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

@Composable
private fun GeekListItemCommentContent(
    comments: List<GeekListComment>,
    contentPadding: PaddingValues,
    lazyListState: LazyListState = rememberLazyListState(),
    scrollState: ScrollState = rememberScrollState(),
) {
    if (comments.isEmpty()) {
        EmptyFullSizeScrollableContent(
            textResource = R.string.empty_comments,
            iconPainter = painterResource(R.drawable.ic_twotone_comment_48),
            scrollState = scrollState,
            padding = contentPadding,
        )
    } else {
        GeekListCommentList(
            comments,
            modifier = Modifier.padding(contentPadding),
            state = lazyListState,
        )
    }
}

@StringRes
private fun GeekListItem.titleResId(): Int {
    return when (this.objectType) {
        GeekListItem.ObjectType.BoardGame -> R.string.title_board_game
        GeekListItem.ObjectType.BoardGameAccessory -> R.string.title_board_game_accessory
        GeekListItem.ObjectType.Thing -> R.string.title_thing
        GeekListItem.ObjectType.Publisher -> R.string.title_board_game_publisher
        GeekListItem.ObjectType.Company -> R.string.title_company
        GeekListItem.ObjectType.Designer -> R.string.title_board_game_designer
        GeekListItem.ObjectType.Person -> R.string.title_person
        GeekListItem.ObjectType.BoardGameFamily -> R.string.title_board_game_family
        GeekListItem.ObjectType.Family -> R.string.title_family
        GeekListItem.ObjectType.File -> R.string.title_file
        GeekListItem.ObjectType.GeekList -> R.string.title_geeklist
        GeekListItem.ObjectType.Unknown -> ResourcesCompat.ID_NULL
    }
}

@Composable
private fun GeekListItemHeader(geekListItem: GeekListItem, rank: Int, geekListTitle: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.card_padding_openSource))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = rank.toFormattedString(),
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Text(
                    text = geekListTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ListItemSecondaryText(
                    geekListItem.username,
                    icon = painterResource(R.drawable.account_circle_24px),
                    contentDescription = stringResource(R.string.author),
                    textStyle = MaterialTheme.typography.bodyLarge,
                )
                val typeResId = geekListItem.titleResId()
                if (typeResId != ResourcesCompat.ID_NULL) {
                    ListItemVerticalDivider()
                    ListItemSecondaryText(
                        stringResource(typeResId),
                        icon = painterResource(R.drawable.label_24px),
                        contentDescription = stringResource(R.string.type),
                        textStyle = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ListItemSecondaryText(
                    geekListItem.numberOfThumbs.toFormattedString(),
                    icon = painterResource(R.drawable.thumb_up_24px),
                    contentDescription = stringResource(R.string.number_of_thumbs)
                )
                ListItemVerticalDivider()
                var relativePostTimestamp by remember { mutableStateOf("") }
                var relativeEditTimestamp by remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    while (true) {
                        relativePostTimestamp = geekListItem.postDateTime.formatTimestamp(context).toString()
                        relativeEditTimestamp = geekListItem.editDateTime.formatTimestamp(context).toString()
                        delay(30.seconds)
                    }
                }
                ListItemSecondaryText(
                    relativePostTimestamp,
                    icon = painterResource(R.drawable.time_24px),
                    contentDescription = stringResource(R.string.posted)
                )
                if (geekListItem.postDateTime != geekListItem.editDateTime) {
                    ListItemVerticalDivider()
                    ListItemSecondaryText(
                        relativeEditTimestamp,
                        icon = painterResource(R.drawable.time_edit_24px),
                        contentDescription = stringResource(R.string.edited)
                    )
                }
            }
        }
    }
}

@Composable
private fun GeekListItemDescriptionContent(
    body: String,
    scrollState: ScrollState = rememberScrollState(),
    markupConverter: XmlApiMarkupConverter? = null
) {
    if (body.isEmpty()) {
        EmptyFullSizeScrollableContent(
            R.string.empty_geeklist_description,
            painterResource(R.drawable.description_24px),
            scrollState = scrollState,
        )
    } else {
        Text(
            text = AnnotatedString.fromHtml(markupConverter?.toHtml(body, prewrap = false) ?: body),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                    vertical = dimensionResource(R.dimen.material_margin_vertical),
                )
        )
    }
}

@PreviewLightDark
@Composable
private fun GeekListItemPreview(
    @PreviewParameter(provider = GeekListItemPreviewParameterProvider::class) geekListItem: Pair<GeekListItem, Int>
) {
    BggAppTheme {
        GeekListItemHeader(
            geekListItem.first,
            geekListItem.second,
            "This is the GeekList Title. It is a very long title that should be truncated! Alas, I'm not sure it will be.",
            modifier = Modifier.padding(8.dp)
        )
    }
}

private class GeekListItemPreviewParameterProvider : PreviewParameterProvider<Pair<GeekListItem, Int>> {
    override val values = sequenceOf(
        GeekListItem(
            id = 1,
            objectId = 31,
            objectName = "Gaia Project",
            objectType = GeekListItem.ObjectType.BoardGame,
            username = "ccomeaux",
            numberOfThumbs = 42,
            thumbnailUrls = listOf("https://cf.geekdo-images.com/PyUol9QxBnZQCJqZI6bmSA__square/img/610c2mQNSggoh45dO3leJaLBruk=/75x75/filters:strip_icc()/pic8632666.png"),
            postDateTime = 1234123412345L,
            editDateTime = 1234123412345L,
        ) to 1,
        GeekListItem(
            id = 12,
            objectId = 31,
            objectName = "Gaia Project",
            objectType = GeekListItem.ObjectType.BoardGame,
            username = "author",
            numberOfThumbs = 4321,
            thumbnailUrls = listOf("https://cf.geekdo-images.com/PyUol9QxBnZQCJqZI6bmSA__square/img/610c2mQNSggoh45dO3leJaLBruk=/75x75/filters:strip_icc()/pic8632666.png"),
            postDateTime = 1234123412345L,
            editDateTime = 1234123454321L,
        ) to 99,
        GeekListItem(
            id = 13,
            objectId = 31,
            objectName = "No Image",
            objectType = GeekListItem.ObjectType.BoardGame,
            numberOfThumbs = 0,
            username = "ccomeaux",
        ) to 100,
    )
}
