package com.boardgamegeek.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogGameUsersBinding
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.ui.viewmodel.GameViewModel

class GameUsersDialogFragment : DialogFragment() {
    private var _binding: DialogGameUsersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.setTitle(R.string.title_users)
        _binding = DialogGameUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val viewModel = ViewModelProvider(this).get(GameViewModel::class.java)
        viewModel.game.observe(this, Observer { gameEntityRefreshableResource ->
            gameEntityRefreshableResource?.data?.let {
                val game = gameEntityRefreshableResource.data
                colorize(game.darkColor)

                val maxUsers = game.maxUsers.toDouble()

                binding.numberOwningBar.setBar(R.string.owning_meter_text, game.numberOfUsersOwned.toDouble(), maxUsers)
                binding.numberTradingBar.setBar(R.string.trading_meter_text, game.numberOfUsersTrading.toDouble(), maxUsers)
                binding.numberWantingBar.setBar(R.string.wanting_meter_text, game.numberOfUsersWanting.toDouble(), maxUsers)
                binding.numberWishingBar.setBar(R.string.wishing_meter_text, game.numberOfUsersWishListing.toDouble(), maxUsers)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun colorize(@ColorInt color: Int) {
        listOf(binding.numberOwningBar, binding.numberTradingBar, binding.numberWantingBar, binding.numberWishingBar).forEach { it.colorize(color) }
    }

    companion object {
        fun launch(host: FragmentActivity) {
            val dialog = GameUsersDialogFragment()
            dialog.setStyle(STYLE_NORMAL, R.style.Theme_bgglight_Dialog)
            host.showAndSurvive(dialog)
        }
    }
}
