package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentBuddyBinding
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.dialog.RenamePlayerDialogFragment
import com.boardgamegeek.ui.dialog.UpdateBuddyNicknameDialogFragment
import com.boardgamegeek.ui.viewmodel.BuddyViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BuddyFragment : Fragment() {
    private var _binding: FragmentBuddyBinding? = null
    private val binding get() = _binding!!
    private var buddyName: String? = null
    private var playerName: String? = null
    private var defaultTextColor: Int = 0
    private var lightTextColor: Int = 0
    private val viewModel by activityViewModels<BuddyViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentBuddyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.swipeRefresh.isEnabled = false
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.swipeRefresh.setBggColors()

        defaultTextColor = binding.nicknameView.textColors.defaultColor
        lightTextColor = ContextCompat.getColor(requireContext(), R.color.secondary_text)

        viewModel.buddy.observe(viewLifecycleOwner) {
            binding.swipeRefresh.isRefreshing = it?.status == Status.REFRESHING

            if (it?.data == null) {
                binding.buddyInfoView.isVisible = false
                binding.collectionCard.isVisible = false
                binding.updatedView.isVisible = false

                binding.swipeRefresh.isEnabled = false
            } else {
                buddyName = it.data.userName
                binding.avatarView.loadImage(it.data.avatarUrl, R.drawable.person_image_empty)
                binding.fullNameView.text = it.data.fullName
                binding.usernameView.text = buddyName
                playerName = it.data.playNickname.ifBlank { it.data.firstName }
                binding.nicknameView.text = playerName
                binding.nicknameView.setOnClickListener {
                    requireActivity().showAndSurvive(UpdateBuddyNicknameDialogFragment.newInstance(playerName))
                }
                binding.buddyInfoView.isVisible = true

                binding.collectionRoot.setOnClickListener {
                    BuddyCollectionActivity.start(requireContext(), buddyName)
                }
                binding.collectionCard.isVisible = true

                binding.updatedView.timestamp = it.data.updatedTimestamp
                binding.updatedView.isVisible = true

                binding.swipeRefresh.isEnabled = true
            }
        }

        viewModel.player.observe(viewLifecycleOwner) { player ->
            if (playerName == null) {
                playerName = player?.name
                binding.nicknameView.text = playerName
                binding.nicknameView.setOnClickListener {
                    requireActivity().showAndSurvive(RenamePlayerDialogFragment.newInstance(playerName))
                }
            }

            val playCount = player?.playCount ?: 0
            val winCount = player?.winCount ?: 0
            if (playCount > 0 || winCount > 0) {
                binding.playsView.text = requireContext().getQuantityText(R.plurals.winnable_plays_suffix, playCount, playCount)
                binding.winsView.text = requireContext().getQuantityText(R.plurals.wins_suffix, winCount, winCount)
                binding.winPercentageView.text = getString(R.string.percentage, (winCount.toDouble() / playCount * 100).toInt())
                binding.playsRoot.setOnClickListener {
                    if (buddyName.isNullOrBlank()) {
                        PlayerPlaysActivity.start(requireContext(), playerName)
                    } else {
                        BuddyPlaysActivity.start(requireContext(), buddyName)
                    }
                }
                binding.playsCard.fadeIn()
            } else {
                binding.playsCard.fadeOut()
            }
        }

        viewModel.colors.observe(viewLifecycleOwner) { colors ->
            binding.colorContainer.removeAllViews()
            binding.colorContainer.isVisible = (colors?.size ?: 0) > 0
            colors?.take(3)?.forEach { color ->
                requireContext().createSmallCircle().also { view ->
                    view.setColorViewValue(color.rgb)
                    binding.colorContainer.addView(view)
                }
            }
            binding.colorsRoot.setOnClickListener {
                PlayerColorsActivity.start(requireContext(), buddyName, playerName)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
