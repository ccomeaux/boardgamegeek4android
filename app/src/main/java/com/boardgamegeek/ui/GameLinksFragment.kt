package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGameLinksBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameViewModel

class GameLinksFragment : Fragment() {
    private var _binding: FragmentGameLinksBinding? = null
    private val binding get() = _binding!!
    
    @ColorInt
    private var iconColor = Color.TRANSPARENT

    private val viewModel: GameViewModel by lazy {
        ViewModelProvider(requireActivity()).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGameLinksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.game.observe(this, Observer {
            it.data?.let { game ->
                if (game.id != BggContract.INVALID_ID) {
                    binding.geekbuddyAnalysisLink.setOnClickListener { context.linkToBgg("geekbuddy/analyze/thing", game.id) }
                    binding.bggLink.setOnClickListener { context.linkBgg(game.id) }
                }
                if ( game.name.isNotBlank()) {
                    binding.bgPricesLink.setOnClickListener { context.linkBgPrices(game.name) }
                    binding.bgPricesUkLink.setOnClickListener { context.linkBgPricesUk(game.name) }
                    binding.amazonLink.setOnClickListener { context.linkAmazon(game.name, LINK_AMAZON_COM) }
                    binding.amazonUkLink.setOnClickListener { context.linkAmazon(game.name, LINK_AMAZON_UK) }
                    binding.amazonDeLink.setOnClickListener { context.linkAmazon(game.name, LINK_AMAZON_DE) }
                    binding.ebayLink.setOnClickListener { context.linkEbay(game.name) }
                }
            }
        })

        viewModel.game.observe(this, Observer { game ->
            colorize(game?.data?.iconColor ?: Color.TRANSPARENT)
        })
    }

    private fun colorize(color: Int) {
        if (color != iconColor) {
            iconColor = color
            if (isAdded) {
                val icons = listOf(binding.geekbuddyAnalysisLinkIcon, binding.bggLinkIcon, binding.bgPricesLinkIcon, binding.amazonLinkIcon, binding.ebayLinkIcon)
                if (iconColor == Color.TRANSPARENT) {
                    icons.forEach { it.clearColorFilter() }
                } else {
                    icons.forEach { it.setColorFilter(iconColor) }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(): GameLinksFragment {
            return GameLinksFragment()
        }
    }
}
