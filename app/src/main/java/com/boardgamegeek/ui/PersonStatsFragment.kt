package com.boardgamegeek.ui

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.entities.PersonStatsEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.viewmodel.PersonViewModel
import kotlinx.android.synthetic.main.fragment_person_stats.*
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import java.util.*

class PersonStatsFragment : Fragment(R.layout.fragment_person_stats) {
    private var objectDescription = ""

    private val viewModel by activityViewModels<PersonViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        collectionStatusButton.setOnClickListener {
            requireActivity().createThemedBuilder()
                    .setTitle(R.string.title_modify_collection_status)
                    .setMessage(R.string.msg_modify_collection_status)
                    .setPositiveButton(R.string.modify) { _, _ ->
                        defaultSharedPreferences.addSyncStatus(COLLECTION_STATUS_PLAYED)
                        defaultSharedPreferences.addSyncStatus(COLLECTION_STATUS_RATED)
                        SyncService.sync(context, SyncService.FLAG_SYNC_COLLECTION)
                        bindCollectionStatusMessage()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(true)
                    .show()
        }

        bindCollectionStatusMessage()

        objectDescription = getString(R.string.title_person).toLowerCase(Locale.getDefault())
        viewModel.person.observe(viewLifecycleOwner, Observer {
            val resourceId = when (it.type) {
                PersonViewModel.PersonType.ARTIST -> R.string.title_artist
                PersonViewModel.PersonType.DESIGNER -> R.string.title_designer
                PersonViewModel.PersonType.PUBLISHER -> R.string.title_publisher
            }
            objectDescription = getString(resourceId).toLowerCase(Locale.getDefault())
        })

        viewModel.stats.observe(viewLifecycleOwner, Observer {
            when (it) {
                null -> showEmpty()
                else -> showData(it)
            }
            progress.hide()
        })
    }

    private fun bindCollectionStatusMessage() {
        collectionStatusGroup.isVisible = !defaultSharedPreferences.isStatusSetToSync(COLLECTION_STATUS_RATED)
    }

    private fun showEmpty() {
        statsView.fadeOut()
        emptyMessageView.fadeIn()
    }

    private fun showData(stats: PersonStatsEntity) {
        if (stats.averageRating > 0.0) {
            averageRating.text = stats.averageRating.asRating(context)
            averageRating.setTextViewBackground(stats.averageRating.toColor(ratingColors))
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
                    objectDescription)
        }

        playCount.text = stats.playCount.toString()
        hIndex.text = stats.hIndex.description
        hIndexLabel.setOnClickListener {
            context?.showClickableAlertDialogPlural(
                    R.string.h_index,
                    R.plurals.person_game_h_index_info,
                    stats.hIndex.h,
                    stats.hIndex.h,
                    stats.hIndex.n)
        }

        statsView.fadeIn()
        emptyMessageView.fadeOut()
    }
}
