package com.boardgamegeek.extensions

import android.app.Activity
import android.graphics.Insets
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowInsets
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ShareCompat
import com.boardgamegeek.R
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

fun Activity.shareGame(gameId: Int, gameName: String, method: String, firebaseAnalytics: FirebaseAnalytics? = null) {
    val subject = resources.getString(R.string.share_game_subject, gameName)
    val text = "${resources.getString(R.string.share_game_text)}\n\n${formatGameLink(gameId, gameName)}"
    share(subject, text, R.string.title_share_game)

    firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SHARE) {
        param(FirebaseAnalytics.Param.METHOD, method)
        param(FirebaseAnalytics.Param.ITEM_ID, gameId.toString())
        param(FirebaseAnalytics.Param.ITEM_NAME, gameName)
        param(FirebaseAnalytics.Param.CONTENT_TYPE, "Game")
    }
}

fun Activity.shareGames(games: List<Pair<Int, String>>, method: String, firebaseAnalytics: FirebaseAnalytics? = null) {
    val text = StringBuilder(resources.getString(R.string.share_games_text))
    text.append("\n\n")
    val gameNames = arrayListOf<String>()
    val gameIds = arrayListOf<Int>()
    for (game in games) {
        text.append(formatGameLink(game.first, game.second))
        gameNames.add(game.second)
        gameIds.add(game.first)
    }
    share(resources.getString(R.string.share_games_subject), text.toString(), R.string.title_share_games)
    firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SHARE) {
        param(FirebaseAnalytics.Param.METHOD, method)
        param(FirebaseAnalytics.Param.ITEM_ID, gameIds.formatList())
        param(FirebaseAnalytics.Param.ITEM_NAME, gameNames.formatList())
        param(FirebaseAnalytics.Param.CONTENT_TYPE, "Games")
    }
}

fun Activity.share(subject: String, text: CharSequence, @StringRes titleResId: Int = R.string.title_share) {
    startActivity(
        ShareCompat.IntentBuilder(this)
            .setType("text/plain")
            .setSubject(subject.trim())
            .setText(text)
            .setChooserTitle(titleResId)
            .createChooserIntent()
    )
}

fun formatGameLink(id: Int, name: String) = "$name (${createBggUri(BOARDGAME_PATH, id)})\n"

fun AppCompatActivity.setDoneCancelActionBarView(listener: View.OnClickListener?) {
    val toolbar = findViewById<Toolbar>(R.id.toolbar_done_cancel) ?: return
    toolbar.setContentInsetsAbsolute(0, 0)
    toolbar.findViewById<View>(R.id.menu_cancel).setOnClickListener(listener)
    toolbar.findViewById<View>(R.id.menu_done).setOnClickListener(listener)
    setSupportActionBar(toolbar)
}

@Suppress("DEPRECATION")
fun Activity.calculateScreenWidth(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = windowManager.currentWindowMetrics
        val insets: Insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        windowMetrics.bounds.width() - insets.left - insets.right
    } else {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        displayMetrics.widthPixels
    }
}
