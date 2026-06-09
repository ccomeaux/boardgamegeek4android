package com.boardgamegeek.ui.collectionshelf

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import com.boardgamegeek.R
import kotlinx.coroutines.android.awaitFrame

@Composable
fun AddCommentDialog(
    gameName: String,
    comment: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onConfirmation: (comment: String) -> Unit = {},
) {
    var textFieldValue by remember { mutableStateOf(comment) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    AlertDialog(
        modifier = modifier,
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(onClick = {
                onConfirmation(textFieldValue)
            }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text(stringResource(R.string.cancel)) }
        },
        title = { Text(gameName) },
        text = {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                        vertical = dimensionResource(R.dimen.material_margin_vertical)
                    )
                    .focusRequester(focusRequester)
                    .focusable(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = MaterialTheme.shapes.small,
                label = { Text(stringResource(R.string.add_comment)) },
                trailingIcon = {
                    IconButton(onClick = { textFieldValue = "" }) {
                        Icon(painterResource(R.drawable.clear_24px), contentDescription = null)
                    }
                }
            )
        }
    )
    LaunchedEffect(Unit) {
        awaitFrame()
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Composable
fun AddTextDialog(
    label: String,
    gameName: String,
    initialText: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onConfirmation: (text: String) -> Unit = {},
) {
    var textFieldValue by remember { mutableStateOf(initialText) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    AlertDialog(
        modifier = modifier,
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(onClick = {
                onConfirmation(textFieldValue)
            }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text(stringResource(R.string.cancel)) }
        },
        title = { Text(gameName) },
        text = {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                        vertical = dimensionResource(R.dimen.material_margin_vertical)
                    )
                    .focusRequester(focusRequester)
                    .focusable(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = MaterialTheme.shapes.small,
                label = { Text(label) },
                trailingIcon = {
                    IconButton(onClick = { textFieldValue = "" }) {
                        Icon(painterResource(R.drawable.clear_24px), contentDescription = null)
                    }
                }
            )
        }
    )
    LaunchedEffect(Unit) {
        awaitFrame()
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Preview
@Composable
private fun CommentDialogPreview() {
    AddCommentDialog(
        "Legacy of Yu",
        "Solo only Solo only Solo only Solo only Solo only Solo only Solo only Solo only Solo only Solo only Solo only Solo only Solo only Solo only",
    )
}

@Preview
@Composable
private fun CommentDialogPreview2() {
    AddTextDialog(
        "Add Comment",
        "Legacy of Yu",
        "Solo only Solo only Solo only Solo only Solo only Solo only Solo only Solo only Solo only Solo only Solo only Solo only Solo only Solo only",
    )
}
//stringResource(R.string.add_comment)