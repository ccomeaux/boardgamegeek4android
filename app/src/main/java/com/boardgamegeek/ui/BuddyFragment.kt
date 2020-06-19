package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.dialog.RenamePlayerDialogFragment
import com.boardgamegeek.ui.dialog.UpdateBuddyNicknameDialogFragment
import com.boardgamegeek.ui.viewmodel.BuddyViewModel
import kotlinx.android.synthetic.main.fragment_buddy.*

class BuddyFragment : Fragment(R.layout.fragment_buddy) {
    private var buddyName: String? = null
    private var playerName: String? = null
    private var defaultTextColor: Int = 0
    private var lightTextColor: Int = 0

    private val viewModel by activityViewModels<BuddyViewModel>()

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        swipeRefresh.isEnabled = false
        swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        swipeRefresh.setBggColors()

        defaultTextColor = nicknameView.textColors.defaultColor
        lightTextColor = ContextCompat.getColor(requireContext(), R.color.secondary_text)

        viewModel.buddy.observe(viewLifecycleOwner, Observer {
            swipeRefresh.isRefreshing = it?.status == Status.REFRESHING

            if (it?.data == null) {
                buddyInfoView.fadeOut()
                collectionCard.fadeOut()
                updatedView.fadeOut()

                swipeRefresh.isEnabled = false
            } else {
                buddyName = it.data.userName
                avatarView.loadThumbnail(it.data.avatarUrl, R.drawable.person_image_empty)
                fullNameView.text = it.data.fullName
                usernameView.text = buddyName
                playerName = if (it.data.playNickname.isBlank()) {
                    it.data.firstName
                } else {
                    it.data.playNickname
                }
                nicknameView.text = playerName
                nicknameView.setOnClickListener {
                    requireActivity().showAndSurvive(UpdateBuddyNicknameDialogFragment.newInstance(playerName!!))
                }
                buddyInfoView.fadeIn()

                collectionRoot.setOnClickListener {
                    BuddyCollectionActivity.start(requireContext(), buddyName)
                }
                collectionCard.fadeIn()

                updatedView.timestamp = it.data.updatedTimestamp
                updatedView.fadeIn()

                swipeRefresh.isEnabled = true
            }
        })

        viewModel.player.observe(viewLifecycleOwner, Observer { player ->
            if (playerName == null) {
                playerName = player.name
                nicknameView.text = playerName
                nicknameView.setOnClickListener {
                    requireActivity().showAndSurvive(RenamePlayerDialogFragment.newInstance(playerName!!))
                }
            }

            val playCount = player?.playCount ?: 0
            val winCount = player?.winCount ?: 0
            if (playCount > 0 || winCount > 0) {
                playsView.text = requireContext().getQuantityText(R.plurals.winnable_plays_suffix, playCount, playCount)
                winsView.text = requireContext().getQuantityText(R.plurals.wins_suffix, winCount, winCount)
                winPercentageView.text = getString(R.string.percentage, (winCount.toDouble() / playCount * 100).toInt())
                playsRoot.setOnClickListener {
                    if (buddyName.isNullOrBlank()) {
                        PlayerPlaysActivity.start(requireContext(), playerName)
                    } else {
                        BuddyPlaysActivity.start(requireContext(), buddyName)
                    }
                }
                playsCard.fadeIn()
            } else {
                playsCard.fadeOut()
            }
        })

        viewModel.colors.observe(viewLifecycleOwner, Observer { colors ->
            colorContainer.removeAllViews()
            colorContainer.isVisible = (colors?.size ?: 0) > 0
            colors?.take(3)?.forEach { color ->
                requireContext().createSmallCircle().also { view ->
                    view.setColorViewValue(color.rgb)
                    colorContainer.addView(view)
                }
            }
            colorsRoot.setOnClickListener {
                PlayerColorsActivity.start(requireContext(), buddyName, playerName)
            }
        })
    }
}
