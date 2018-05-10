package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.ActivityUtils
import kotlinx.android.synthetic.main.fragment_game_links.*

class GameLinksFragment : Fragment() {
    private var gameId: Int = 0
    private var gameName: String? = null
    @ColorInt
    private var iconColor = Color.TRANSPARENT

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_links, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        readBundle(arguments)

        colorize()

        geekbuddyAnalysisLink.setOnClickListener { ActivityUtils.linkToBgg(context, "geekbuddy/analyze/thing", gameId) }
        bggLink.setOnClickListener { ActivityUtils.linkBgg(context, gameId) }
        bgPricesLink.setOnClickListener { ActivityUtils.linkBgPrices(context, gameName) }
        bgPricesUkLink.setOnClickListener { ActivityUtils.linkBgPricesUk(context, gameName) }
        amazonLink.setOnClickListener { ActivityUtils.linkAmazon(context, gameName, ActivityUtils.LINK_AMAZON_COM) }
        amazonUkLink.setOnClickListener { ActivityUtils.linkAmazon(context, gameName, ActivityUtils.LINK_AMAZON_UK) }
        amazonDeLink.setOnClickListener { ActivityUtils.linkAmazon(context, gameName, ActivityUtils.LINK_AMAZON_DE) }
        ebayLink.setOnClickListener { ActivityUtils.linkEbay(context, gameName) }
    }

    private fun readBundle(bundle: Bundle?) {
        if (bundle == null) return
        gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = bundle.getString(KEY_GAME_NAME)
        iconColor = bundle.getInt(KEY_ICON_COLOR, Color.TRANSPARENT)
    }

    private fun colorize() {
        if (isAdded && iconColor != Color.TRANSPARENT) {
            geekbuddyAnalysisLinkIcon.setColorFilter(iconColor)
            bggLinkIcon.setColorFilter(iconColor)
            bgPricesLinkIcon.setColorFilter(iconColor)
            amazonLinkIcon.setColorFilter(iconColor)
            ebayLinkIcon.setColorFilter(iconColor)
        }
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_ICON_COLOR = "ICON_COLOR"

        @JvmStatic
        fun newInstance(gameId: Int, gameName: String, @ColorInt iconColor: Int): GameLinksFragment {
            val args = Bundle()
            args.putInt(KEY_GAME_ID, gameId)
            args.putString(KEY_GAME_NAME, gameName)
            args.putInt(KEY_ICON_COLOR, iconColor)
            val fragment = GameLinksFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
