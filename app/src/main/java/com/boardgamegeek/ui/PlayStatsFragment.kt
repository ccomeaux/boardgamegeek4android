package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.entities.HIndexEntity
import com.boardgamegeek.entities.PlayStatsEntity
import com.boardgamegeek.entities.PlayerStatsEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.dialog.PlayStatsIncludeSettingsDialogFragment
import com.boardgamegeek.ui.viewmodel.PlayStatsViewModel
import com.boardgamegeek.ui.widget.PlayStatRow
import kotlinx.android.synthetic.main.fragment_play_stats.*
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import java.util.*

class PlayStatsFragment : Fragment(R.layout.fragment_play_stats) {
    private var isOwnedSynced: Boolean = false
    private var isPlayedSynced: Boolean = false
    private var includeIncompletePlays = false
    private var includeExpansions = false
    private var includeAccessories = false
    private val viewModel by activityViewModels<PlayStatsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collectionStatusSettingsButton.setOnClickListener {
            requireActivity().createThemedBuilder()
                    .setTitle(R.string.title_modify_collection_status)
                    .setMessage(R.string.msg_modify_collection_status)
                    .setPositiveButton(R.string.modify) { _, _ ->
                        defaultSharedPreferences.addSyncStatus(COLLECTION_STATUS_OWN)
                        defaultSharedPreferences.addSyncStatus(COLLECTION_STATUS_PLAYED)
                        SyncService.sync(context, SyncService.FLAG_SYNC_COLLECTION)
                        bindCollectionStatusMessage()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(true)
                    .show()
        }

        includeSettingsButton.setOnClickListener {
            showAndSurvive(PlayStatsIncludeSettingsDialogFragment())
        }

        viewModel.includeIncomplete.observe(viewLifecycleOwner, Observer {
            includeIncompletePlays = it ?: false
            bindAccuracyMessage()
        })
        viewModel.includeExpansions.observe(viewLifecycleOwner, Observer {
            includeExpansions = it ?: false
            bindAccuracyMessage()
        })
        viewModel.includeAccessories.observe(viewLifecycleOwner, Observer {
            includeAccessories = it ?: false
            bindAccuracyMessage()
        })

        viewModel.getPlays().observe(viewLifecycleOwner, Observer { entity ->
            if (entity == null) {
                showEmpty()
            } else {
                bindUi(entity)
                gameHIndexInfoView.setOnClickListener {
                    context?.showClickableAlertDialog(
                            R.string.play_stat_game_h_index,
                            R.string.play_stat_game_h_index_info,
                            entity.hIndex.h,
                            entity.hIndex.n)
                }
            }
        })
        viewModel.getPlayers().observe(viewLifecycleOwner, Observer { entity ->
            if (entity == null) return@Observer
            bindPlayerUi(entity)
            playerHIndexInfoView.setOnClickListener {
                context?.showClickableAlertDialog(
                        R.string.play_stat_player_h_index,
                        R.string.play_stat_player_h_index_info,
                        entity.hIndex.h,
                        entity.hIndex.n)
            }
        })

        bindCollectionStatusMessage()
    }

    private fun bindCollectionStatusMessage() {
        isOwnedSynced = defaultSharedPreferences.isStatusSetToSync(COLLECTION_STATUS_OWN)
        isPlayedSynced = defaultSharedPreferences.isStatusSetToSync(COLLECTION_STATUS_PLAYED)
        collectionStatusContainer.isVisible = !isOwnedSynced || !isPlayedSynced
    }

    private fun bindAccuracyMessage() {
        val messages = ArrayList<String>(3)
        if (!includeIncompletePlays) {
            messages.add(getString(R.string.incomplete_plays).toLowerCase(Locale.getDefault()))
        }
        if (!includeExpansions) {
            messages.add(getString(R.string.expansions).toLowerCase(Locale.getDefault()))
        }
        if (!includeAccessories) {
            messages.add(getString(R.string.accessories).toLowerCase(Locale.getDefault()))
        }
        if (messages.isEmpty()) {
            accuracyContainer.visibility = View.GONE
        } else {
            accuracyContainer.visibility = View.VISIBLE
            accuracyMessage.text = getString(R.string.play_stat_accuracy, messages.formatList(getString(R.string.or).toLowerCase(Locale.getDefault())))
        }
    }

