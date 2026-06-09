package com.boardgamegeek.ui.collectionshelf

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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.boardgamegeek.R
import com.boardgamegeek.model.Game
import kotlinx.coroutines.android.awaitFrame

@Composable
fun AddRatingDialog(
    gameName: String,
    rating: Double,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onConfirmation: (rating: Double) -> Unit = {},
) {
    var textFieldValue by remember { mutableStateOf(if (rating == Game.UNRATED) "" else rating.toString()) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val textFieldHasErrors by remember {
        derivedStateOf {
            if (textFieldValue.isNotEmpty()) {
                val rating = textFieldValue.toDoubleOrNull()
                if (rating != null) {
                    rating !in 1.0..10.0
                } else true
            } else {
                false
            }
        }
    }
    AlertDialog(
        modifier = modifier,
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                enabled = !textFieldHasErrors,
                onClick = {
                    onConfirmation(textFieldValue.toDoubleOrNull() ?: Game.UNRATED)
                },
            ) {
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
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.small,
                label = { Text(stringResource(R.string.menu_rate_item)) },
                singleLine = true,
                isError = textFieldHasErrors,
                supportingText = { Text(stringResource(R.string.rate_item_suggestion)) },
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

@Preview(showBackground = true)
@Composable
private fun RatingDialogPreview() {
    AddRatingDialog(
        "Legacy of Yu",
        6.0,
    )
}
