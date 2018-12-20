package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.dialog.RenamePlayerDialogFragment
import com.boardgamegeek.ui.dialog.UpdateBuddyNicknameDialogFragment
import com.boardgamegeek.ui.viewmodel.BuddyViewModel
import com.boardgamegeek.util.ImageUtils.loadThumbnail
import kotlinx.android.synthetic.main.fragment_buddy.*
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.ctx

class BuddyFragment : Fragment() {
    private var buddyName: String? = null
    private var playerName: String? = null
    private var defaultTextColor: Int = 0
    private var lightTextColor: Int = 0

    private val viewModel: BuddyViewModel by lazy {
        ViewModelProviders.of(act).get(BuddyViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buddyName = arguments?.getString(KEY_BUDDY_NAME) ?: ""
        playerName = arguments?.getString(KEY_PLAYER_NAME) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_buddy, container, false)
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        swipeRefresh.isEnabled = false
        swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        swipeRefresh.setBggColors()

        defaultTextColor = nicknameView.textColors.defaultColor
        lightTextColor = ContextCompat.getColor(requireContext(), R.color.secondary_text)

        viewModel.buddy.observe(this, Observer {
            swipeRefresh?.post { swipeRefresh?.isRefreshing = it?.status == Status.REFRESHING }

            if (it?.data == null) {
                buddyInfoView.isGone = true

                nicknameView.setTextColor(defaultTextColor)
                nicknameView.text = playerName
                nicknameView.setOnClickListener {
                    val nickname = nicknameView.text.toString()
                    act.showAndSurvive(RenamePlayerDialogFragment.newInstance(nickname))
                }

                collectionCard.isGone = true
                updatedView.isGone = true

                swipeRefresh.isEnabled = false
            } else {
                buddyInfoView.isVisible = true
                avatarView.loadThumbnail(it.data.avatarUrl, R.drawable.person_image_empty)
                fullNameView.text = it.data.fullName
                usernameView.text = buddyName

                if (it.data.playNickname.isBlank()) {
                    nicknameView.setTextColor(lightTextColor)
                    nicknameView.text = it.data.firstName
                } else {
                    nicknameView.setTextColor(defaultTextColor)
                    nicknameView.text = it.data.playNickname
                }
                nicknameView.setOnClickListener {
                    val nickname = nicknameView.text.toString()
                    act.showAndSurvive(UpdateBuddyNicknameDialogFragment.newInstance(nickname))
                }

                collectionCard.isVisible = true
                collectionRoot.setOnClickListener {
                    BuddyCollectionActivity.start(ctx, buddyName)
                }

                updatedView.timestamp = it.data.updatedTimestamp
                updatedView.isVisible = true

                swipeRefresh.isEnabled = true
            }
        })

        viewModel.player.observe(this, Observer { player ->
            val playCount = player?.playCount ?: 0
            val winCount = player?.winCount ?: 0
            if (playCount > 0 || winCount > 0) {
                playsView.text = ctx.getQuantityText(R.plurals.winnable_plays_suffix, playCount, playCount)
                winsView.text = ctx.getQuantityText(R.plurals.wins_suffix, winCount, winCount)
                winPercentageView.text = getString(R.string.percentage, (winCount.toDouble() / playCount * 100).toInt())
                playsCard.isVisible = true
            } else {
                playsCard.isGone = true
            }
            playsRoot.setOnClickListener {
                if (buddyName.isNullOrBlank()) {
                    PlayerPlaysActivity.start(ctx, playerName)
                } else {
                    BuddyPlaysActivity.start(ctx, buddyName)
                }
            }
        })

        viewModel.colors.observe(this, Observer { colors ->
            colorContainer.removeAllViews()
            colorContainer.isVisible = (colors?.size ?: 0) > 0
            colors?.take(3)?.forEach { color ->
                requireContext().createSmallCircle().also { view ->
                    view.setColorViewValue(color.rgb)
                    colorContainer.addView(view)
                }
            }
            colorsRoot.setOnClickListener {
                PlayerColorsActivity.start(ctx, buddyName, playerName)
            }
        })
    }

    companion object {
        private const val KEY_BUDDY_NAME = "BUDDY_NAME"
        private const val KEY_PLAYER_NAME = "PLAYER_NAME"

        fun newInstance(username: String?, playerName: String?): BuddyFragment {
            return BuddyFragment().apply {
                arguments = bundleOf(
                        KEY_BUDDY_NAME to username,
                        KEY_PLAYER_NAME to playerName)
            }
        }
    }
}
