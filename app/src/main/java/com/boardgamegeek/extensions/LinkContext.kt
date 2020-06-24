package com.boardgamegeek.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.HttpUtils
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import org.jetbrains.anko.toast
import timber.log.Timber

const val BOARDGAME_PATH = "boardgame"
const val LINK_AMAZON_COM = "www.amazon.com"
const val LINK_AMAZON_UK = "www.amazon.co.uk"
const val LINK_AMAZON_DE = "www.amazon.de"

private val BGG_URI = Uri.parse("https://www.boardgamegeek.com/")

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

fun Context?.linkToBgg(path: String) {
    link(createBggUri(path))
}

fun Context?.linkToBgg(path: String, id: Int) {
    link(createBggUri(path, id))
}

fun Context?.link(url: String) {
    link(Uri.parse(url))
}

private fun Context?.link(link: Uri) {
    if (this == null) return
    val intent = Intent(Intent.ACTION_VIEW, link)
    if (isIntentAvailable(intent)) {
        startActivity(intent)
        FirebaseAnalytics.getInstance(this).logEvent("link") {
            param("Uri", link.toString())
        }
    } else {
        val message = "Can't figure out how to launch $link"
        Timber.w(message)
        toast(message)
    }
}

fun createBggUri(path: String): Uri = BGG_URI.buildUpon().appendEncodedPath(path).build()

fun createBggUri(path: String, id: Int): Uri = BGG_URI.buildUpon().appendEncodedPath(path).appendPath(id.toString()).build()
