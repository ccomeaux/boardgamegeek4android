package com.boardgamegeek.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.ui.viewmodel.GameViewModel
import kotlinx.android.synthetic.main.dialog_game_users.*

class GameUsersDialogFragment : DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.setTitle(R.string.title_users)
        return inflater.inflate(R.layout.dialog_game_users, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val viewModel by activityViewModels<GameViewModel>()
        viewModel.game.observe(this, Observer { gameEntityRefreshableResource ->
            gameEntityRefreshableResource?.data?.let {
                val game = gameEntityRefreshableResource.data
                colorize(game.darkColor)

                val maxUsers = game.maxUsers.toDouble()

                numberOwningBar.setBar(R.string.owning_meter_text, game.numberOfUsersOwned.toDouble(), maxUsers)
                numberTradingBar.setBar(R.string.trading_meter_text, game.numberOfUsersTrading.toDouble(), maxUsers)
                numberWantingBar.setBar(R.string.wanting_meter_text, game.numberOfUsersWanting.toDouble(), maxUsers)
                numberWishingBar.setBar(R.string.wishing_meter_text, game.numberOfUsersWishListing.toDouble(), maxUsers)
            }
        })
    }

    private fun colorize(@ColorInt color: Int) {
        listOf(numberOwningBar, numberTradingBar, numberWantingBar, numberWishingBar).forEach { it?.colorize(color) }
    }

    companion object {
        fun launch(host: FragmentActivity) {
            val dialog = GameUsersDialogFragment()
            dialog.setStyle(STYLE_NORMAL, R.style.Theme_bgglight_Dialog)
            host.showAndSurvive(dialog)
        }
    }
}
