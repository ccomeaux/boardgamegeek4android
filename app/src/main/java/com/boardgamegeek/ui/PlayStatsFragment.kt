package com.boardgamegeek.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TextView
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayStatsEntity
import com.boardgamegeek.entities.PlayerStatsEntity
import com.boardgamegeek.extensions.asPercentage
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.formatList
import com.boardgamegeek.io.BggService
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.dialog.PlayStatsIncludeSettingsDialogFragment
import com.boardgamegeek.ui.viewmodel.PlayStatsViewModel
import com.boardgamegeek.ui.widget.PlayStatRow
import com.boardgamegeek.util.DialogUtils
import com.boardgamegeek.util.PreferencesUtils
import kotlinx.android.synthetic.main.fragment_play_stats.*
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.ctx
import java.util.*

class PlayStatsFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var isOwnedSynced: Boolean = false
    private var isPlayedSynced: Boolean = false
    private val viewModel: PlayStatsViewModel by lazy {
        ViewModelProviders.of(act).get(PlayStatsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_play_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collectionStatusSettingsButton.setOnClickListener {
            DialogUtils.createThemedBuilder(ctx)
                    .setTitle(R.string.play_stat_title_collection_status)
                    .setMessage(R.string.play_stat_msg_collection_status)
                    .setPositiveButton(R.string.modify) { _, _ ->
                        PreferencesUtils.addSyncStatus(ctx, BggService.COLLECTION_QUERY_STATUS_OWN)
                        PreferencesUtils.addSyncStatus(ctx, BggService.COLLECTION_QUERY_STATUS_PLAYED)
                        SyncService.sync(ctx, SyncService.FLAG_SYNC_COLLECTION)
                        bindCollectionStatusMessage()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(true)
                    .show()
        }

        includeSettingsButton.setOnClickListener {
            DialogUtils.showFragment(act, PlayStatsIncludeSettingsDialogFragment.newInstance(), "play_stats_settings_include")
        }


        viewModel.getPlays().observe(this, Observer { entity ->
            if (entity == null) {
                showEmpty()
            } else {
                bindUi(entity)
                gameHIndexInfoView.setOnClickListener {
                    showAlertDialog(R.string.play_stat_game_h_index,
                            R.string.play_stat_game_h_index_info,
                            entity.hIndex.toString())
                }
            }
        })
        viewModel.getPlayers().observe(this, Observer { entity ->
            if (entity == null) return@Observer
            bindPlayerUi(entity)
            playerHIndexInfoView.setOnClickListener {
                showAlertDialog(R.string.play_stat_player_h_index,
                        R.string.play_stat_player_h_index_info,
                        entity.hIndex.toString())
            }

        })

        bindCollectionStatusMessage()
        bindAccuracyMessage()
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(act).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(act).unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun bindCollectionStatusMessage() {
        isOwnedSynced = PreferencesUtils.isStatusSetToSync(ctx, BggService.COLLECTION_QUERY_STATUS_OWN)
        isPlayedSynced = PreferencesUtils.isStatusSetToSync(ctx, BggService.COLLECTION_QUERY_STATUS_PLAYED)
        collectionStatusContainer.visibility = if (isOwnedSynced && isPlayedSynced) View.GONE else View.VISIBLE
    }

    private fun bindAccuracyMessage() {
        val messages = ArrayList<String>(3)
        if (!PreferencesUtils.logPlayStatsIncomplete(ctx)) {
            messages.add(getString(R.string.incomplete_games).toLowerCase())
        }
        if (!PreferencesUtils.logPlayStatsExpansions(ctx)) {
            messages.add(getString(R.string.expansions).toLowerCase())
        }
        if (!PreferencesUtils.logPlayStatsAccessories(ctx)) {
            messages.add(getString(R.string.accessories).toLowerCase())
        }
        if (messages.isEmpty()) {
            accuracyContainer.visibility = View.GONE
        } else {
            accuracyContainer.visibility = View.VISIBLE
            accuracyMessage.text = getString(R.string.play_stat_accuracy, messages.formatList(getString(R.string.or).toLowerCase()))
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
            PlayStatRow(ctx).apply {
                playCountTable.addView(this)
                setLabel(R.string.play_stat_top_100)
                setValue("${stats.top100Count}%")
            }
        }

        gameHIndexView.text = stats.hIndex.toString()
        bindHIndexTable(gameHIndexTable, stats.hIndex, stats.getHIndexGames())

        advancedTable.removeAllViews()
        if (stats.friendless != PlayStatsEntity.INVALID_FRIENDLESS) {
            advancedHeader.visibility = View.VISIBLE
            advancedCard.visibility = View.VISIBLE
            PlayStatRow(ctx).apply {
                setLabel(R.string.play_stat_friendless)
                setValue(stats.friendless)
                setInfoText(R.string.play_stat_friendless_info)
                advancedTable.addView(this)
            }
        }
        if (stats.utilization != PlayStatsEntity.INVALID_UTILIZATION) {
            advancedHeader.visibility = View.VISIBLE
            advancedCard.visibility = View.VISIBLE
            PlayStatRow(ctx).apply {
                setLabel(R.string.play_stat_utilization)
                setInfoText(R.string.play_stat_utilization_info)
                setValue(stats.utilization.asPercentage())
                advancedTable.addView(this)
            }
        }
        if (stats.cfm != PlayStatsEntity.INVALID_CFM) {
            advancedHeader.visibility = View.VISIBLE
            advancedCard.visibility = View.VISIBLE
            PlayStatRow(ctx).apply {
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
            PlayStatRow(ctx).apply {
                playCountTable.addView(this)
                setLabel(labelResId)
                setValue(value)
            }
        }
    }

    private fun bindPlayerUi(stats: PlayerStatsEntity) {
        playerHIndexView.text = stats.hIndex.toString()
        bindHIndexTable(playerHIndexTable, stats.hIndex, stats.getHIndexPlayers())
    }

    private fun bindHIndexTable(table: TableLayout, hIndex: Int, entries: List<Pair<String, Int>>?) {
        table.removeAllViews()
        if (entries == null || entries.isEmpty()) {
            table.visibility = View.GONE
        } else {
            val rankedEntries = entries.mapIndexed { index, pair -> "${pair.first} (#${index + 1})" to pair.second }

            val nextHighestHIndex = entries.findLast { it.second > hIndex }?.second ?: hIndex+1
            val nextLowestHIndex = entries.find { it.second < hIndex }?.second ?: hIndex-1

            val prefix = rankedEntries.filter { it.second == nextHighestHIndex }
            prefix.forEach {
                PlayStatRow(ctx).apply {
                    setLabel(it.first)
                    setValue(it.second)
                    table.addView(this)
                }
            }

            val list = rankedEntries.filter { it.second == hIndex }
            if (list.isEmpty()) {
                addDivider(table)
            } else {
                list.forEach {
                    PlayStatRow(ctx).apply {
                        setLabel(it.first)
                        setValue(it.second)
                        setBackgroundResource(R.color.light_blue)
                        table.addView(this)
                    }
                }
            }

            val suffix = rankedEntries.filter { it.second == nextLowestHIndex }
            suffix.forEach {
                PlayStatRow(ctx).apply {
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
        View(ctx).apply {
            this.layoutParams = TableLayout.LayoutParams(0, 1)
            this.setBackgroundResource(R.color.dark_blue)
            container.addView(this)
        }
    }

    private fun showAlertDialog(@StringRes titleResId: Int, @StringRes messageResId: Int, vararg formatArgs: Any) {
        val spannableMessage = SpannableString(getString(messageResId, *formatArgs))
        Linkify.addLinks(spannableMessage, Linkify.WEB_URLS)
        val dialog = AlertDialog.Builder(ctx)
                .setTitle(titleResId)
                .setMessage(spannableMessage)
                .show()
        dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key.startsWith(PreferencesUtils.LOG_PLAY_STATS_PREFIX)) {
            bindAccuracyMessage()
            // TODO refresh view model
        }
    }
}
