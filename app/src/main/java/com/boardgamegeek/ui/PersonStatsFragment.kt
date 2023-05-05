package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentPersonStatsBinding
import com.boardgamegeek.entities.PersonStatsEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.viewmodel.PersonViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat
import java.util.*

@AndroidEntryPoint
class PersonStatsFragment : Fragment() {
    private var _binding: FragmentPersonStatsBinding? = null
    private val binding get() = _binding!!
    private var objectDescription = ""
    private val viewModel by activityViewModels<PersonViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPersonStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.collectionStatusButton.setOnClickListener {
            val prefs = requireContext().preferences()
            requireActivity().createThemedBuilder()
                .setTitle(R.string.title_modify_collection_status)
                .setMessage(R.string.msg_modify_collection_status)
                .setPositiveButton(R.string.modify) { _, _ ->
                    prefs.addSyncStatus(COLLECTION_STATUS_PLAYED)
                    prefs.addSyncStatus(COLLECTION_STATUS_RATED)
                    SyncService.sync(context, SyncService.FLAG_SYNC_COLLECTION)
                    bindCollectionStatusMessage()
                }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
                .show()
        }

        bindCollectionStatusMessage()

        objectDescription = getString(R.string.title_person).lowercase(Locale.getDefault())
        viewModel.person.observe(viewLifecycleOwner) {
            val resourceId = when (it.type) {
                PersonViewModel.PersonType.ARTIST -> R.string.title_artist
                PersonViewModel.PersonType.DESIGNER -> R.string.title_designer
                PersonViewModel.PersonType.PUBLISHER -> R.string.title_publisher
            }
            objectDescription = getString(resourceId).lowercase(Locale.getDefault())
        }

        viewModel.stats.observe(viewLifecycleOwner) {
            when (it) {
                null -> {
                    binding.statsView.isVisible = false
                    binding.emptyMessageView.isVisible = true
                }
                else -> showData(it)
            }
            binding.progress.hide()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindCollectionStatusMessage() {
        binding.collectionStatusGroup.isVisible = !requireContext().preferences().isStatusSetToSync(COLLECTION_STATUS_RATED)
    }

    private fun showData(stats: PersonStatsEntity) {
        if (stats.averageRating > 0.0) {
            binding.averageRating.text = stats.averageRating.asBoundedRating(context, DecimalFormat("#0.0"), defaultResId = R.string.unrated)
            binding.averageRating.setTextViewBackground(stats.averageRating.toColor(BggColors.ratingColors))
            binding.averageRatingGroup.isVisible = true
        } else {
            binding.averageRatingGroup.isVisible = false
        }

        binding.whitmoreScore.text = stats.whitmoreScore.toString()
        if (stats.whitmoreScore != stats.whitmoreScoreWithExpansions) {
            binding.whitmoreScoreWithExpansions.text = stats.whitmoreScoreWithExpansions.toString()
            binding.whitmoreScoreWithExpansionsGroup.isVisible = true
        } else {
            binding.whitmoreScoreWithExpansionsGroup.isVisible = false
        }
        binding.whitmoreScoreLabel.setOnClickListener {
            context?.showClickableAlertDialog(
                R.string.whitmore_score,
                R.string.whitmore_score_info,
                objectDescription
            )
        }

        binding.playCount.text = stats.playCount.toString()
        binding.hIndex.text = stats.hIndex.description
        binding.hIndexLabel.setOnClickListener {
            context?.showClickableAlertDialogPlural(
                R.string.h_index,
                R.plurals.person_game_h_index_info,
                stats.hIndex.h,
                stats.hIndex.h,
                stats.hIndex.n
            )
        }

        binding.statsView.isVisible = true
        binding.emptyMessageView.isVisible = false
    }
}
