@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.boardgamegeek.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boardgamegeek.R
import com.boardgamegeek.model.Person
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.ArtistsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class ArtistsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
            val viewModel: ArtistsViewModel = viewModel()
            val artists = viewModel.artistsByHeader.observeAsState()
            val sortBy = viewModel.sort.observeAsState(Person.SortType.Name)

            val calculationProgressState = viewModel.statsCalculationProgress.observeAsState(0.0f)
            val animatedProgress by animateFloatAsState(
                targetValue = calculationProgressState.value,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
            )

            BggAppTheme {
                Drawer {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            val showProgress by remember { derivedStateOf { animatedProgress > 0.0f && animatedProgress < 1.0f } }
                            Column {
                                ArtistsTopBar(
                                    artists.value?.values?.sumOf { it.size } ?: 0,
                                    onUpClick = {
                                        onBackPressedDispatcher.onBackPressed()
                                    },
                                    onRefreshClick = { viewModel.calculateStats() },
                                    onSortClick = { sortType ->
                                        viewModel.sort(sortType)
                                    },
                                    sortBy = sortBy.value,
                                    scrollBehavior = scrollBehavior,
                                )
                                if (showProgress) {
                                    LinearProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp),
                                        strokeCap = StrokeCap.Butt,
                                    )
                                }
                            }
                        },
                    ) { contentPadding ->
                        ArtistsContent(
                            artists = artists.value,
                            contentPadding = contentPadding,
                        )
                    }
                }
            }
        }
    }
}

private enum class ArtistsSort(
    val type: Person.SortType,
    @StringRes val labelResId: Int,
) {
    Name(Person.SortType.Name, R.string.menu_sort_name),
    ItemCount(Person.SortType.ItemCount, R.string.menu_sort_item_count),
    Whitmore(Person.SortType.WhitmoreScore, R.string.menu_sort_whitmore_score)
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
private fun ArtistsTopBar(
    artistsCount: Int,
    onUpClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSortClick: (Person.SortType) -> Unit,
    sortBy: Person.SortType,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    var expandedMenu by remember { mutableStateOf(false) }
    MediumFlexibleTopAppBar(
        title = {
            Text(stringResource(id = R.string.title_artists))
        },
        subtitle = {
            if (artistsCount > 0) {
                ArtistsSort.entries.find { it.type == sortBy }?.let {
                    Text(stringResource(R.string.count_by_sort, artistsCount, stringResource(it.labelResId)))
                }
            }
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(
                onClick = { onUpClick() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.up),
                )
            }
        },
        actions = {
            IconButton(onClick = { onRefreshClick() }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.menu_refresh),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = { expandedMenu = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(R.string.menu_sort),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            DropdownMenu(
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false }
            ) {
                ArtistsSort.entries.forEach {
                    DropdownMenuItem(
                        text = { Text(stringResource(it.labelResId)) },
                        leadingIcon = {
                            RadioButton(
                                selected = (it.type == sortBy),
                                onClick = null
                            )
                        },
                        onClick = {
                            expandedMenu = false
                            onSortClick(it.type)
                        }
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun ArtistsHeaderPreview() {
    BggAppTheme {
        ArtistsTopBar(
            1234,
            onUpClick = {},
            onRefreshClick = {},
            onSortClick = {},
            sortBy = Person.SortType.Name,
        )
    }
}

@Composable
private fun ArtistsContent(
    artists: Map<String, List<Person>>?,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    when {
        artists == null -> {
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
        artists.isEmpty() -> {
            EmptyContent(
                R.string.empty_artists, Icons.Outlined.Brush,
                Modifier
                    .padding(contentPadding)
                    .fillMaxSize()
            )
        }
        else -> {
            LazyColumn(contentPadding = contentPadding) {
                artists.forEach { (headerText, artists) ->
                    stickyHeader {
                        ListHeader(headerText)
                    }
                    itemsIndexed(
                        items = artists,
                        key = { _, artist -> artist.id }
                    ) { index, artist ->
                        val context = LocalContext.current
                        PersonListItem(
                            person = artist,
                            modifier = Modifier.animateItem(),
                            onClick = { PersonActivity.startForArtist(context, artist.id, artist.name) },
                        )
                        if (index < artists.lastIndex)
                            HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Preview(heightDp = 256, backgroundColor = 0xFF00FFFF, showBackground = true)
@Composable
private fun TotalPreview() {
    BggAppTheme {
        ArtistsContent(
            mapOf(
                "A" to listOf(
                    Person(
                        internalId = 42L,
                        id = 1,
                        name = "Chris Comeaux",
                        description = "A dude!",
                        updatedTimestamp = null,
                        thumbnailUrl = "",
                        itemCount = 42,
                        whitmoreScore = 17,
                    ),
                    Person(
                        internalId = 43L,
                        id = 2,
                        name = "Stefan Feld",
                        description = "A dude!",
                        updatedTimestamp = Date(12345678901L),
                        thumbnailUrl = "https://cf.geekdo-images.com/yaDyQ_rrLU7P4HUm1ebX5Q__imagepagezoom/img/ryZWb1v-qW3gMLnXE1J6uxOc3LM=/fit-in/1200x900/filters:no_upscale():strip_icc()/pic1452960.jpg",
                        itemCount = 1,
                        whitmoreScore = 0,
                    ),
                )
            )
        )
    }
}
