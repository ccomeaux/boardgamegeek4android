package com.boardgamegeek.ui.person

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.model.Person.Type

@Composable
fun Type.mapToDescription(): String {
    return when (this) {
        Type.Artist -> stringResource(R.string.title_artist)
        Type.Designer -> stringResource(R.string.title_designer)
        Type.Publisher -> stringResource(R.string.title_publisher)
    }
}

@Composable
fun Type.mapToPainter(): Painter {
    return when (this) {
        Type.Artist -> painterResource(R.drawable.artist_24px)
        Type.Designer -> painterResource(R.drawable.designer_24px)
        Type.Publisher -> painterResource(R.drawable.publisher_24px)
    }
}
