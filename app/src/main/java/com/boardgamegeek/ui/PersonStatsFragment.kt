package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentPersonStatsBinding
import com.boardgamegeek.entities.PersonStatsEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.viewmodel.PersonViewModel

class PersonStatsFragment : Fragment() {
    private var _binding: FragmentPersonStatsBinding? = null
    private val binding get() = _binding!!
    private var objectDescription = ""

    private val viewModel: PersonViewModel by lazy {
        ViewModelProvider(this).get(PersonViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPersonStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.collectionStatusButton.setOnClickListener {
            requireActivity().createThemedBuilder()
                    .setTitle(R.string.title_modify_collection_status)
                    .setMessage(R.string.msg_modify_collection_status)
                    .setPositiveButton(R.string.modify) { _, _ ->
                        context.addSyncStatus(COLLECTION_STATUS_PLAYED)
                        context.addSyncStatus(COLLECTION_STATUS_RATED)
                        SyncService.sync(context, SyncService.FLAG_SYNC_COLLECTION)
                        bindCollectionStatusMessage()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(true)
                    .show()
        }

        bindCollectionStatusMessage()

        objectDescription = getString(R.string.title_person).lowercase()
        viewModel.person.observe(viewLifecycleOwner, Observer {
            objectDescription = when (it.type) {
                PersonViewModel.PersonType.ARTIST -> getString(R.string.title_artist).lowercase()
                PersonViewModel.PersonType.DESIGNER -> getString(R.string.title_designer).lowercase()
                PersonViewModel.PersonType.PUBLISHER -> getString(R.string.title_publisher).lowercase()
            }
        })

        viewModel.stats.observe(viewLifecycleOwner, Observer {
            when (it) {
                null -> showEmpty()
                else -> showData(it)
            }
            binding.progress.hide()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindCollectionStatusMessage() {
        binding.collectionStatusGroup.isVisible = !context.isStatusSetToSync(COLLECTION_STATUS_RATED)
    }

    private fun showEmpty() {
        binding.statsView.fadeOut()
        binding.emptyMessageView.fadeIn()
    }

    private fun showData(stats: PersonStatsEntity) {
        if (stats.averageRating > 0.0) {
            binding.averageRating.text = stats.averageRating.asRating(context)
            binding.averageRating.setTextViewBackground(stats.averageRating.toColor(ratingColors))
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
                    objectDescription)
        }

        binding.playCount.text = stats.playCount.toString()
        binding.hIndex.text = stats.hIndex.description
        binding.hIndexLabel.setOnClickListener {
            context?.showClickableAlertDialogPlural(
                    R.string.h_index,
                    R.plurals.person_game_h_index_info,
                    stats.hIndex.h,
                    stats.hIndex.h,
                    stats.hIndex.n)
        }

        binding.statsView.fadeIn()
        binding.emptyMessageView.fadeOut()
    }

    companion object {
        @JvmStatic
        fun newInstance(): PersonStatsFragment {
            return PersonStatsFragment()
        }
    }
}
