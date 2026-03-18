package com.boardgamegeek.ui.compose

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.boardgamegeek.extensions.setWebViewText


@Composable
fun ComposeWebView(
    body: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            WebView(context).apply {
                val textColorHex = textColor.toHexString()
                val htmlContent =
                    "<html><head><style type=\"text/css\">body{color: $textColorHex}</style></head><body>$body</body></html>"
                setWebViewText(htmlContent)
                setBackgroundColor(backgroundColor.toArgb())
            }
        }
    )
}

private fun Color.toHexString(includeAlpha: Boolean = false): String {
    val alpha = (this.alpha * 255).toInt()
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()

    return if (includeAlpha)
        String.format("#%02x%02x%02x%02x", alpha, red, green, blue)
    else
        String.format("#%02x%02x%02x", red, green, blue)
}
