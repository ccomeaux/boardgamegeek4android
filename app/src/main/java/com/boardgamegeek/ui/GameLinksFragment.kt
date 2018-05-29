package com.boardgamegeek.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.util.ActivityUtils
import kotlinx.android.synthetic.main.fragment_game_links.*
import org.jetbrains.anko.support.v4.act

class GameLinksFragment : Fragment() {
    @ColorInt
    private var iconColor = Color.TRANSPARENT

    private val viewModel: GameViewModel by lazy {
        ViewModelProviders.of(act).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_links, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val gameId = arguments?.getInt(KEY_GAME_ID, BggContract.INVALID_ID) ?: BggContract.INVALID_ID
        val gameName = arguments?.getString(KEY_GAME_NAME)

        if (gameId != BggContract.INVALID_ID) {
            geekbuddyAnalysisLink.setOnClickListener { ActivityUtils.linkToBgg(context, "geekbuddy/analyze/thing", gameId) }
            bggLink.setOnClickListener { ActivityUtils.linkBgg(context, gameId) }
        }
        if (!gameName.isNullOrBlank()) {
            bgPricesLink.setOnClickListener { ActivityUtils.linkBgPrices(context, gameName) }
            bgPricesUkLink.setOnClickListener { ActivityUtils.linkBgPricesUk(context, gameName) }
            amazonLink.setOnClickListener { ActivityUtils.linkAmazon(context, gameName, ActivityUtils.LINK_AMAZON_COM) }
            amazonUkLink.setOnClickListener { ActivityUtils.linkAmazon(context, gameName, ActivityUtils.LINK_AMAZON_UK) }
            amazonDeLink.setOnClickListener { ActivityUtils.linkAmazon(context, gameName, ActivityUtils.LINK_AMAZON_DE) }
            ebayLink.setOnClickListener { ActivityUtils.linkEbay(context, gameName) }
        }

        colorize(arguments?.getInt(KEY_ICON_COLOR, Color.TRANSPARENT) ?: Color.TRANSPARENT)
        viewModel.game.observe(this, Observer { game ->
            colorize(game?.data?.iconColor ?: Color.TRANSPARENT)
        })
    }

    private fun colorize(color: Int) {
        if (color != iconColor) {
            iconColor = color
            if (isAdded) {
                if (iconColor == Color.TRANSPARENT) {
                    geekbuddyAnalysisLinkIcon.clearColorFilter()
                    bggLinkIcon.clearColorFilter()
                    bgPricesLinkIcon.clearColorFilter()
                    amazonLinkIcon.clearColorFilter()
                    ebayLinkIcon.clearColorFilter()
                } else {
                    geekbuddyAnalysisLinkIcon.setColorFilter(iconColor)
                    geekbuddyAnalysisLinkIcon.clearColorFilter()
                    bggLinkIcon.setColorFilter(iconColor)
                    bgPricesLinkIcon.setColorFilter(iconColor)
                    amazonLinkIcon.setColorFilter(iconColor)
                    ebayLinkIcon.setColorFilter(iconColor)
                }
            }
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
