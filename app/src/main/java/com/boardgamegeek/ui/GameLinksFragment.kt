package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameViewModel
import kotlinx.android.synthetic.main.fragment_game_links.*

class GameLinksFragment : Fragment(R.layout.fragment_game_links) {
    @ColorInt
    private var iconColor = Color.TRANSPARENT

    private val viewModel by activityViewModels<GameViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.game.observe(viewLifecycleOwner, Observer {
            it.data?.let { game ->
                if (game.id != BggContract.INVALID_ID) {
                    geekbuddyAnalysisLink.setOnClickListener { context.linkToBgg("geekbuddy/analyze/thing", game.id) }
                    bggLink.setOnClickListener { context.linkBgg(game.id) }
                }
                if (game.name.isNotBlank()) {
                    bgPricesLink.setOnClickListener { context.linkBgPrices(game.name) }
                    bgPricesUkLink.setOnClickListener { context.linkBgPricesUk(game.name) }
                    amazonLink.setOnClickListener { context.linkAmazon(game.name, LINK_AMAZON_COM) }
                    amazonUkLink.setOnClickListener { context.linkAmazon(game.name, LINK_AMAZON_UK) }
                    amazonDeLink.setOnClickListener { context.linkAmazon(game.name, LINK_AMAZON_DE) }
                    ebayLink.setOnClickListener { context.linkEbay(game.name) }
                }
            }
        })

        viewModel.game.observe(viewLifecycleOwner, Observer { game ->
            colorize(game?.data?.iconColor ?: Color.TRANSPARENT)
        })
    }

    private fun colorize(color: Int) {
        if (color != iconColor) {
            iconColor = color
            if (isAdded) {
                val icons = listOf(geekbuddyAnalysisLinkIcon, bggLinkIcon, bgPricesLinkIcon, amazonLinkIcon, ebayLinkIcon)
                if (iconColor == Color.TRANSPARENT) {
                    icons.forEach { it.clearColorFilter() }
                } else {
                    icons.forEach { it.setColorFilter(iconColor) }
                }
            }
        }
    }
}
