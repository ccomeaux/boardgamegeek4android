package com.boardgamegeek.ui.compose

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.boardgamegeek.R
import com.boardgamegeek.ui.theme.BggAppTheme

@Composable
fun SearchTextField(
    textFieldState: TextFieldState,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onClearClick: () -> Unit = { textFieldState.clearText() },
    leadingIcon: @Composable (() -> Unit)? = {
        Icon(painterResource(R.drawable.search_24px), contentDescription = null)
    },
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
                    Icon(painterResource(R.drawable.clear_24px), contentDescription = stringResource(R.string.clear))
                }
            }
        },
    )
}

@Preview
@Composable
private fun SearchTextFieldPreview() {
    BggAppTheme() {
        SearchTextField(TextFieldState("ticket"))
    }
}