package com.boardgamegeek.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.pm.ShortcutManagerCompat
import com.boardgamegeek.R
import com.boardgamegeek.ui.compose.BggLoadingIndicator
import com.boardgamegeek.ui.compose.CollectionItemListItem
import com.boardgamegeek.ui.compose.EmptyContent
import com.boardgamegeek.ui.compose.SearchTextField
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.ShortcutSelectionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShortcutSelectionActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val viewModel by viewModels<ShortcutSelectionViewModel>()
            val snackbarHostState = remember { SnackbarHostState() }

            val collectionItems by viewModel.items.observeAsState()
            val createdShortcut by viewModel.createdShortcut.observeAsState()
            LaunchedEffect(createdShortcut) {
                createdShortcut?.getContentIfNotHandled()?.let {
                    setResult(RESULT_OK, ShortcutManagerCompat.createShortcutResultIntent(context, it))
                    finish()
                }
            }

            val errorMessage by viewModel.errorMessage.observeAsState()
            LaunchedEffect(errorMessage) {
                errorMessage?.getContentIfNotHandled()?.let {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(it)
                    }
                }
            }

            BggAppTheme {
                val textFieldState: TextFieldState = rememberTextFieldState()
                Scaffold(
                    topBar = {
                        Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                            LaunchedEffect(textFieldState.text) {
                                viewModel.filter(textFieldState.text.toString())
                            }
                            ShortcutTopAppBar(
                                textFieldState = textFieldState,
                                onCloseClick = { finish() },
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                ) { contentPadding ->
                    when {
                        collectionItems == null -> {
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
                        collectionItems.orEmpty().isEmpty() -> {
                            EmptyContent(
                                if (textFieldState.text.isBlank())
                                    R.string.empty_games
                                else
                                    R.string.empty_search,
                                Icons.Default.Collections,
                                Modifier
                                    .fillMaxSize()
                                    .padding(contentPadding)
                                    .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal)),
                            )
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = contentPadding,
                            ) {
                                items(
                                    items = collectionItems.orEmpty(),
                                ) {
                                    CollectionItemListItem(
                                        name = it.collectionName,
                                        thumbnailUrl = it.robustThumbnailUrl,
                                        yearPublished = it.yearPublished,
                                        rating = it.rating,
                                        onClick = { viewModel.createShortcut(it) },
                                        onLongClick = {},
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortcutTopAppBar(
    modifier: Modifier = Modifier,
    textFieldState: TextFieldState = rememberTextFieldState(),
    onCloseClick: () -> Unit = {},
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    TopAppBar(
        title = {
            AnimatedContent(targetState = isSearchExpanded, label = "cross fade") { expanded ->
                if (expanded) {
                    SearchTextField(
                        textFieldState = textFieldState,
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .fillMaxWidth()
                            .padding(end = dimensionResource(R.dimen.material_margin_horizontal)),
                        leadingIcon = {
                            IconButton(
                                onClick = {
                                    textFieldState.clearText()
                                    isSearchExpanded = false
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            }
                        },
                        placeholderText = stringResource(R.string.search_hint),
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                } else {
                    Text(stringResource(R.string.title_shortcut_selection))
                }
            }
        },
        modifier = modifier,
        navigationIcon = {
            if (!isSearchExpanded) {
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }
        },
        actions = {
            if (!isSearchExpanded) {
                IconButton(
                    onClick = { isSearchExpanded = true },
                ) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.menu_search))
                }
            }
        }
    )
}

@Preview
@Composable
private fun Preview() {
    BggAppTheme {
        ShortcutTopAppBar()
    }
}