    private fun bindUi(stats: PlayStatsEntity) {
        playCountTable.removeAllViews()
        maybeAddPlayCountStat(R.string.play_stat_play_count, stats.numberOfPlays)
        maybeAddPlayCountStat(R.string.play_stat_distinct_games, stats.numberOfPlayedGames)
        maybeAddPlayCountStat(R.string.play_stat_dollars, stats.numberOfDollars)
        maybeAddPlayCountStat(R.string.play_stat_half_dollars, stats.numberOfHalfDollars)
        maybeAddPlayCountStat(R.string.play_stat_quarters, stats.numberOfQuarters)
        maybeAddPlayCountStat(R.string.play_stat_dimes, stats.numberOfDimes)
        maybeAddPlayCountStat(R.string.play_stat_nickels, stats.numberOfNickels)

        if (isPlayedSynced) {
            PlayStatRow(requireContext()).apply {
                playCountTable.addView(this)
                setLabel(R.string.play_stat_top_100)
                setValue("${stats.top100Count}%")
            }
        }

        gameHIndexView.text = stats.hIndex.description
        bindHIndexTable(gameHIndexTable, stats.hIndex, stats.getHIndexGames())

        advancedTable.removeAllViews()
        if (stats.friendless != PlayStatsEntity.INVALID_FRIENDLESS) {
            advancedHeader.visibility = View.VISIBLE
            advancedCard.visibility = View.VISIBLE
            PlayStatRow(requireContext()).apply {
                setLabel(R.string.play_stat_friendless)
                setValue(stats.friendless)
                setInfoText(R.string.play_stat_friendless_info)
                advancedTable.addView(this)
            }
        }
        if (stats.utilization != PlayStatsEntity.INVALID_UTILIZATION) {
            advancedHeader.visibility = View.VISIBLE
            advancedCard.visibility = View.VISIBLE
            PlayStatRow(requireContext()).apply {
                setLabel(R.string.play_stat_utilization)
                setInfoText(R.string.play_stat_utilization_info)
                setValue(stats.utilization.asPercentage())
                advancedTable.addView(this)
            }
        }
        if (stats.cfm != PlayStatsEntity.INVALID_CFM) {
            advancedHeader.visibility = View.VISIBLE
            advancedCard.visibility = View.VISIBLE
            PlayStatRow(requireContext()).apply {
                setLabel(R.string.play_stat_cfm)
                setInfoText(R.string.play_stat_cfm_info)
                setValue(stats.cfm)
                advancedTable.addView(this)
            }
        }
        showData()
    }

    private fun maybeAddPlayCountStat(@StringRes labelResId: Int, value: Int) {
        if (value > 0) {
            PlayStatRow(requireContext()).apply {
                playCountTable.addView(this)
                setLabel(labelResId)
                setValue(value)
            }
        }
    }

    private fun bindPlayerUi(stats: PlayerStatsEntity) {
        playerHIndexView.text = stats.hIndex.description
        bindHIndexTable(playerHIndexTable, stats.hIndex, stats.getHIndexPlayers())
        playerHIndexTable.setOnClickListener {
            PlayersActivity.startByPlayCount(requireContext())
        }
    }

    private fun bindHIndexTable(table: TableLayout, hIndex: HIndexEntity, entries: List<Pair<String, Int>>?) {
        table.removeAllViews()
        if (entries == null || entries.isEmpty()) {
            table.visibility = View.GONE
        } else {
            val rankedEntries = entries.filter { pair -> pair.first.isNotBlank() && pair.second > 0 }.mapIndexed { index, pair -> "${pair.first} (#${index + 1})" to pair.second }

            val nextHighestHIndex = entries.findLast { it.second > hIndex.h }?.second
                    ?: hIndex.h + 1
            val nextLowestHIndex = entries.find { it.second < hIndex.h }?.second ?: hIndex.h - 1

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

    private fun showEmpty() {
        progressView.fadeOut()
        emptyView.fadeIn()
        scrollContainer.fadeOut()
    }

    private fun showData() {
        progressView.fadeOut()
        emptyView.fadeOut()
        scrollContainer.fadeIn()
    }

    private fun addDivider(container: ViewGroup) {
        View(context).apply {
            this.layoutParams = TableLayout.LayoutParams(0, 1)
            this.setBackgroundResource(R.color.dark_blue)
            container.addView(this)
        }
    }
}
