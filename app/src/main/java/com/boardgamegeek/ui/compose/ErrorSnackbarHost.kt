package com.boardgamegeek.ui.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.boardgamegeek.ui.theme.BggAppTheme
import kotlinx.coroutines.launch

@Composable
fun ErrorSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState = hostState) { snackbarData ->
        Snackbar(
            snackbarData = snackbarData,
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        )
    }
}

@Preview
@Composable
private fun ErrorSnackbarHostPreview() {
    BggAppTheme {
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            snackbarHost = { ErrorSnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            Button(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            "This is a custom snackbar message"
                        )
                    }
                },
                modifier = Modifier.padding(paddingValues)
            ) {
                Text("Show Snackbar")
            }
        }
    }
}