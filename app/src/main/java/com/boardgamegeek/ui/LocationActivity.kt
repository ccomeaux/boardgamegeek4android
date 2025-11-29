package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.Play
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.LocationPlaysViewModel
import com.boardgamegeek.util.XmlApiMarkupConverter
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class LocationActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val locationName = intent.getStringExtra(KEY_LOCATION_NAME).orEmpty()

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Location")
                param(FirebaseAnalytics.Param.ITEM_NAME, locationName)
            }
        }

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            val viewModel by viewModels<LocationPlaysViewModel>()
            val plays by viewModel.plays.observeAsState()
            val updateMessage by viewModel.updateMessage.observeAsState()

            viewModel.setLocation(locationName)

            LaunchedEffect(updateMessage) {
                updateMessage?.getContentIfNotHandled()?.let { content ->
                    snackbarHostState.showSnackbar(content, duration = SnackbarDuration.Long)
                }
            }
            BggAppTheme {
                Drawer {
                    var openDialog by remember { mutableStateOf(false) }
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            LocationPlaysTopBar(
                                locationName = locationName,
                                playCount = plays?.values?.sumOf { list -> list.sumOf { play -> play.quantity } } ?: 0,
                                scrollBehavior = scrollBehavior,
                                onUpClick = {
                                    startActivity(intentFor<LocationsActivity>().clearTop())
                                    finish()
                                },
                                onEditClick = {
                                    // showAndSurvive(EditLocationNameDialogFragment.newInstance(locationName))
                                    openDialog = true
                                }
                            )
                        },
                        snackbarHost = { snackbarHostState }
                    ) { contentPadding ->
                        LocationPlaysScreen(
                            plays,
                            contentPadding = contentPadding,
                        )
                        if (openDialog) {
                            EditDialog(
                                onConfirmation = { newLocationName ->
                                    viewModel.renameLocation(locationName, newLocationName)
                                    openDialog = false
                                },
                                onDismissRequest = {
                                    openDialog = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_LOCATION_NAME = "LOCATION_NAME"

        fun start(context: Context, locationName: String) {
            context.startActivity<LocationActivity>(KEY_LOCATION_NAME to locationName)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LocationPlaysTopBar(
    locationName: String,
    playCount: Int,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onUpClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
) {
    MediumFlexibleTopAppBar(
        title = { Text(locationName.ifBlank { stringResource(R.string.no_location) }) },
        subtitle = {
            if (playCount > 0) {
                Text(pluralStringResource(R.plurals.plays_suffix, playCount, playCount))
            }
        },
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
            IconButton(onClick = { onEditClick() }) {
                Icon(
                    painterResource(R.drawable.edit_24px),
                    contentDescription = stringResource(R.string.menu_edit),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    )
}


@Composable
private fun LocationPlaysScreen(
    plays: Map<String, List<Play>>?,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    when {
        plays == null -> {
            BggLoadingIndicatorBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        }
        plays.isEmpty() -> {
            EmptyFullSizeScrollableContent(
                R.string.empty_plays_location,
                painterResource(R.drawable.plays_24px),
                padding = contentPadding,
            )
        }
        else -> {
            val context = LocalContext.current
            val markupConverter = XmlApiMarkupConverter(context)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                plays.forEach { (headerText, plays) ->
                    stickyHeader {
                        ListHeader(headerText)
                    }
                    items(
                        items = plays,
                        key = { it.internalId },
                    ) {
                        PlayListItem(
                            it,
                            showGameName = true,
                            markupConverter = markupConverter,
                        ) {
                            PlayActivity.start(context, it.internalId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditDialog(
    onConfirmation: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = FocusRequester()
) {
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    var textFieldValue by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(
                onClick = { onConfirmation(textFieldValue.trim()) },
                enabled = textFieldValue.isNotBlank()
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismissRequest() }) { Text(stringResource(R.string.cancel)) }
        },
        title = { Text(text = stringResource(R.string.title_edit_location)) },
        text = {
            TextField(
                value = textFieldValue,
                maxLines = 1,
                label = { Text(stringResource(R.string.location)) },
                onValueChange = {
                    textFieldValue = it
                },
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .focusRequester(focusRequester = focusRequester)
            )
        },
        modifier = modifier,
    )
}