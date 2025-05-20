package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentPlayStatsBinding
import com.boardgamegeek.model.HIndex
import com.boardgamegeek.model.PlayStats
import com.boardgamegeek.model.PlayerStats
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.ui.dialog.PlayStatsIncludeSettingsDialogFragment
import com.boardgamegeek.ui.viewmodel.PlayStatsViewModel
import com.boardgamegeek.ui.widget.PlayStatRow
import com.boardgamegeek.work.SyncCollectionWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class PlayStatsFragment : Fragment() {
    private var _binding: FragmentPlayStatsBinding? = null
    private val binding get() = _binding!!
    private var isOwnedSynced: Boolean = false
    private var isPlayedSynced: Boolean = false
    private var includeIncompletePlays = false
    private var includeExpansions = false
    private var includeAccessories = false
    private val viewModel by activityViewModels<PlayStatsViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPlayStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.collectionStatusSettingsButton.setOnClickListener {
            val prefs = requireContext().preferences()
            requireActivity().createThemedBuilder()
                .setTitle(R.string.title_modify_collection_status)
                .setMessage(R.string.msg_modify_collection_status)
                .setPositiveButton(R.string.modify) { _, _ ->
                    prefs.addSyncStatus(CollectionStatus.Own)
                    prefs.addSyncStatus(CollectionStatus.Played)
                    SyncCollectionWorker.requestSync(requireContext())
                    bindCollectionStatusMessage()
                }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
                .show()
        }

        binding.includeSettingsButton.setOnClickListener {
            showAndSurvive(PlayStatsIncludeSettingsDialogFragment())
        }

        viewModel.includeIncomplete.observe(viewLifecycleOwner) {
            includeIncompletePlays = it ?: false
            bindAccuracyMessage()
        }
        viewModel.includeExpansions.observe(viewLifecycleOwner) {
            includeExpansions = it ?: false
            bindAccuracyMessage()
        }
        viewModel.includeAccessories.observe(viewLifecycleOwner) {
            includeAccessories = it ?: false
            bindAccuracyMessage()
        }

        viewModel.plays.observe(viewLifecycleOwner) { playStats ->
            if (playStats == null) {
                binding.progressView.hide()
                binding.emptyView.isVisible = true
                binding.scrollContainer.isVisible = false
            } else {
                bindUi(playStats)
                binding.gameHIndexInfoView.setOnClickListener {
                    context?.showClickableAlertDialog(
                        R.string.play_stat_game_h_index,
                        R.string.play_stat_game_h_index_info,
                        playStats.hIndex.h,
                        playStats.hIndex.n
                    )
                }
            }
        }
        viewModel.players.observe(viewLifecycleOwner, Observer { playerStats ->
            if (playerStats == null) return@Observer
            bindPlayerUi(playerStats)
            binding.playerHIndexInfoView.setOnClickListener {
                context?.showClickableAlertDialog(
                    R.string.play_stat_player_h_index,
                    R.string.play_stat_player_h_index_info,
                    playerStats.hIndex.h,
                    playerStats.hIndex.n
                )
            }
        })

        bindCollectionStatusMessage()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindCollectionStatusMessage() {
        val prefs = requireContext().preferences()
        isOwnedSynced = prefs.isStatusSetToSync(CollectionStatus.Own)
        isPlayedSynced = prefs.isStatusSetToSync(CollectionStatus.Played)
        binding.collectionStatusContainer.isVisible = !isOwnedSynced || !isPlayedSynced
    }

    private fun bindAccuracyMessage() {
        val messages = ArrayList<String>(3)
        if (!includeIncompletePlays) {
            messages.add(getString(R.string.incomplete_plays).lowercase(Locale.getDefault()))
        }
        if (!includeExpansions) {
            messages.add(getString(R.string.expansions).lowercase(Locale.getDefault()))
        }
        if (!includeAccessories) {
            messages.add(getString(R.string.accessories).lowercase(Locale.getDefault()))
        }
        if (messages.isEmpty()) {
            binding.accuracyContainer.visibility = View.GONE
        } else {
            binding.accuracyContainer.visibility = View.VISIBLE
            binding.accuracyMessage.text = getString(
                R.string.play_stat_accuracy, messages.formatList(
                    getString(R.string.or).lowercase(
                        Locale.getDefault()
                    )
                )
            )
        }
    }

    private fun bindUi(stats: PlayStats) {
        binding.playCountTable.removeAllViews()
        maybeAddPlayCountStat(R.string.play_stat_play_count, stats.numberOfPlays)
        maybeAddPlayCountStat(R.string.play_stat_distinct_games, stats.numberOfPlayedGames)
        maybeAddPlayCountStat(R.string.play_stat_dollars, stats.numberOfDollars)
        maybeAddPlayCountStat(R.string.play_stat_half_dollars, stats.numberOfHalfDollars)
        maybeAddPlayCountStat(R.string.play_stat_quarters, stats.numberOfQuarters)
        maybeAddPlayCountStat(R.string.play_stat_dimes, stats.numberOfDimes)
        maybeAddPlayCountStat(R.string.play_stat_nickels, stats.numberOfNickels)

        if (isPlayedSynced) {
            PlayStatRow(requireContext()).apply {
                binding.playCountTable.addView(this)
                setLabel(R.string.play_stat_top_100)
                setValue("${stats.top100Count}%")
            }
        }

        binding.gameHIndexView.text = stats.hIndex.description
        bindHIndexTable(binding.gameHIndexTable, stats.hIndex, stats.getHIndexGames())

        binding.advancedTable.removeAllViews()
        if (stats.gIndex.isValid()) {
            binding.advancedHeader.isVisible = true
            binding.advancedCard.isVisible = true
            PlayStatRow(requireContext()).apply {
                setLabel(R.string.g_index)
                setValue(stats.gIndex.description)
                setInfoText(R.string.play_stat_game_g_index_info)
                binding.advancedTable.addView(this)
            }
        }
        if (stats.friendless != PlayStats.INVALID_FRIENDLESS) {
            binding.advancedHeader.visibility = View.VISIBLE
            binding.advancedCard.visibility = View.VISIBLE
            PlayStatRow(requireContext()).apply {
                setLabel(R.string.play_stat_friendless)
                setValue(stats.friendless)
                setInfoText(R.string.play_stat_friendless_info)
                binding.advancedTable.addView(this)
            }
        }
        if (stats.utilization != PlayStats.INVALID_UTILIZATION) {
            binding.advancedHeader.visibility = View.VISIBLE
            binding.advancedCard.visibility = View.VISIBLE
            PlayStatRow(requireContext()).apply {
                setLabel(R.string.play_stat_utilization)
                setInfoText(R.string.play_stat_utilization_info)
                setValue(stats.utilization.asPercentage())
                binding.advancedTable.addView(this)
            }
        }
        if (stats.cfm != PlayStats.INVALID_CFM) {
            binding.advancedHeader.visibility = View.VISIBLE
            binding.advancedCard.visibility = View.VISIBLE
            PlayStatRow(requireContext()).apply {
                setLabel(R.string.play_stat_cfm)
                setInfoText(R.string.play_stat_cfm_info)
                setValue(stats.cfm)
                binding.advancedTable.addView(this)
            }
        }
        binding.progressView.hide()
        binding.emptyView.isVisible = false
        binding.scrollContainer.isVisible = true
    }

    private fun maybeAddPlayCountStat(@StringRes labelResId: Int, value: Int) {
        if (value > 0) {
            PlayStatRow(requireContext()).apply {
                binding.playCountTable.addView(this)
                setLabel(labelResId)
                setValue(value)
            }
        }
    }

    private fun bindPlayerUi(stats: PlayerStats) {
        binding.playerHIndexView.text = stats.hIndex.description
        bindHIndexTable(binding.playerHIndexTable, stats.hIndex, stats.hIndexPlayers)
        binding.playerHIndexTable.setOnClickListener {
            PlayersActivity.startByPlayCount(requireContext())
        }
    }

    private fun bindHIndexTable(table: TableLayout, hIndex: HIndex, entries: List<Pair<String, Int>>?) {
        table.removeAllViews()
        if (entries.isNullOrEmpty()) {
            table.visibility = View.GONE
        } else {
            val rankedEntries = entries.filter { pair -> pair.first.isNotBlank() && pair.second > 0 }
                .mapIndexed { index, pair -> "${pair.first} (#${index + 1})" to pair.second }

            val nextHighestHIndex = entries.findLast { it.second > hIndex.h }?.second ?: (hIndex.h + 1)
            val nextLowestHIndex = entries.find { it.second < hIndex.h }?.second ?: (hIndex.h - 1)

            val prefix = rankedEntries.filter { it.second == nextHighestHIndex && it.first.isNotBlank() }
            prefix.forEach {
                PlayStatRow(requireContext()).apply {
                    setLabel(it.first)
                    setValue(it.second)
                    table.addView(this)
                }
            }

            val list = rankedEntries.filter { it.second == hIndex.h && it.first.isNotBlank() }
            if (list.isEmpty()) {
                addDivider(table)
            } else {
                list.forEach {
                    PlayStatRow(requireContext()).apply {
                        setLabel(it.first)
                        setValue(it.second)
                        setBackgroundResource(R.color.light_blue)
                        table.addView(this)
                    }
                }
            }

            val suffix = rankedEntries.filter { it.second == nextLowestHIndex }
            suffix.forEach {
                PlayStatRow(requireContext()).apply {
                    setLabel(it.first)
                    setValue(it.second)
                    table.addView(this)
                }
            }
            table.visibility = View.VISIBLE
        }
    }

    private fun addDivider(container: ViewGroup) {
        View(context).apply {
            this.layoutParams = TableLayout.LayoutParams(0, 1)
            this.setBackgroundResource(R.color.dark_blue)
            container.addView(this)
        }
    }
}
