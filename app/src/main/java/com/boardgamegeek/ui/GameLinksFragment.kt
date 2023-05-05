package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.databinding.FragmentGameLinksBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameLinksFragment : Fragment() {
    private var _binding: FragmentGameLinksBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GameViewModel>()

    @ColorInt
    private var iconColor = Color.TRANSPARENT

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGameLinksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.game.observe(viewLifecycleOwner) {
            it?.data?.let { game ->
                if (game.id != BggContract.INVALID_ID) {
                    binding.geekbuddyAnalysisLink.setOnClickListener { context.linkToBgg("geekbuddy/analyze/thing", game.id) }
                    binding.bggLink.setOnClickListener { context.linkBgg(game.id) }
                }
                if (game.name.isNotBlank()) {
                    binding.bgPricesLink.setOnClickListener { context.linkBgPrices(game.name) }
                    binding.bgPricesUkLink.setOnClickListener { context.linkBgPricesUk(game.name) }
                    binding.amazonLink.setOnClickListener { context.linkAmazon(game.name, LINK_AMAZON_COM) }
                    binding.amazonUkLink.setOnClickListener { context.linkAmazon(game.name, LINK_AMAZON_UK) }
                    binding.amazonDeLink.setOnClickListener { context.linkAmazon(game.name, LINK_AMAZON_DE) }
                    binding.ebayLink.setOnClickListener { context.linkEbay(game.name) }
                }
            }
        }

        viewModel.game.observe(viewLifecycleOwner) { game ->
            colorize(game?.data?.iconColor ?: Color.TRANSPARENT)
        }
    }

    private fun colorize(color: Int) {
        if (color != iconColor) {
            iconColor = color
            if (isAdded) {
                listOf(
                    binding.geekbuddyAnalysisLinkIcon,
                    binding.bggLinkIcon,
                    binding.bgPricesLinkIcon,
                    binding.amazonLinkIcon,
                    binding.ebayLinkIcon,
                ).forEach {
                    it.setOrClearColorFilter(iconColor)
                }
            }
        }
    }
}
