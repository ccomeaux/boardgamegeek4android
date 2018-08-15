package com.boardgamegeek.extensions

import android.app.Activity
import android.support.annotation.StringRes
import android.support.v4.app.ShareCompat
import android.util.Pair
import com.boardgamegeek.R
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ShareEvent
import java.util.*

fun Activity.shareGame(gameId: Int, gameName: String, method: String) {
    val subject = String.format(resources.getString(R.string.share_game_subject), gameName)
    val text = "${resources.getString(R.string.share_game_text)}\n\n${formatGameLink(gameId, gameName)}"
    share(subject, text, R.string.title_share_game)
    Answers.getInstance().logShare(ShareEvent()
            .putMethod(method)
            .putContentType("Game")
            .putContentName(gameName)
            .putContentId(gameId.toString()))
}

fun Activity.shareGames(games: List<Pair<Int, String>>, method: String) {
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
    Answers.getInstance().logShare(ShareEvent()
            .putMethod(method)
            .putContentType("Games")
            .putContentName(gameNames.formatList())
            .putContentId(gameIds.formatList()))
}

fun Activity.share(subject: String, text: CharSequence, @StringRes titleResId: Int) {
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


