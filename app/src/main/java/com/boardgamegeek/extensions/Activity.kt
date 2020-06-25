package com.boardgamegeek.extensions

import android.app.Activity
import android.util.Pair
import androidx.annotation.StringRes
import androidx.core.app.ShareCompat
import com.boardgamegeek.R
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import java.util.*

fun Activity.shareGame(gameId: Int, gameName: String, method: String, firebaseAnalytics: FirebaseAnalytics) {
    val subject = String.format(resources.getString(R.string.share_game_subject), gameName)
    val text = "${resources.getString(R.string.share_game_text)}\n\n${formatGameLink(gameId, gameName)}"
    share(subject, text, R.string.title_share_game)

    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
        param(FirebaseAnalytics.Param.METHOD, method)
        param(FirebaseAnalytics.Param.ITEM_ID, gameId.toString())
        param(FirebaseAnalytics.Param.ITEM_NAME, gameName)
        param(FirebaseAnalytics.Param.CONTENT_TYPE, "Game")
    }
}

fun Activity.shareGames(games: List<Pair<Int, String>>, method: String, firebaseAnalytics: FirebaseAnalytics) {
    val text = StringBuilder(resources.getString(R.string.share_games_text))
    text.append("\n").append("\n")
    val gameNames = ArrayList<String>()
    val gameIds = ArrayList<Int>()
    for (game in games) {
        text.append(formatGameLink(game.first, game.second))
        gameNames.add(game.second)
        gameIds.add(game.first)
    }
    share(resources.getString(R.string.share_games_subject), text.toString(), R.string.title_share_games)
    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
        param(FirebaseAnalytics.Param.METHOD, method)
        param(FirebaseAnalytics.Param.ITEM_ID, gameIds.formatList())
        param(FirebaseAnalytics.Param.ITEM_NAME, gameNames.formatList())
        param(FirebaseAnalytics.Param.CONTENT_TYPE, "Games")
    }
}

fun Activity.share(subject: String, text: CharSequence, @StringRes titleResId: Int = R.string.title_share) {
    val intent = ShareCompat.IntentBuilder.from(this)
            .setType("text/plain")
            .setSubject(subject.trim { it <= ' ' })
            .setText(text)
            .setChooserTitle(titleResId)
            .createChooserIntent()
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    }
}

fun formatGameLink(id: Int, name: String) = "$name (${createBggUri(BOARDGAME_PATH, id)})\n"


