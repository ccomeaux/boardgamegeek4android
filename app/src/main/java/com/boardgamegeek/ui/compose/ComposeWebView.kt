package com.boardgamegeek.ui.compose

import android.webkit.WebView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ComposeWebView(
    body: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
) {
    AndroidView(
        modifier = modifier,
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

private fun WebView.setWebViewText(html: String) {
    this.loadDataWithBaseURL(null, fixInternalLinks(html), "text/html", "UTF-8", null)
}

private fun fixInternalLinks(text: String): String {
    // ensure internal, path-only links are complete with the hostname
    if (text.isEmpty()) return ""
    var fixedText = text.replace("<a\\s+href=\"/".toRegex(), "<a href=\"https://www.boardgamegeek.com/")
    fixedText = fixedText.replace("<img\\s+src=\"//".toRegex(), "<img src=\"https://")
    return fixedText
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
