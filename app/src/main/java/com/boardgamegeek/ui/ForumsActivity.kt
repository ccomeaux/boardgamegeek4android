package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boardgamegeek.R
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.model.Forum
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.model.Status
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.ForumsViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@AndroidEntryPoint
class ForumsActivity : BaseActivity() {
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Forums")
            }
        }

        intent?.let {
            objectId = it.getIntExtra(KEY_OBJECT_ID, BggContract.INVALID_ID)
            objectName = it.getStringExtra(KEY_OBJECT_NAME).orEmpty()
        }

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            val viewModel: ForumsViewModel = viewModel()
            val forums = viewModel.forums.observeAsState(RefreshableResource.refreshing(null))
            viewModel.setRegion()

            BggAppTheme {
                Drawer(
                    selectedItem = DrawerItem.Forums,
                    drawerState = drawerState,
                ) {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            ForumsTopAppBar(
                                onMenuClick = {
                                    coroutineScope.launch {
                                        drawerState.open()
                                    }
                                },
                                scrollBehavior = scrollBehavior,
                            )
                        }
                    ) { contentPadding ->
                        LazyColumn(contentPadding = contentPadding) { }
                        when (forums.value.status) {
                            Status.ERROR -> {
                                ErrorContent(
                                    text = forums.value.message.ifEmpty { stringResource(R.string.error_loading_forums) },
                                    imageVector = Icons.AutoMirrored.Filled.ListAlt,
                                    modifier = Modifier.padding(contentPadding)
                                )
                            }
                            Status.REFRESHING -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(contentPadding)
                                ) {
                                    BggLoadingIndicator(
                                        Modifier
                                            .align(Alignment.Center)
                                            .padding(dimensionResource(R.dimen.padding_extra))
                                    )
                                }
                            }
                            Status.SUCCESS -> {
                                forums.value.data?.let {
                                    val context = LocalContext.current
                                    ForumsContent(it, contentPadding) { forum ->
                                        ForumActivity.start(context, forum.id, forum.title, objectId, objectName, Forum.Type.REGION)
                                    }
                                } ?: EmptyContent(
                                    R.string.empty_forums,
                                    Icons.Filled.Forum,
                                    Modifier
                                        .fillMaxSize()
                                        .padding(contentPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_OBJECT_ID = "ID"
        private const val KEY_OBJECT_NAME = "NAME"

        fun startUp(context: Context) = context.startActivity(context.intentFor<ForumsActivity>().clearTop())
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
private fun ForumsTopAppBar(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    MediumFlexibleTopAppBar(
        title = { Text(stringResource(R.string.title_forums)) },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = { onMenuClick() }) {
                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.navigation_drawer))
            }
        }
    )
}

@Composable
private fun ForumsContent(
    forums: List<Forum>?,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onItemClick: (forum: Forum) -> Unit,
) {
    when {
        forums == null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                BggLoadingIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .padding(dimensionResource(R.dimen.padding_extra))
                )
            }
        }
        forums.isEmpty() -> {
            EmptyContent(
                R.string.empty_forums, Icons.Outlined.Forum,
                Modifier
                    .padding(contentPadding)
                    .fillMaxSize()
            )
        }
        else -> {
            LazyColumn(modifier = Modifier.padding(contentPadding)) {
                itemsIndexed(
                    items = forums,
                    key = { _, forum -> forum.id }
                ) { index, forum ->
                    ForumListItem(
                        forum = forum,
                        modifier = Modifier,
                        onClick = { onItemClick(forum) },
                    )
                    if (index < forums.lastIndex)
                        HorizontalDivider() // TODO skip this on either side of a header
                }
            }
        }
    }
}
