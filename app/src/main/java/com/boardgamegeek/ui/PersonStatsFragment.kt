package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.entities.PersonStatsEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.viewmodel.PersonViewModel
import com.boardgamegeek.util.PreferencesUtils
import kotlinx.android.synthetic.main.fragment_person_stats.*

class PersonStatsFragment : Fragment() {
    private var objectDescription = ""

    private val viewModel: PersonViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(PersonViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_person_stats, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        collectionStatusButton.setOnClickListener {
            requireActivity().createThemedBuilder()
                    .setTitle(R.string.title_modify_collection_status)
                    .setMessage(R.string.msg_modify_collection_status)
                    .setPositiveButton(R.string.modify) { _, _ ->
                        PreferencesUtils.addSyncStatus(context, BggService.COLLECTION_QUERY_STATUS_PLAYED)
                        PreferencesUtils.addSyncStatus(context, BggService.COLLECTION_QUERY_STATUS_RATED)
                        SyncService.sync(context, SyncService.FLAG_SYNC_COLLECTION)
                        bindCollectionStatusMessage()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(true)
                    .show()
        }

        bindCollectionStatusMessage()

        objectDescription = getString(R.string.title_person).toLowerCase()
        viewModel.person.observe(this, Observer {
            objectDescription = when (it.type) {
                PersonViewModel.PersonType.ARTIST -> getString(R.string.title_artist).toLowerCase()
                PersonViewModel.PersonType.DESIGNER -> getString(R.string.title_designer).toLowerCase()
                PersonViewModel.PersonType.PUBLISHER -> getString(R.string.title_publisher).toLowerCase()
            }
        })

        viewModel.stats.observe(this, Observer {
            when (it) {
                null -> showEmpty()
                else -> showData(it)
            }
            progress.hide()
        })
    }

    private fun bindCollectionStatusMessage() {
        collectionStatusGroup.isVisible = !PreferencesUtils.isStatusSetToSync(context, BggService.COLLECTION_QUERY_STATUS_RATED)
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
                    objectDescription,
                    stats.hIndex.toString())
        }

        playCount.text = stats.playCount.toString()
        hIndex.text = stats.hIndex.toString()
        hIndexLabel.setOnClickListener {
            context?.showClickableAlertDialog(
                    R.string.h_index,
                    R.plurals.peron_game_h_index_info,
                    stats.hIndex,
                    stats.hIndex.toString())
        }

        statsView.fadeIn()
        emptyMessageView.fadeOut()
    }

    companion object {
        @JvmStatic
        fun newInstance(): PersonStatsFragment {
            return PersonStatsFragment()
        }
    }
}
