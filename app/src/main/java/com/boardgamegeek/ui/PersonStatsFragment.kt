package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.entities.PersonStatsEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.viewmodel.PersonViewModel
import kotlinx.android.synthetic.main.fragment_person_stats.*
import java.util.*

class PersonStatsFragment : Fragment(R.layout.fragment_person_stats) {
    private var objectDescription = ""

    private val viewModel by activityViewModels<PersonViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collectionStatusButton.setOnClickListener {
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
        viewModel.person.observe(viewLifecycleOwner, {
            val resourceId = when (it.type) {
                PersonViewModel.PersonType.ARTIST -> R.string.title_artist
                PersonViewModel.PersonType.DESIGNER -> R.string.title_designer
                PersonViewModel.PersonType.PUBLISHER -> R.string.title_publisher
            }
            objectDescription = getString(resourceId).lowercase(Locale.getDefault())
        })

        viewModel.stats.observe(viewLifecycleOwner, {
            when (it) {
                null -> showEmpty()
                else -> showData(it)
            }
            progress.hide()
        })
    }

    private fun bindCollectionStatusMessage() {
        collectionStatusGroup.isVisible = !requireContext().preferences().isStatusSetToSync(COLLECTION_STATUS_RATED)
    }

    private fun showEmpty() {
        statsView.fadeOut()
        emptyMessageView.fadeIn()
    }

    private fun showData(stats: PersonStatsEntity) {
        if (stats.averageRating > 0.0) {
            averageRating.text = stats.averageRating.asRating(context)
            averageRating.setTextViewBackground(stats.averageRating.toColor(BggColors.ratingColors))
            averageRatingGroup.isVisible = true
        } else {
            averageRatingGroup.isVisible = false
        }

        whitmoreScore.text = stats.whitmoreScore.toString()
        if (stats.whitmoreScore != stats.whitmoreScoreWithExpansions) {
            whitmoreScoreWithExpansions.text = stats.whitmoreScoreWithExpansions.toString()
            whitmoreScoreWithExpansionsGroup.isVisible = true
        } else {
            whitmoreScoreWithExpansionsGroup.isVisible = false
        }
        whitmoreScoreLabel.setOnClickListener {
            context?.showClickableAlertDialog(
                R.string.whitmore_score,
                R.string.whitmore_score_info,
                objectDescription
            )
        }

        playCount.text = stats.playCount.toString()
        hIndex.text = stats.hIndex.description
        hIndexLabel.setOnClickListener {
            context?.showClickableAlertDialogPlural(
                R.string.h_index,
                R.plurals.person_game_h_index_info,
                stats.hIndex.h,
                stats.hIndex.h,
                stats.hIndex.n
            )
        }

        statsView.fadeIn()
        emptyMessageView.fadeOut()
    }
}
