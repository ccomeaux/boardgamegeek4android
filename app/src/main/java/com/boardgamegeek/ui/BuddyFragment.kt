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
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentBuddyBinding
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.dialog.RenamePlayerDialogFragment
import com.boardgamegeek.ui.dialog.UpdateBuddyNicknameDialogFragment
import com.boardgamegeek.ui.viewmodel.BuddyViewModel
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.ctx

class BuddyFragment : Fragment() {
    private var _binding: FragmentBuddyBinding? = null
    private val binding get() = _binding!!
    private var buddyName: String? = null
    private var playerName: String? = null
    private var defaultTextColor: Int = 0
    private var lightTextColor: Int = 0

    private val viewModel: BuddyViewModel by lazy {
        ViewModelProvider(this).get(BuddyViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buddyName = arguments?.getString(KEY_BUDDY_NAME) ?: ""
        playerName = arguments?.getString(KEY_PLAYER_NAME) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBuddyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        binding.swipeRefresh.isEnabled = false
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.swipeRefresh.setBggColors()

        defaultTextColor = binding.nicknameView.textColors.defaultColor
        lightTextColor = ContextCompat.getColor(requireContext(), R.color.secondary_text)

        viewModel.buddy.observe(this, Observer {
            binding.swipeRefresh.post { binding.swipeRefresh.isRefreshing = it?.status == Status.REFRESHING }

            if (it?.data == null) {
                binding.buddyInfoView.isGone = true

                binding.nicknameView.setTextColor(defaultTextColor)
                binding.nicknameView.text = playerName
                binding.nicknameView.setOnClickListener {
                    val nickname = binding.nicknameView.text.toString()
                    act.showAndSurvive(RenamePlayerDialogFragment.newInstance(nickname))
                }

                binding.collectionCard.isGone = true
                binding.updatedView.isGone = true

                binding.swipeRefresh.isEnabled = false
            } else {
                binding.buddyInfoView.isVisible = true
                binding.avatarView.loadThumbnail(it.data.avatarUrl, R.drawable.person_image_empty)
                binding.fullNameView.text = it.data.fullName
                binding.usernameView.text = buddyName

                if (it.data.playNickname.isBlank()) {
                    binding.nicknameView.setTextColor(lightTextColor)
                    binding.nicknameView.text = it.data.firstName
                } else {
                    binding.nicknameView.setTextColor(defaultTextColor)
                    binding.nicknameView.text = it.data.playNickname
                }
                binding.nicknameView.setOnClickListener {
                    val nickname = binding.nicknameView.text.toString()
                    act.showAndSurvive(UpdateBuddyNicknameDialogFragment.newInstance(nickname))
                }

                binding.collectionCard.isVisible = true
                binding.collectionRoot.setOnClickListener {
                    BuddyCollectionActivity.start(ctx, buddyName)
                }

                binding.updatedView.timestamp = it.data.updatedTimestamp
                binding.updatedView.isVisible = true

                binding.swipeRefresh.isEnabled = true
            }
        })

        viewModel.player.observe(this, Observer { player ->
            val playCount = player?.playCount ?: 0
            val winCount = player?.winCount ?: 0
            if (playCount > 0 || winCount > 0) {
                binding.playsView.text = ctx.getQuantityText(R.plurals.winnable_plays_suffix, playCount, playCount)
                binding.winsView.text = ctx.getQuantityText(R.plurals.wins_suffix, winCount, winCount)
                binding.winPercentageView.text = getString(R.string.percentage, (winCount.toDouble() / playCount * 100).toInt())
                binding.playsCard.isVisible = true
            } else {
                binding.playsCard.isGone = true
            }
            binding.playsRoot.setOnClickListener {
                if (buddyName.isNullOrBlank()) {
                    PlayerPlaysActivity.start(ctx, playerName)
                } else {
                    BuddyPlaysActivity.start(ctx, buddyName)
                }
            }
        })

        viewModel.colors.observe(this, Observer { colors ->
            binding.colorContainer.removeAllViews()
            binding.colorContainer.isVisible = (colors?.size ?: 0) > 0
            colors?.take(3)?.forEach { color ->
                requireContext().createSmallCircle().also { view ->
                    view.setColorViewValue(color.rgb)
                    binding.colorContainer.addView(view)
                }
            }
            binding.colorsRoot.setOnClickListener {
                PlayerColorsActivity.start(ctx, buddyName, playerName)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
