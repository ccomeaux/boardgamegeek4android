package com.boardgamegeek.ui

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.Transition
import android.transition.TransitionManager
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGamePlayStatsBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GamePlayStatsViewModel
import com.boardgamegeek.ui.widget.PlayStatRow
import com.boardgamegeek.ui.widget.PlayerStatView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class GamePlayStatsFragment : Fragment() {
    private var _binding: FragmentGamePlayStatsBinding? = null
    private val binding get() = _binding!!
    private var gameId = BggContract.INVALID_ID

    @ColorInt
    private var headerColor = 0

    @ColorInt
    private lateinit var playCountColors: IntArray

    private var publishedPlayingTime = 0
    private var personalRating = Game.UNRATED
    private var modifiedWhitmoreScore = Game.UNRATED
    private var isGameOwned = false
    private var playerTransition: Transition? = null
    private val selectedItems = SparseBooleanArray()

    private val prefs: SharedPreferences by lazy { requireContext().preferences() }
    private val viewModel by viewModels<GamePlayStatsViewModel>()

    private var plays = listOf<Play>()
    private var players = listOf<PlayPlayer>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGamePlayStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            gameId = it.getInt(KEY_GAME_ID, BggContract.INVALID_ID)
            headerColor = it.getInt(KEY_HEADER_COLOR, ContextCompat.getColor(requireContext(), R.color.accent))
        }

        if (headerColor != Color.TRANSPARENT) {
            listOf(
                binding.counts.playCountHeaderView,
                binding.scores.scoreHeaderView,
                binding.players.playersHeaderView,
                binding.dates.datesHeaderView,
                binding.time.playTimeHeaderView,
                binding.locations.locationsHeaderView,
                binding.advanced.advancedHeaderView
            )
                .forEach { it.setTextColor(headerColor) }
            listOf(binding.scores.scoreHelpView, binding.players.playersSkillHelpView)
                .forEach { it.setColorFilter(headerColor) }
        }
        playCountColors = intArrayOf(
            ContextCompat.getColor(requireContext(), R.color.orange),
            ContextCompat.getColor(requireContext(), R.color.dark_blue),
            ContextCompat.getColor(requireContext(), R.color.light_blue)
        )

        binding.counts.playCountChart.apply {
            description = null
            setDrawGridBackground(false)
            axisLeft.isEnabled = false
            axisRight.granularity = 1.0f
            xAxis.granularity = 1.0f
            xAxis.setDrawGridLines(false)
        }

        playerTransition = AutoTransition()
        playerTransition?.duration = 150
        playerTransition?.interpolator = android.view.animation.AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in)

        binding.scores.scoreHelpView.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_scores)
                .setView(R.layout.dialog_help_score)
                .show()
        }

        binding.players.playersSkillHelpView.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_players_skill)
                .setMessage(R.string.player_skill_info)
                .show()
        }

        viewModel.collectionItems.observe(viewLifecycleOwner) {
            it?.first()?.let { item: CollectionItem ->
                playCountColors = intArrayOf(
                    item.winsColor.colorOrElse(R.color.orange),
                    item.winnablePlaysColor.colorOrElse(R.color.dark_blue),
                    item.allPlaysColor.colorOrElse(R.color.light_blue),
                )
            }

            publishedPlayingTime = it?.first()?.playingTime ?: 0
            personalRating = it?.filter { item -> item.rating > 0.0 }?.map { item -> item.rating }?.average() ?: Game.UNRATED
            isGameOwned = it?.any { item -> item.own } ?: false
            modifiedWhitmoreScore = it?.filter { item -> item.rating > 0.0 }?.map { item -> item.modifiedWhitmoreScore }?.average() ?: 0.0

            viewModel.plays.observe(viewLifecycleOwner) { plays ->
                if (plays.isNullOrEmpty()) {
                    binding.progressView.hide()
                    binding.dataView.fadeOut()
                    binding.emptyView.fadeIn()
                } else {
                    this.plays = plays.sortedByDescending { play -> play.dateInMillis }
                    viewModel.players.observe(viewLifecycleOwner) { players ->
                        this.players = players

                        val stats = Stats(
                            plays.filter { play ->
                                prefs[PlayStatPrefs.LOG_PLAY_STATS_INCOMPLETE, false] ?: false || !play.incomplete
                            },
                            players,
                            publishedPlayingTime,
                            personalRating,
                            prefs[PlayStatPrefs.KEY_GAME_H_INDEX, 0] ?: 0,
                            modifiedWhitmoreScore,
                        )
                        stats.calculate()
                        bindUi(stats)

                        binding.progressView.hide()
                        binding.emptyView.fadeOut()
                        binding.dataView.fadeIn()
                    }
                }
            }
        }
        viewModel.setGameId(gameId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun Int.colorOrElse(@ColorRes colorResId: Int) =
        if (this == Color.TRANSPARENT) ContextCompat.getColor(requireContext(), colorResId) else this

    private fun bindUi(stats: Stats) {
        // region PLAY COUNT

        binding.counts.playCountTable.removeAllViews()

        val playStatRow = PlayStatRow(requireContext())
        when {
            stats.getDateForPlayNumber(100).isNotEmpty() -> playStatRow.setValue(getString(R.string.play_stat_dollar))
            stats.getDateForPlayNumber(50).isNotEmpty() -> playStatRow.setValue(getString(R.string.play_stat_half_dollar))
            stats.getDateForPlayNumber(25).isNotEmpty() -> playStatRow.setValue(getString(R.string.play_stat_quarter))
            stats.getDateForPlayNumber(10).isNotEmpty() -> playStatRow.setValue(getString(R.string.play_stat_dime))
            stats.getDateForPlayNumber(5).isNotEmpty() -> playStatRow.setValue(getString(R.string.play_stat_nickel))
        }
        binding.counts.playCountTable.addView(playStatRow)

        val playCount = stats.playCountSince()
        val playCountIncomplete = stats.playCountSince(includeIncomplete = true) - playCount
        addPlayStat(binding.counts.playCountTable, playCount.toString(), R.string.play_stat_play_count)
        if (playCountIncomplete > 0) {
            addPlayStat(binding.counts.playCountTable, playCountIncomplete.toString(), R.string.play_stat_play_count_incomplete)
        }
        addPlayStat(binding.counts.playCountTable, stats.getMonthsPlayed().toString(), R.string.play_stat_months_played)
        if (stats.playsPerMonth > 0.0) {
            addPlayStat(binding.counts.playCountTable, DOUBLE_FORMAT.format(stats.playsPerMonth), R.string.play_stat_play_rate)
        }

        val username = prefs[AccountPreferences.KEY_USERNAME, ""] ?: ""
        if (username.isBlank()) {
            binding.counts.playCountChart.visibility = View.GONE
        } else {
            val playCountValues = ArrayList<BarEntry>()
            for (i in stats.minPlayerCount..stats.maxPlayerCount) {
                val winnablePlayCount = stats.getPlayerStats(username)?.getWinnablePlayCountByPlayerCount(i) ?: 0
                val wins = stats.getPlayerStats(username)?.getWinCountByPlayerCount(i) ?: 0
                val playCountPerPlayer = stats.getPlayCountByPlayerCount(i)
                playCountValues.add(
                    BarEntry(
                        i.toFloat(), floatArrayOf(
                            wins.toFloat(),
                            winnablePlayCount - wins.toFloat(),
                            playCountPerPlayer - winnablePlayCount.toFloat()
                        )
                    )
                )
            }
            if (playCountValues.size > 0) {
                val playCountDataSet = BarDataSet(playCountValues, getString(R.string.title_plays)).apply {
                    setDrawValues(false)
                    isHighlightEnabled = false
                    setColors(*playCountColors)
                    stackLabels = arrayOf(getString(R.string.title_wins), getString(R.string.winnable), getString(R.string.all))
                }
                val dataSets = mutableListOf<IBarDataSet>()
                dataSets.add(playCountDataSet)
                binding.counts.playCountChart.data = BarData(dataSets)
                binding.counts.playCountChart.animateY(1000, Easing.EaseInOutBack)
                binding.counts.playCountChart.visibility = View.VISIBLE
            } else {
                binding.counts.playCountChart.visibility = View.GONE
            }
        }

        // endregion PLAY COUNT

        // region SCORES

        if (stats.hasScores()) {
            binding.scores.lowScoreView.text = SCORE_FORMAT.format(stats.lowScore)
            binding.scores.averageScoreView.text = SCORE_FORMAT.format(stats.averageScore)
            binding.scores.averageWinScoreView.text = SCORE_FORMAT.format(stats.averageWinningScore)
            binding.scores.highScoreView.text = SCORE_FORMAT.format(stats.highScore)

            if (stats.highScore != INVALID_SCORE && stats.lowScore != INVALID_SCORE && stats.highScore > stats.lowScore) {
                binding.scores.scoreGraphView.lowScore = stats.lowScore
                binding.scores.scoreGraphView.averageScore = stats.averageScore
                binding.scores.scoreGraphView.averageWinScore = stats.averageWinningScore
                binding.scores.scoreGraphView.highScore = stats.highScore
                binding.scores.scoreGraphView.visibility = View.VISIBLE
            }

            binding.scores.lowScoreView.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.title_low_scorers)
                    .setMessage(stats.lowScorers)
                    .show()
            }

            binding.scores.highScoreView.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.title_high_scorers)
                    .setMessage(stats.highScorers)
                    .show()
            }

            binding.scores.scoresCard.visibility = View.VISIBLE
        } else {
            binding.scores.scoresCard.visibility = View.GONE
        }

        // endregion

        // region PLAYERS
        binding.players.playersList.removeAllViews()
        for ((position, stat) in stats.getPlayerStats().withIndex()) {
            val view = PlayerStatView(requireActivity()).apply {
                setName(stat.description)
                setWinInfo(stat.numberOfPlaysWon, stat.numberOfWinnablePlays)
                setWinSkill(stat.winSkill)
                setOverallLowScore(stats.lowScore)
                setOverallAverageScore(stats.averageScore)
                setOverallAverageWinScore(stats.averageWinningScore)
                setOverallHighScore(stats.highScore)
                setLowScore(stat.lowScore)
                setAverageScore(stat.averageScore)
                setAverageWinScore(stat.averageWinScore)
                setHighScore(stat.highScore)
                showScores(selectedItems[position, false])
                if (stats.hasScores()) {
                    setOnClickListener {
                        TransitionManager.beginDelayedTransition(binding.players.playersList, playerTransition)
                        if (selectedItems.get(position, false)) {
                            selectedItems.delete(position)
                            showScores(false)
                        } else {
                            selectedItems.put(position, true)
                            showScores(true)
                        }
                    }
                }
            }
            binding.players.playersList.addView(view)
        }
        binding.players.playersCard.isVisible = players.isNotEmpty()

        // endregion PLAYERS

        // region DATES

        binding.dates.datesTable.removeAllViews()
        addStatRowMaybe(binding.dates.datesTable, stats.getDateForPlayNumber(1)).setLabel(R.string.play_stat_first_play)
        addStatRowMaybe(binding.dates.datesTable, stats.getDateForPlayNumber(5)).setLabel(R.string.play_stat_nickel)
        addStatRowMaybe(binding.dates.datesTable, stats.getDateForPlayNumber(10)).setLabel(R.string.play_stat_dime)
        addStatRowMaybe(binding.dates.datesTable, stats.getDateForPlayNumber(25)).setLabel(R.string.play_stat_quarter)
        addStatRowMaybe(binding.dates.datesTable, stats.getDateForPlayNumber(50)).setLabel(R.string.play_stat_half_dollar)
        addStatRowMaybe(binding.dates.datesTable, stats.getDateForPlayNumber(100)).setLabel(R.string.play_stat_dollar)
        addStatRowMaybe(binding.dates.datesTable, stats.getDateForPlayNumber(stats.playCountSince())).setLabel(R.string.play_stat_last_play)

        // endregion DATES

        // region PLAY TIME

        binding.time.playTimeTable.removeAllViews()

        addPlayStat(binding.time.playTimeTable, stats.hoursPlayedSince().toInt().toString(), R.string.play_stat_hours_played)
        val average = stats.averagePlayTime
        if (average > 0) {
            addPlayStat(binding.time.playTimeTable, average.asTime(), R.string.play_stat_average_play_time)
            if (publishedPlayingTime > 0) {
                if (average > publishedPlayingTime) {
                    addPlayStat(binding.time.playTimeTable, (average - publishedPlayingTime).asTime(), R.string.play_stat_average_play_time_slower)
                } else if (publishedPlayingTime > average) {
                    addPlayStat(binding.time.playTimeTable, (publishedPlayingTime - average).asTime(), R.string.play_stat_average_play_time_faster)
                }
            } // don't display anything if the average is exactly as expected
        }
        if (stats.averagePlayTimePerPlayer > 0) {
            addPlayStat(binding.time.playTimeTable, stats.averagePlayTimePerPlayer.asTime(), R.string.play_stat_average_play_time_per_player)
        }

        // endregion PLAY TIME

        // region LOCATIONS

        binding.locations.locationsTable.removeAllViews()
        binding.locations.locationsCard.isVisible = stats.playsPerLocation.isNotEmpty()
        for (location in stats.playsPerLocation) {
            addPlayStat(binding.locations.locationsTable, location.value.toString(), location.key)
        }

        // endregion LOCATIONS

        // region ADVANCED

        binding.advanced.advancedTable.removeAllViews()
        if (personalRating != Game.UNRATED) {
            addPlayStat(
                binding.advanced.advancedTable,
                stats.calculateFriendlessHappinessMetric().toString(),
                R.string.play_stat_fhm
            ).setInfoText(R.string.play_stat_fhm_info)
            addPlayStat(
                binding.advanced.advancedTable,
                stats.calculateHuberHappinessMetricSince().toString(),
                R.string.play_stat_hhm
            ).setInfoText(R.string.play_stat_hhm_info)
            addPlayStat(
                binding.advanced.advancedTable,
                DOUBLE_FORMAT.format(stats.calculateHuberHeat()),
                R.string.play_stat_huber_heat
            ).setInfoText(R.string.play_stat_huber_heat_info)
            addPlayStat(
                binding.advanced.advancedTable,
                DOUBLE_FORMAT.format(stats.calculateZefquaaviusHeat()),
                R.string.play_stat_zefquaavius_heat
            ).setInfoText(R.string.play_stat_zefquaavius_heat_info)
            addPlayStat(
                binding.advanced.advancedTable,
                DOUBLE_FORMAT.format(stats.calculateRandyCoxNotUnhappinessMetric()),
                R.string.play_stat_ruhm
            ).setInfoText(R.string.play_stat_ruhm_info)
        }
        if (isGameOwned) {
            addPlayStat(
                binding.advanced.advancedTable,
                stats.calculateUtilization().asPercentage(),
                R.string.play_stat_utilization
            ).setInfoText(R.string.play_stat_utilization_info)
        }
        if (stats.playCountShortOfHIndex > 0) {
            addPlayStat(binding.advanced.advancedTable, stats.playCountShortOfHIndex.toString(), R.string.play_stat_game_h_index_offset_out)
        } else {
            addPlayStat(binding.advanced.advancedTable, "", R.string.play_stat_game_h_index_offset_in)
        }

        // endregion ADVANCED
    }

    private fun addPlayStat(table: TableLayout, value: String, label: String): PlayStatRow {
        return PlayStatRow(requireContext()).apply {
            setValue(value)
            setLabel(label)
            table.addView(this)
        }
    }

    private fun addPlayStat(table: TableLayout, value: String, @StringRes label: Int): PlayStatRow {
        return PlayStatRow(requireContext()).apply {
            setValue(value)
            setLabel(label)
            table.addView(this)
        }
    }

    private fun addStatRowMaybe(container: ViewGroup, date: String?): PlayStatRow {
        val view = PlayStatRow(requireContext())
        if (date?.isNotEmpty() == true) {
            container.addView(view)
            view.setValueAsDate(date, requireContext())
        }
        return view
    }

    // TODO move to ViewModel
    private class Stats(
        val plays: List<Play>,
        val players: List<PlayPlayer>,
        val publishedPlayingTime: Int,
        val personalRating: Double,
        val hIndex: Int,
        val modifiedWhitmoreScore: Double,
    ) {
        private var playCountByPlayerCount = mapOf<Int, Int>()
        private var playCountByLocation = mapOf<String, Int>()
        private val playerStats = mutableMapOf<String, PlayerStats>()
        private val sortedPlaysWithPlayers = mutableListOf<Play>()
        private val playDates = mutableListOf<String>()
        private val filteredPlayers = mutableListOf<Pair<Play, PlayPlayer>>()
        private val sortedPlays = plays.sortedBy { it.dateInMillis }

        val Play.date: String
            get() = simpleDateFormat.format(this.dateInMillis)

        val Play.yearAndMonth: String
            get() = this.date.substring(0, 7)

        fun calculate() {
            // reset values
            playerStats.clear()

            // set values
            playCountByPlayerCount = plays.filter { it.playerCount > 0 }.groupingBy { it.playerCount }.fold(0) { playCount, play ->
                playCount + play.quantity
            }
            playCountByLocation = plays.filter { it.location.isNotBlank() }.groupingBy { it.location }.fold(0) { playCount, play ->
                playCount + play.quantity
            }

            for (play in sortedPlays) {
                sortedPlaysWithPlayers += play.copy(_players = players.filter { it.playInternalId == play.internalId })
                repeat(play.quantity) {
                    playDates += play.date
                    filteredPlayers += players.filter { it.playInternalId == play.internalId }.map { play to it }
                }
            }

            filteredPlayers.groupBy(
                keySelector = { it.second.id },
            ).forEach { (key, value) ->
                playerStats[key] = PlayerStats(value)
            }
        }

        private fun getPlaysSince(dateInMillis: Long?) = dateInMillis?.let { plays.filter { it.dateInMillis > dateInMillis } } ?: plays

        // region DATE

        fun getDateForPlayNumber(number: Int) = playDates.getOrNull(number - 1) ?: ""

        fun getMonthsPlayed() = plays.map { it.yearAndMonth }.toSet().size

        /**
         * Calculate the number of days from the first play to the last play.
         */
        private fun calculateFlash(): Long {
            return daysBetweenDates(sortedPlays.first().dateInMillis, sortedPlays.last().dateInMillis)
        }

        /**
         * Calculate the number of days since the last play.
         */
        private fun calculateLag(): Long {
            return daysBetweenDates(sortedPlays.last().dateInMillis)
        }

        //private fun calculateSpan(): Long {
        //  return daysBetweenDates(sortedPlays.first().dateInMillis)
        //}

        private fun daysBetweenDates(from: Long, to: Long = System.currentTimeMillis()): Long {
            return (to - from).milliseconds.inWholeDays.coerceAtLeast(1)
        }

        // endregion DATE

        // region QUANTITY

        fun playCountSince(dateInMillis: Long? = null, includeIncomplete: Boolean = false): Int =
            getPlaysSince(dateInMillis).filter { includeIncomplete || !it.incomplete }.sumOf { it.quantity }

        // endregion QUANTITY

        // region PLAYING TIME

        fun hoursPlayedSince(dateInMillis: Long? = null): Double {
            val (estimatePlays, actualPlays) = getPlaysSince(dateInMillis).partition { it.length == 0 }
            return (estimatePlays.sumOf { publishedPlayingTime * it.quantity } + actualPlays.sumOf { it.length }) / 60.0
        }

        val averagePlayTime: Int
            get() = if (plays.any { it.length > 0 }) {
                val playsWithLength = plays.filter { it.length > 0 }
                playsWithLength.sumOf { it.length } / playsWithLength.sumOf { it.quantity }
            } else 0

        val averagePlayTimePerPlayer: Int
            get() = if (plays.any { it.length > 0 && it.playerCount > 0 }) {
                val filteredPlays = plays.filter { it.length > 0 && it.playerCount > 0 }
                filteredPlays.sumOf { it.length / it.playerCount } / filteredPlays.sumOf { it.quantity }
            } else 0

        // endregion PLAYING TIME

        // region PLAYER COUNT

        val minPlayerCount: Int
            get() = playCountByPlayerCount.keys.minOrNull() ?: 0

        val maxPlayerCount: Int
            get() = playCountByPlayerCount.keys.maxOrNull() ?: 0

        fun getPlayCountByPlayerCount(playerCount: Int) = playCountByPlayerCount[playerCount] ?: 0

        // endregion PLAYER COUNT

        // region PLAYERS

        fun getPlayerStats(): List<PlayerStats> {
            return playerStats.values.toList().sortedByDescending { it.numberOfPlays }
        }

        fun getPlayerStats(username: String): PlayerStats? {
            return playerStats.values.find { it.username == username }
        }

        // endregion PLAYERS

        // region SCORE

        fun hasScores() = filteredPlayers.any { it.second.numericScore != null }

        val lowScore: Double get() = filteredPlayers.mapNotNull { it.second.numericScore }.minOrNull() ?: INVALID_SCORE

        val highScore: Double get() = filteredPlayers.mapNotNull { it.second.numericScore }.maxOrNull() ?: INVALID_SCORE

        val averageScore: Double get() = filteredPlayers.mapNotNull { it.second.numericScore }.average()

        val averageWinningScore: Double get() = filteredPlayers.filter { it.second.isWin }.mapNotNull { it.second.numericScore }.average()

        val lowScorers: String
            get() = if (lowScore == INVALID_SCORE) "" else
                filteredPlayers.filter { it.second.numericScore == lowScore }.map { it.second.description }.formatList()

        val highScorers: String
            get() = if (highScore == INVALID_SCORE) "" else
                filteredPlayers.filter { it.second.numericScore == highScore }.map { it.second.description }.formatList()

        // endregion SCORE

        // region LOCATION

        val playsPerLocation: List<Map.Entry<String, Int>>
            get() = playCountByLocation.entries.toList().sortedBy { it.key }.sortedByDescending { it.value }

        // endregion LOCATION

        // region METRICS

        /**
         * The number of plays per month, from the first and last plays.
         */
        val playsPerMonth: Double
            get() = (((playCountSince() * 365.25) / calculateFlash()) / 12).coerceAtMost(playCountSince().toDouble())

        fun calculateUtilization(): Double {
            return playCountSince().toDouble().cdf(lambda)
        }

        fun calculateFriendlessHappinessMetric(): Int {
            if (personalRating == Game.UNRATED) return 0
            return ((personalRating * 5) + playCountSince() + (4 * getMonthsPlayed()) + hoursPlayedSince()).toInt()
        }

        fun calculateHuberHappinessMetricSince(dateInMillis: Long? = null): Int {
            if (personalRating == Game.UNRATED) return 0
            return ((personalRating - 4.5) * hoursPlayedSince(dateInMillis)).toInt()
        }

        fun calculateRandyCoxNotUnhappinessMetric(): Double {
            if (personalRating == Game.UNRATED) return 0.0
            val raw = ((calculateFlash().toDouble()) / calculateLag()) * getMonthsPlayed() * personalRating
            return if (raw < 1.0) 0.0 else ln(raw)
        }

        val playCountShortOfHIndex: Int by lazy {
            (hIndex - playCountSince()).coerceAtLeast(0)
        }

        // TODO - why would we ever need this?
//        fun getMonthsPerPlay(): Int {
//            val days = calculateSpan()
//            val months = (days / 365.25 * 12).toInt()
//            return months / playCount
//        }

        fun calculateHuberHeat(): Double {
            val lastYear = Calendar.getInstance().apply {
                add(Calendar.YEAR, -1)
            }.timeInMillis
            return calculateGrayHotness(lastYear)
        }

        fun calculateZefquaaviusHeat(): Double {
            val lastYear = Calendar.getInstance().apply {
                add(Calendar.YEAR, -1)
            }.timeInMillis
            return calculateZefquaaviusHotness(lastYear)
        }

        fun calculateZefquaaviusHotness(sinceDateInMillis: Long): Double {
            return calculateGrayHotness(sinceDateInMillis) * modifiedWhitmoreScore
        }

        fun calculateGrayHotness(sinceDateInMillis: Long): Double {
            // http://matthew.gray.org/2005/10/games_16.html
            val intervalPlayCount = playCountSince(sinceDateInMillis)
            val s = 1 + (intervalPlayCount.toDouble() / playCountSince())
            return s * s * sqrt(intervalPlayCount.toDouble()) * calculateHuberHappinessMetricSince(sinceDateInMillis)
        }

        // endregion METRICS

        companion object {
            private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            private val lambda = ln(0.1) / -10
        }
    }

    private class PlayerStats(val plays: List<Pair<Play, PlayPlayer>>) {
        val Play.isWinnable: Boolean
            get() {
                return when {
                    noWinStats -> false
                    playerCount == 0 -> false
                    deleteTimestamp > 0L -> false
                    updateTimestamp > 0L -> true
                    else -> playId > 0
                }
            }

        val username: String
            get() = plays.first().second.username

        val description: String
            get() = plays.first().second.description

        val numberOfPlays: Int
            get() = plays.sumOf { it.first.quantity }

        val numberOfWinnablePlays: Int
            get() = plays.filter { it.first.isWinnable }.sumOf { it.first.quantity }

        val numberOfPlaysWon: Int
            get() = plays.filter { it.second.isWin }.sumOf { it.first.quantity }

        val lowScore: Double
            get() = plays.mapNotNull { it.second.numericScore }.minOrNull() ?: INVALID_SCORE

        val highScore: Double
            get() = plays.mapNotNull { it.second.numericScore }.maxOrNull() ?: INVALID_SCORE

        fun getWinCountByPlayerCount(playerCount: Int): Int {
            return plays.filter { it.second.isWin && it.first.playerCount == playerCount }.sumOf { it.first.quantity }
        }

        fun getWinnablePlayCountByPlayerCount(playerCount: Int): Int {
            return plays.filter { it.first.isWinnable && it.first.playerCount == playerCount }.sumOf { it.first.quantity }
        }

        //fun getPlayCountByPlayerCount(playerCount: Int): Int {
        //    return plays.filter { it.first.playerCount == playerCount }.sumOf { it.first.quantity }
        //}

        val winSkill: Int
            get() = ((plays.filter { it.second.isWin }.sumOf { it.first.quantity * it.first.playerCount }
                .toDouble() / plays.filter { it.first.isWinnable }
                .sumOf { it.first.quantity }) * 100).toInt()

        val averageScore: Double
            get() = plays.mapNotNull { it.second.numericScore }.let {
                if (it.isEmpty()) INVALID_SCORE else it.average()
            }

        val averageWinScore: Double
            get() = plays.filter { it.second.isWin }.mapNotNull { it.second.numericScore }.let {
                if (it.isEmpty()) INVALID_SCORE else it.average()
            }
    }

    companion object {
        private val SCORE_FORMAT = DecimalFormat("0.##")
        private val DOUBLE_FORMAT = DecimalFormat("0.00")

        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_HEADER_COLOR = "HEADER_COLOR"
        private const val INVALID_SCORE = Int.MIN_VALUE.toDouble()

        fun newInstance(gameId: Int, @ColorInt headerColor: Int): GamePlayStatsFragment {
            return GamePlayStatsFragment().apply {
                arguments = bundleOf(
                    KEY_GAME_ID to gameId,
                    KEY_HEADER_COLOR to headerColor,
                )
            }
        }
    }
}
