@file:OptIn(ExperimentalMaterial3Api::class)

package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.extensions.getParcelableCompat
import com.boardgamegeek.extensions.link
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.model.GeekListComment
import com.boardgamegeek.model.GeekListItem
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.GameActivity.Companion.start
import com.boardgamegeek.ui.compose.EmptyContent
import com.boardgamegeek.ui.compose.GeekListCommentList
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.util.XmlApiMarkupConverter
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class GeekListItemActivity : BaseActivity() {
    private var geekListId = 0
    private var geekListTitle = ""
    private var order = 0
    private var geekListItem = GeekListItem()

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
                            var selectedDestination by rememberSaveable { mutableIntStateOf(0) }

                            val descriptionScrollState: ScrollState = rememberScrollState()
                            val commentListState: LazyListState = rememberLazyListState()
                            val emptyCommentListScrollState: ScrollState = rememberScrollState()

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
                            GeekListItemTabRow(
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
                                    GeekListItemTab.Description.ordinal -> {
                                        GeekListItemDescriptionContent(
                                            geekListItem.body,
                                            modifier = Modifier.padding(paddingValues),
                                            scrollState = descriptionScrollState,
                                            markupConverter,
                                        )
                                    }
                                    GeekListItemTab.Comments.ordinal -> GeekListItemCommentContent(
                                        geekListItem.comments,
                                        contentPadding = paddingValues,
                                        lazyListState = commentListState,
                                        scrollState = emptyCommentListScrollState,
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
        title = {
            Column {
                Text(geekListTitle.ifEmpty { stringResource(R.string.title_geeklist_item) })
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = { onUpClick() }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(R.string.up))
            }
        },
        actions = {
            IconButton(onClick = { onViewClick() }) { // TODO find a better icon
                Icon(Icons.Default.OpenInBrowser, contentDescription = stringResource(R.string.menu_view_in_browser))
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

private enum class GeekListItemTab(@StringRes val resId: Int) {
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
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    val dividerModifier = Modifier
        .size(18.dp)
        .padding(horizontal = 8.dp)
    val iconModifier = Modifier
        .size(18.dp)
        .padding(end = 8.dp)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.card_padding_openSource))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = numberFormat.format(rank),
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
                Icon(
                    Icons.Outlined.AccountCircle,
                    contentDescription = stringResource(R.string.author),
                    modifier = iconModifier
                )
                Text(
                    text = geekListItem.username,
                    style = MaterialTheme.typography.bodyLarge,
                )
                val typeResId = geekListItem.titleResId()
                if (typeResId != ResourcesCompat.ID_NULL) {
                    VerticalDivider(dividerModifier)
                    Icon(
                        Icons.AutoMirrored.Outlined.Label,
                        contentDescription = stringResource(R.string.type),
                        modifier = iconModifier
                    )
                    Text(
                        text = stringResource(typeResId),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.ThumbUp,
                    contentDescription = stringResource(R.string.number_of_thumbs),
                    modifier = iconModifier,
                )
                Text(
                    text = numberFormat.format(geekListItem.numberOfThumbs),
                    style = MaterialTheme.typography.bodyMedium,
                )
                VerticalDivider(dividerModifier)
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = stringResource(R.string.posted),
                    modifier = iconModifier,
                )
                var relativePostTimestamp by remember { mutableStateOf(geekListItem.postDateTime.formatTimestamp(context).toString()) }
                var relativeEditTimestamp by remember { mutableStateOf(geekListItem.editDateTime.formatTimestamp(context).toString()) }
                Text(
                    text = relativePostTimestamp,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
                if (geekListItem.postDateTime != geekListItem.editDateTime) {
                    VerticalDivider(dividerModifier)
                    Icon(
                        Icons.Outlined.Update,
                        contentDescription = stringResource(R.string.edited),
                        modifier = iconModifier,
                    )
                    Text(
                        text = relativeEditTimestamp,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(30.seconds)
                        relativePostTimestamp = geekListItem.postDateTime.formatTimestamp(context).toString()
                        relativeEditTimestamp = geekListItem.editDateTime.formatTimestamp(context).toString()
                    }
                }
            }
        }
    }
}

@Composable
private fun GeekListItemDescriptionContent(
    body: String,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    markupConverter: XmlApiMarkupConverter? = null
) {
    if (body.isEmpty()) {
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
            text = AnnotatedString.fromHtml(markupConverter?.toHtml(body) ?: body),
            modifier = modifier
                .fillMaxSize()
                .padding(top = 16.dp)
                .verticalScroll(scrollState)
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
