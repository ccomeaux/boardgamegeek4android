package com.boardgamegeek.extensions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.Html
import android.text.SpannedString
import android.text.TextUtils
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.persister.PlayPersister
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.util.HttpUtils
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import org.jetbrains.anko.toast
import timber.log.Timber

const val BOARDGAME_PATH = "boardgame"
const val LINK_AMAZON_COM = "www.amazon.com"
const val LINK_AMAZON_UK = "www.amazon.co.uk"
const val LINK_AMAZON_DE = "www.amazon.de"
private val BGG_URI = Uri.parse("https://www.boardgamegeek.com/")

@Suppress("DEPRECATION")
fun Context.getText(@StringRes id: Int, vararg args: Any): CharSequence {
    val encodedArgs = encodeArgs(args)
    val htmlString = String.format(Html.toHtml(SpannedString(getText(id))), *encodedArgs.toTypedArray())
    return Html.fromHtml(htmlString).trimTrailingWhitespace()
}

@Suppress("DEPRECATION")
fun Context.getQuantityText(@PluralsRes id: Int, quantity: Int, vararg args: Any?): CharSequence {
    val encodedArgs = encodeArgs(args)
    val htmlString = String.format(Html.toHtml(SpannedString(resources.getQuantityText(id, quantity))), *encodedArgs.toTypedArray())
    return Html.fromHtml(htmlString).trimTrailingWhitespace()
}

private fun encodeArgs(args: Array<out Any?>): List<Any?> {
    val encodedArgs = mutableListOf<Any?>()
    for (i in args.indices) {
        val arg = args[i]
        encodedArgs.add(if (arg is String) TextUtils.htmlEncode(arg) else arg)
    }
    return encodedArgs
}

fun Context.logQuickPlay(gameId: Int, gameName: String) {
    val play = Play(gameId, gameName)
    play.setCurrentDate()
    play.updateTimestamp = System.currentTimeMillis()
    PlayPersister(this).save(play, BggContract.INVALID_ID.toLong(), false)
    SyncService.sync(this, SyncService.FLAG_SYNC_PLAYS_UPLOAD)
}

fun Context?.linkBgg(gameId: Int) {
    if (gameId == BggContract.INVALID_ID) return
    linkToBgg(BOARDGAME_PATH, gameId)
}

fun Context?.linkBgPrices(gameName: String) {
    if (gameName.isBlank()) return
    link("http://boardgameprices.com/compare-prices-for?q=" + HttpUtils.encode(gameName))
}

fun Context?.linkBgPricesUk(gameName: String) {
    if (gameName.isBlank()) return
    link("https://boardgameprices.co.uk/item/search?search=" + HttpUtils.encode(gameName))
}

fun Context?.linkAmazon(gameName: String, domain: String) {
    if (gameName.isBlank()) return
    link(String.format("http://%s/gp/aw/s/?i=toys&keywords=%s", domain, HttpUtils.encode(gameName)))
}

fun Context?.linkEbay(gameName: String) {
    if (gameName.isBlank()) return
    link("http://m.ebay.com/sch/i.html?_sacat=233&cnm=Games&_nkw=" + HttpUtils.encode(gameName))
}

fun Context?.linkToBgg(path: String, id: Int) {
    link(createBggUri(path, id))
    Answers.getInstance().logCustom(CustomEvent("Link")
            .putCustomAttribute("Path", path))
}

fun Context?.link(url: String) {
    link(Uri.parse(url))
    Answers.getInstance().logCustom(CustomEvent("Link").putCustomAttribute("Url", url))
}

private fun Context?.link(link: Uri) {
    if (this == null) return
    val intent = Intent(Intent.ACTION_VIEW, link)
    if (isIntentAvailable(intent)) {
        startActivity(intent)
    } else {
        val message = "Can't figure out how to launch $link"
        Timber.w(message)
        toast(message)
    }
}

fun createBggUri(path: String, id: Int): Uri = BGG_URI.buildUpon().appendEncodedPath(path).appendPath(id.toString()).build()

fun Context.isIntentAvailable(intent: Intent): Boolean {
    val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return list.size > 0
}