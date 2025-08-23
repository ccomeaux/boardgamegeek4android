package com.boardgamegeek.ui.compose

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.boardgamegeek.R

@Composable
fun SearchTextField(
    textFieldState: TextFieldState,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onClearClick: () -> Unit = { textFieldState.clearText() },
    leadingIcon: @Composable (() -> Unit)? = { Icon(Icons.Default.Search, contentDescription = null) },
    placeholderText: String = stringResource(R.string.menu_search)
) {
    TextField(
        state = textFieldState,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { performDefaultAction ->
            onSearchClick()
            performDefaultAction()
        },
        lineLimits = TextFieldLineLimits.SingleLine,
        placeholder = { Text(placeholderText) },
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.extraLarge,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        leadingIcon = leadingIcon,
        trailingIcon = {
            if (textFieldState.text.isNotEmpty()) {
                IconButton(onClick = { onClearClick() }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                }
            }
        },
    )
}