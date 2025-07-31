@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.boardgamegeek.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.model.Location
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.LocationsViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocationsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Locations")
            }
        }

        setContent {
            val context = LocalContext.current
            val viewModel by viewModels<LocationsViewModel>()
            val sortBy by viewModel.sortType.observeAsState(Location.SortType.PLAY_COUNT)
            val locations by viewModel.locations.observeAsState()
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            BggAppTheme {
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        LocationsTopBar(
                            locationCount = locations?.size ?: 0,
                            sortBy = sortBy,
                            scrollBehavior = scrollBehavior,
                            onUpClick = { finish() },
                            onSortClick = { viewModel.sort(it) },
                        )
                    },
                ) { contentPadding ->
                    LocationsScreen(
                        locations,
                        contentPadding = contentPadding,
                        onItemClick = {
                            LocationActivity.start(context, it.name)
                        }
                    )
                }
            }
        }
    }
}

private enum class LocationsSort(
    val type: Location.SortType,
    @StringRes val labelResId: Int,
) {
    Name(Location.SortType.NAME, R.string.menu_sort_name),
    PlayCount(Location.SortType.PLAY_COUNT, R.string.menu_sort_quantity),
}

@Composable
fun LocationsTopBar(
    locationCount: Int,
    sortBy: Location.SortType,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onUpClick: () -> Unit = {},
    onSortClick: (Location.SortType) -> Unit = {},
) {
    var expandedMenu by remember { mutableStateOf(false) }
    MediumFlexibleTopAppBar(
        title = { Text(stringResource(R.string.title_locations)) },
        subtitle = {
            if (locationCount > 0) {
                LocationsSort.entries.find { it.type == sortBy }?.let {
                    Text(stringResource(R.string.count_by_sort, locationCount, stringResource(it.labelResId)))
                }
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
                LocationsSort.entries.forEach {
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
        }
    )
}

@Composable
private fun LocationsScreen(
    locations: Map<String, List<Location>>?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onItemClick: (Location) -> Unit = {},
) {
    when {
        locations == null -> {
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
        locations.isEmpty() -> {
            EmptyContent(
                R.string.empty_locations,
                Icons.Default.LocationOn,
                Modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
            )
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                locations.forEach { (headerText, locations) ->
                    stickyHeader {
                        ListHeader(headerText)
                    }
                    items(
                        items = locations,
                        key = { it.name }
                    ) { location ->
                        LocationListItem(
                            location,
                            onClick = { onItemClick(location) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationListItem(
    location: Location,
    modifier: Modifier = Modifier,
    onClick: (Location) -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.twoLineHeight)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = { onClick(location) })
            .padding(ListItemDefaults.paddingValues)
    ) {
        ListItemPrimaryText(location.name.ifBlank { stringResource(R.string.no_location) })
        ListItemSecondaryText(pluralStringResource(R.plurals.plays_suffix, location.playCount, location.playCount))
    }
}

@Preview
@Composable
private fun LocationListItemPreview(
    @PreviewParameter(LocationPreviewParameterProvider::class) location: Location
) {
    BggAppTheme {
        LocationListItem(location)
    }
}

private class LocationPreviewParameterProvider : PreviewParameterProvider<Location> {
    override val values = sequenceOf(
        Location(
            name = "House",
            playCount = 256,
        ),
        Location(
            name = "Gulf Games",
            playCount = 0,
        ),
        Location(
            name = "Library",
            playCount = 1,
        )
    )
}
