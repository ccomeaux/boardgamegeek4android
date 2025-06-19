package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ListAlt
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
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.databinding.ActivityDrawerComposeBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.model.GeekListItem
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.model.Status
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.EmptyContent
import com.boardgamegeek.ui.compose.GeekListCommentList
import com.boardgamegeek.ui.compose.GeekListHeader
import com.boardgamegeek.ui.compose.LoadingIndicator
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.GeekListViewModel
import com.boardgamegeek.util.XmlApiMarkupConverter
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class GeekListActivity : DrawerActivity() {
    private lateinit var binding: ActivityDrawerComposeBinding
    private var geekListId = BggContract.INVALID_ID
    private var geekListTitle: String = ""
    private val viewModel by viewModels<GeekListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        geekListId = intent.getIntExtra(KEY_ID, BggContract.INVALID_ID)
        geekListTitle = intent.getStringExtra(KEY_TITLE).orEmpty()

        safelySetTitle(geekListTitle)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekList")
                param(FirebaseAnalytics.Param.ITEM_ID, geekListId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, geekListTitle)
            }
        }

        viewModel.geekList.observe(this) {
            it?.let { (_, data, _) ->
                data?.let { entity ->
                    geekListTitle = entity.title
                    safelySetTitle(geekListTitle)
                }
            }
        }
        viewModel.setId(geekListId)

        binding.composeView.setContent {
            val markupConverter = XmlApiMarkupConverter(this)
            val geekList = viewModel.geekList.observeAsState(RefreshableResource.refreshing(null))
            BggAppTheme {
                when (geekList.value.status) {
                    Status.ERROR -> {
                        // TODO show error
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = geekList.value.message,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                    Status.REFRESHING -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LoadingIndicator(
                                Modifier
                                    .align(Alignment.Center)
                                    .padding(dimensionResource(R.dimen.padding_extra))
                            )
                        }
                    }
                    Status.SUCCESS -> {
                        val paddingValues = PaddingValues(
                            horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                            vertical = dimensionResource(R.dimen.material_margin_vertical)
                        )
                        geekList.value.data?.let {
                            Column(modifier = Modifier.fillMaxSize()) {
                                var selectedDestination by rememberSaveable { mutableIntStateOf(0) }
                                val descriptionScrollState: ScrollState = rememberScrollState()
                                val itemListState: LazyListState = rememberLazyListState()
                                val commentListState: LazyListState = rememberLazyListState()

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(paddingValues)
                                ) {
                                    GeekListHeader(it, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                                }
                                GeekListTabRow(
                                    selectedDestination = selectedDestination,
                                    onClick = { newDestination -> selectedDestination = newDestination },
                                )
                                when (selectedDestination) {
                                    GeekListTab.Description.ordinal -> {
                                        GeekListDescriptionContent(
                                            it,
                                            modifier = Modifier.padding(paddingValues),
                                            scrollState = descriptionScrollState,
                                            markupConverter,
                                        )
                                    }
                                    GeekListTab.Items.ordinal -> GeekListItemListContent(
                                        it,
                                        contentPadding = paddingValues,
                                        state = itemListState
                                    )
                                    GeekListTab.Comments.ordinal -> GeekListItemCommentContent(
                                        it,
                                        contentPadding = paddingValues,
                                        state = commentListState
                                    )
                                }
                            }
                        } ?: EmptyContent(
                            R.string.empty_geeklist,
                            Icons.AutoMirrored.Filled.ListAlt,
                            Modifier
                                .padding(paddingValues)
                                .fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    override fun bindLayout() {
        binding = ActivityDrawerComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun safelySetTitle(title: String?) {
        if (!title.isNullOrBlank()) {
            supportActionBar?.title = title
        }
    }

    override val optionsMenuId = R.menu.view_share

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_view -> linkToBgg("geeklist", geekListId)
            R.id.menu_share -> {
                val description = String.format(getString(R.string.share_geeklist_text), geekListTitle)
                val uri = createBggUri("geeklist", geekListId)
                share(getString(R.string.share_geeklist_subject), "$description\n\n$uri")

                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekList")
                    param(FirebaseAnalytics.Param.ITEM_ID, geekListId.toString())
                    param(FirebaseAnalytics.Param.ITEM_ID, geekListTitle)
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
        return true
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

enum class GeekListTab(@StringRes val resId: Int) {
    Description(R.string.title_description),
    Items(R.string.title_items),
    Comments(R.string.title_comments),
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val geekList =            GeekList(
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        //GeekListHeader(geekList, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        Text(
            text = AnnotatedString.fromHtml(markupConverter?.toHtml(geekList.description) ?: geekList.description),
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}


@Composable
fun GeekListItemListContent(geekList: GeekList?, contentPadding: PaddingValues, state: LazyListState = rememberLazyListState()) {
    val geekListItems = geekList?.items.orEmpty()
    if (geekListItems.isEmpty()) {
        EmptyContent(
            R.string.empty_geeklist,
            Icons.AutoMirrored.Filled.List,
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
        )
    } else {
        val context = LocalContext.current
        LazyColumn(state = state, contentPadding = contentPadding) {
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
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick),
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
            Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            )
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
fun GeekListItemCommentContent(data: GeekList?, contentPadding: PaddingValues, state: LazyListState = rememberLazyListState()) {
    if (data?.comments.isNullOrEmpty()) {
        EmptyContent(
            R.string.empty_comments,
            painterResource(R.drawable.ic_twotone_comment_48),
            Modifier
                .padding(contentPadding)
                .fillMaxSize(),
        )
    } else {
        GeekListCommentList(
            data?.comments.orEmpty(),
            contentPadding = contentPadding,
            state = state,
        )
    }
}
