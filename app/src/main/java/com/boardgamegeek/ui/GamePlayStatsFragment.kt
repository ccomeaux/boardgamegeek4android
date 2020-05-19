package com.boardgamegeek.ui

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.Transition
import android.transition.TransitionManager
import android.util.SparseBooleanArray
import android.util.SparseIntArray
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.entities.HIndexEntity
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.PlayPlayerEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GamePlayStatsViewModel
import com.boardgamegeek.ui.widget.PlayStatRow
import com.boardgamegeek.ui.widget.PlayerStatView
import com.boardgamegeek.util.AnimationUtils
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import kotlinx.android.synthetic.main.fragment_game_play_stats.*
import kotlinx.android.synthetic.main.include_game_play_stats_advanced.*
import kotlinx.android.synthetic.main.include_game_play_stats_counts.*
import kotlinx.android.synthetic.main.include_game_play_stats_dates.*
import kotlinx.android.synthetic.main.include_game_play_stats_locations.*
import kotlinx.android.synthetic.main.include_game_play_stats_players.*
import kotlinx.android.synthetic.main.include_game_play_stats_scores.*
import kotlinx.android.synthetic.main.include_game_play_stats_time.*
import org.jetbrains.anko.support.v4.withArguments
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.*

class GamePlayStatsFragment : Fragment(R.layout.fragment_game_play_stats) {
    private var gameId = BggContract.INVALID_ID

    @ColorInt
    private var headerColor = 0

    @ColorInt
    private lateinit var playCountColors: IntArray

    private var playingTime = 0
    private var personalRating = 0.0
    private var gameOwned = false
    private var playerTransition: Transition? = null
    private val selectedItems = SparseBooleanArray()

    private val prefs: SharedPreferences by lazy { requireContext().preferences() }
    private val viewModel by viewModels<GamePlayStatsViewModel>()

    private var playEntities = listOf<PlayEntity>()
    private var playerEntities = listOf<PlayPlayerEntity>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            gameId = it.getInt(KEY_GAME_ID, BggContract.INVALID_ID)
            headerColor = it.getInt(KEY_HEADER_COLOR, ContextCompat.getColor(requireContext(), R.color.accent))
        }

        if (headerColor != Color.TRANSPARENT) {
            listOf(playCountHeaderView, scoreHeaderView, playersHeaderView, datesHeaderView, playTimeHeaderView, locationsHeaderView, advancedHeaderView)
                    .forEach { it?.setTextColor(headerColor) }
            listOf(scoreHelpView, playersSkillHelpView)
                    .forEach { it?.setColorFilter(headerColor) }
        }
        playCountColors = intArrayOf(
                ContextCompat.getColor(requireContext(), R.color.orange),
                ContextCompat.getColor(requireContext(), R.color.dark_blue),
                ContextCompat.getColor(requireContext(), R.color.light_blue)
        )

        playCountChart.apply {
            description = null
            setDrawGridBackground(false)
            axisLeft.isEnabled = false
            axisRight.granularity = 1.0f
            xAxis.granularity = 1.0f
            xAxis.setDrawGridLines(false)
        }

        if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            playerTransition = AutoTransition()
            playerTransition?.duration = 150
            AnimationUtils.setInterpolator(context, playerTransition)
        }

        scoreHelpView.setOnClickListener {
            AlertDialog.Builder(requireContext())
                    .setTitle(R.string.title_scores)
                    .setView(R.layout.dialog_help_score)
                    .show()
        }

        playersSkillHelpView.setOnClickListener {
            AlertDialog.Builder(requireContext())
                    .setTitle(R.string.title_players_skill)
                    .setMessage(R.string.player_skill_info)
                    .show()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.game.observe(viewLifecycleOwner, Observer { (_, data, _) ->
            data?.first()?.let {
                playCountColors = intArrayOf(
                        if (it.winsColor == Color.TRANSPARENT) ContextCompat.getColor(requireContext(), R.color.orange) else it.winsColor,
                        if (it.winnablePlaysColor == Color.TRANSPARENT) ContextCompat.getColor(requireContext(), R.color.dark_blue) else it.winnablePlaysColor,
                        if (it.allPlaysColor == Color.TRANSPARENT) ContextCompat.getColor(requireContext(), R.color.light_blue) else it.allPlaysColor
                )
            }

            playingTime = data?.first()?.playingTime ?: 0
            personalRating = data?.filter { it.rating > 0.0 }?.map { it.rating }?.average() ?: 0.0
            gameOwned = data?.any { it.own } ?: false

            viewModel.plays.observe(viewLifecycleOwner, Observer { (_, data, _) ->
                if (data == null || data.isEmpty()) {
                    progressView.hide()
                    dataView.fadeOut()
                    emptyView.fadeIn()
                } else {
                    playEntities = data
                    viewModel.players.observe(viewLifecycleOwner, Observer {
                        playerEntities = it

                        val stats = Stats()
                        stats.calculate()
                        bindUi(stats)

                        progressView.hide()
                        emptyView.fadeOut()
                        dataView.fadeIn()
                    })
                }
            })
        })
        viewModel.setGameId(gameId)
    }

    private fun bindUi(stats: Stats) {
        // region PLAY COUNT

        playCountTable.removeAllViews()

        val playStatRow = PlayStatRow(requireContext())
        if (stats.dollarDate.isNotEmpty()) {
            playStatRow.setValue(getString(R.string.play_stat_dollar))
        } else if (stats.halfDollarDate.isNotEmpty()) {
            playStatRow.setValue(getString(R.string.play_stat_half_dollar))
        } else if (stats.quarterDate.isNotEmpty()) {
            playStatRow.setValue(getString(R.string.play_stat_quarter))
        } else if (stats.dimeDate.isNotEmpty()) {
            playStatRow.setValue(getString(R.string.play_stat_dime))
        } else if (stats.nickelDate.isNotEmpty()) {
            playStatRow.setValue(getString(R.string.play_stat_nickel))
        }
        playCountTable.addView(playStatRow)

        addPlayStat(playCountTable, stats.playCount.toString(), R.string.play_stat_play_count)
        if (stats.playCountIncomplete > 0 && stats.playCountIncomplete != stats.playCount) {
            addPlayStat(playCountTable, stats.playCountIncomplete.toString(), R.string.play_stat_play_count_incomplete)
        }
        addPlayStat(playCountTable, stats.getMonthsPlayed().toString(), R.string.play_stat_months_played)
        if (stats.playRate > 0.0) {
            addPlayStat(playCountTable, DOUBLE_FORMAT.format(stats.playRate), R.string.play_stat_play_rate)
        }

        val playCountValues = ArrayList<BarEntry>()
        for (i in stats.minPlayerCount..stats.maxPlayerCount) {
            val winnablePlayCount = stats.getPersonalWinnablePlayCount(i)
            val wins = stats.getPersonalWinCount(i)
            val playCount = stats.getPlayCount(i)
            playCountValues.add(
                    BarEntry(i.toFloat(), floatArrayOf(
                            wins.toFloat(),
                            winnablePlayCount - wins.toFloat(),
                            playCount - winnablePlayCount.toFloat()))
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
            playCountChart.data = BarData(dataSets)
            playCountChart.animateY(1000, Easing.EaseInOutBack)
            playCountChart.visibility = View.VISIBLE
        } else {
            playCountChart.visibility = View.GONE
        }

        // endregion PLAY COUNT

        // region SCORES

        if (stats.hasScores()) {
            lowScoreView.text = SCORE_FORMAT.format(stats.lowScore)
            averageScoreView.text = SCORE_FORMAT.format(stats.averageScore)
            averageWinScoreView.text = SCORE_FORMAT.format(stats.averageWinningScore)
            highScoreView.text = SCORE_FORMAT.format(stats.highScore)
            if (stats.highScore > stats.lowScore) {
                scoreGraphView.lowScore = stats.lowScore
                scoreGraphView.averageScore = stats.averageScore
                scoreGraphView.averageWinScore = stats.averageWinningScore
                scoreGraphView.highScore = stats.highScore
                scoreGraphView.visibility = View.VISIBLE
            }

            lowScoreView.setOnClickListener {
                AlertDialog.Builder(requireContext())
                        .setTitle(R.string.title_low_scorers)
                        .setMessage(stats.lowScorers)
                        .show()
            }

            highScoreView.setOnClickListener {
                AlertDialog.Builder(requireContext())
                        .setTitle(R.string.title_high_scorers)
                        .setMessage(stats.highScorers)
                        .show()
            }

            scoresCard.visibility = View.VISIBLE
        } else {
            scoresCard.visibility = View.GONE
        }

        // endregion

        // region PLAYERS
        playersList.removeAllViews()
        for ((position, playerStats) in stats.getPlayerStats().withIndex()) {
            val stat = playerStats.value
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
                        if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                            TransitionManager.beginDelayedTransition(playersList, playerTransition)
                        }
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
            playersList.addView(view)
        }
        playersCard.isVisible = playerEntities.isNotEmpty()

        // endregion PLAYERS

        // region DATES

        datesTable.removeAllViews()
        addStatRowMaybe(datesTable, stats.firstPlayDate).setLabel(R.string.play_stat_first_play)
        addStatRowMaybe(datesTable, stats.nickelDate).setLabel(R.string.play_stat_nickel)
        addStatRowMaybe(datesTable, stats.dimeDate).setLabel(R.string.play_stat_dime)
        addStatRowMaybe(datesTable, stats.quarterDate).setLabel(R.string.play_stat_quarter)
        addStatRowMaybe(datesTable, stats.halfDollarDate).setLabel(R.string.play_stat_half_dollar)
        addStatRowMaybe(datesTable, stats.dollarDate).setLabel(R.string.play_stat_dollar)
        addStatRowMaybe(datesTable, stats.lastPlayDate).setLabel(R.string.play_stat_last_play)

        // endregion DATES

        // region PLAY TIME

        playTimeTable.removeAllViews()

        addPlayStat(playTimeTable, stats.hoursPlayed.toInt().toString(), R.string.play_stat_hours_played)
        val average = stats.averagePlayTime
        if (average > 0) {
            addPlayStat(playTimeTable, average.asTime(), R.string.play_stat_average_play_time)
            if (playingTime > 0) {
                if (average > playingTime) {
                    addPlayStat(playTimeTable, (average - playingTime).asTime(), R.string.play_stat_average_play_time_slower)
                } else if (playingTime > average) {
                    addPlayStat(playTimeTable, (playingTime - average).asTime(), R.string.play_stat_average_play_time_faster)
                }
            } // don't display anything if the average is exactly as expected
        }
        if (stats.averagePlayTimePerPlayer > 0) {
            addPlayStat(playTimeTable, stats.averagePlayTimePerPlayer.asTime(), R.string.play_stat_average_play_time_per_player)
        }

        // endregion PLAY TIME

        // region LOCATIONS

        locationsTable.removeAllViews()
        locationsCard.isVisible = stats.playsPerLocation.isNotEmpty()
        for (location in stats.playsPerLocation) {
            addPlayStat(locationsTable, location.value.toString(), location.key)
        }

        // endregion LOCATIONS

        // region ADVANCED

        advancedTable.removeAllViews()
        if (personalRating > 0.0) {
            addPlayStat(advancedTable, stats.calculateFhm().toString(), R.string.play_stat_fhm).setInfoText(R.string.play_stat_fhm_info)
            addPlayStat(advancedTable, stats.calculateHhm().toString(), R.string.play_stat_hhm).setInfoText(R.string.play_stat_hhm_info)
            addPlayStat(advancedTable, DOUBLE_FORMAT.format(stats.calculateHuberHeat()), R.string.play_stat_huber_heat).setInfoText(R.string.play_stat_huber_heat_info)
            addPlayStat(advancedTable, DOUBLE_FORMAT.format(stats.calculateRuhm()), R.string.play_stat_ruhm).setInfoText(R.string.play_stat_ruhm_info)
        }
        if (gameOwned) {
            addPlayStat(advancedTable, stats.calculateUtilization().asPercentage(), R.string.play_stat_utilization).setInfoText(R.string.play_stat_utilization_info)
        }
        val hIndexOffset = stats.hIndexOffset
        if (hIndexOffset == HIndexEntity.INVALID_H_INDEX) {
            addPlayStat(advancedTable, "", R.string.play_stat_game_h_index_offset_in)
        } else {
            addPlayStat(advancedTable, hIndexOffset.toString(), R.string.play_stat_game_h_index_offset_out)
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

    private inner class Stats {
        private val lambda = ln(0.1) / -10
        private val playerStats: MutableMap<String, PlayerStats> = HashMap()

        var firstPlayDate: String = ""
        var firstPlayDateInMillis = 0L
        var lastPlayDate: String = ""
        var lastPlayDateInMillis = 0L
        var nickelDate: String = ""
        var dimeDate: String = ""
        var quarterDate: String = ""
        var halfDollarDate: String = ""
        var dollarDate: String = ""

        var playCountIncomplete = 0
            private set
        private var playCountWithLength = 0
        private var playerCountSumWithLength = 0
        private var playCountByPlayerCount = mapOf<Int, Int>()
        private var realMinutesPlayed = 0
        private var estimatedMinutesPlayed = 0
        private var numberOfWinnablePlays = 0
        private var scoreSum = 0.0
        private var scoreCount = 0
        var highScore = 0.0
            private set
        var lowScore = 0.0
            private set
        private var winningScoreCount = 0
        private var winningScoreSum = 0.0
        private var playCountByLocation = mapOf<String, Int>()
        private var monthsPlayed = setOf<String>()

        fun calculate() {
            nickelDate = ""
            dimeDate = ""
            quarterDate = ""
            halfDollarDate = ""
            dollarDate = ""
            var count = 0
            estimatedMinutesPlayed = 0
            realMinutesPlayed = 0
            playCountWithLength = 0
            playerCountSumWithLength = 0
            playerStats.clear()
            scoreCount = 0
            scoreSum = 0.0
            winningScoreCount = 0
            winningScoreSum = 0.0
            highScore = Int.MIN_VALUE.toDouble()
            lowScore = Int.MAX_VALUE.toDouble()

            playCountIncomplete = playEntities.sumBy { it.quantity }

            firstPlayDate = plays.first().date
            firstPlayDateInMillis = plays.first().dateInMillis
            lastPlayDate = plays.last().date
            lastPlayDateInMillis = plays.last().dateInMillis
            numberOfWinnablePlays = plays.filter { it.isWinnable }.sumBy { it.quantity }

            playCountByPlayerCount = plays.filter { it.playerCount > 0 }.groupingBy { it.playerCount }.fold(0) { playCount, play ->
                playCount + play.quantity
            }
            playCountByLocation = plays.filter { it.location.isNotBlank() }.groupingBy { it.location }.fold(0) { playCount, play ->
                playCount + play.quantity
            }
            monthsPlayed = plays.groupBy { it.yearAndMonth }.keys

            for (play in plays) {
                // nickel and dime dates
                if (count < 5 && (count + play.quantity) >= 5) {
                    nickelDate = play.date
                }
                if (count < 10 && (count + play.quantity) >= 10) {
                    dimeDate = play.date
                }
                if (count < 25 && (count + play.quantity) >= 25) {
                    quarterDate = play.date
                }
                if (count < 50 && (count + play.quantity) >= 50) {
                    halfDollarDate = play.date
                }
                if (count < 100 && (count + play.quantity) >= 100) {
                    dollarDate = play.date
                }
                count += play.quantity

                if (play.length == 0) {
                    estimatedMinutesPlayed += playingTime * play.quantity
                } else {
                    realMinutesPlayed += play.length
                    playCountWithLength += play.quantity
                    playerCountSumWithLength += play.playerCount * play.quantity
                }

                for (player in playerEntities.filter { it.playId == play.playId }) {
                    if (player.description.isNotEmpty()) {
                        val playerStats = playerStats[player.id] ?: PlayerStats()
                        playerStats.add(play, player)
                        this.playerStats[player.id] = playerStats
                    }

                    player.numericScore?.let { score ->
                        scoreCount += play.quantity
                        scoreSum += score * play.quantity
                        if (player.isWin) {
                            winningScoreCount += play.quantity
                            winningScoreSum += score * play.quantity
                        }
                        highScore = max(highScore, score)
                        lowScore = min(lowScore, score)
                    }
                }
            }
        }

        val plays: List<PlayEntity> = playEntities.filter { prefs[PlayStats.LOG_PLAY_STATS_INCOMPLETE, false] ?: false || !it.incomplete }.reversed()

        val playCount: Int by lazy { plays.sumBy { it.quantity } }

        fun playCountSince(dateInMillis: Long): Int = plays.filter { it.dateInMillis > dateInMillis }.sumBy { it.quantity }

        fun hoursPlayedSince(dateInMillis: Long): Double {
            val estimated = plays.filter { it.dateInMillis > dateInMillis }.filter { it.length == 0 }.sumBy { playingTime * it.quantity }
            val actual = plays.filter { it.dateInMillis > dateInMillis }.filter { it.length > 0 }.sumBy { it.length }
            return (estimated + actual) / 60.0
        }

        val hoursPlayed: Double
            get() = ((realMinutesPlayed + estimatedMinutesPlayed) / 60).toDouble()

        /* plays per month, only counting the active period) */
        val playRate: Double
            get() = (((playCount * 365.25) / calculateFlash()) / 12).coerceAtMost(playCount.toDouble())

        val averagePlayTime: Int
            get() = if (playCountWithLength > 0) realMinutesPlayed / playCountWithLength else 0

        val averagePlayTimePerPlayer: Int
            get() = if (playerCountSumWithLength > 0) realMinutesPlayed / playerCountSumWithLength else 0

        fun getMonthsPlayed(): Int {
            return monthsPlayed.size
        }

        val minPlayerCount: Int
            get() = playCountByPlayerCount.keys.min() ?: 0

        val maxPlayerCount: Int
            get() = playCountByPlayerCount.keys.max() ?: 0

        fun getPersonalWinCount(playerCount: Int): Int {
            return personalStats?.getWinCountByPlayerCount(playerCount) ?: 0
        }

        fun getPersonalWinnablePlayCount(playerCount: Int): Int {
            return personalStats?.getWinnablePlayCountByPlayerCount(playerCount) ?: 0
        }

        fun getPlayCount(playerCount: Int): Int {
            return playCountByPlayerCount[playerCount] ?: 0
        }

        private val personalStats: PlayerStats?
            get() = AccountUtils.getUsername(activity)?.let { username ->
                playerStats.values.find { it.username == username }
            }

        fun hasScores() = scoreCount > 0

        val averageScore: Double
            get() = scoreSum / scoreCount

        val highScorers: String
            get() = if (highScore == Int.MIN_VALUE.toDouble()) "" else
                playerStats.entries.filter { it.value.highScore == highScore }.map { it.value.description }.formatList()

        val lowScorers: String
            get() = if (lowScore == Int.MAX_VALUE.toDouble()) "" else
                playerStats.entries.filter { it.value.lowScore == lowScore }.map { it.value.description }.formatList()

        val averageWinningScore: Double
            get() = winningScoreSum / winningScoreCount

        fun getPlayerStats(): List<Map.Entry<String, PlayerStats>> {
            return playerStats.entries.toList().sortedByDescending { it.value.numberOfPlays }
        }

        val playsPerLocation: List<Map.Entry<String, Int>>
            get() = playCountByLocation.entries.toList().sortedBy { it.key }.sortedByDescending { it.value }

        fun calculateUtilization(): Double {
            return playCount.toDouble().cdf(lambda)
        }

        fun calculateFhm(): Int {
            return ((personalRating * 5) + playCount + (4 * getMonthsPlayed()) + hoursPlayed).toInt()
        }

        fun calculateHhm(): Int {
            return ((personalRating - 4.5) * hoursPlayed).toInt()
        }

        fun calculateHhmSince(dateInMillis: Long): Int {
            return ((personalRating - 4.5) * hoursPlayedSince(dateInMillis)).toInt()
        }

        fun calculateRuhm(): Double {
            val raw = ((calculateFlash().toDouble()) / calculateLag()) * getMonthsPlayed() * personalRating
            return if (raw < 1.0) 0.0 else ln(raw)
        }

        val hIndexOffset: Int
            get() {
                val hIndex = prefs[PlayStats.KEY_GAME_H_INDEX, 0] ?: 0
                return if (playCount >= hIndex) {
                    HIndexEntity.INVALID_H_INDEX
                } else {
                    hIndex - playCount
                }
            }

        fun getMonthsPerPlay(): Int {
            val days = calculateSpan()
            val months = (days / 365.25 * 12).toInt()
            return months / playCount
        }

        fun calculateHuberHeat(): Double {
            val lastYear = Calendar.getInstance().apply {
                add(Calendar.YEAR, -1)
            }.timeInMillis
            return calculateGrayHotness(lastYear)
        }

        fun calculateGrayHotness(sinceDateInMillis: Long): Double {
            // http://matthew.gray.org/2005/10/games_16.html
            val intervalPlayCount = playCountSince(sinceDateInMillis)
            val s = 1 + (intervalPlayCount.toDouble() / playCount)
            return s * s * sqrt(intervalPlayCount.toDouble()) * calculateHhmSince(sinceDateInMillis)
        }

        fun calculateWhitmoreScore(): Int {
            // A score that ranges from 0 for a completely neutral game (6.5) to 7 for a perfect 10.
            // http://www.boardgamegeek.com/geeklist/37832/my-favorite-designers
            return (personalRating * 2 - 13).toInt().coerceAtLeast(0)
        }

        fun calculateZefquaaviusScore(): Double {
            // A score that ranges from -10 to 10, with more weight given to the ends than the middle
            // modified Whitmore score, see: http://boardgamegeek.com/user/zefquaavius
            val neutralRating = 5.5
            var squared = (personalRating - neutralRating).pow(2)
            if (personalRating < neutralRating) {
                squared *= -1.0
            }
            return squared / 2.025
        }

        fun calculateZefquaaviusHotness(sinceDateInMillis: Long): Double {
            return calculateGrayHotness(sinceDateInMillis) * calculateZefquaaviusScore()
        }

        private fun calculateFlash(): Long {
            return daysBetweenDates(firstPlayDateInMillis, lastPlayDateInMillis)
        }

        private fun calculateLag(): Long {
            return daysBetweenDates(lastPlayDateInMillis)
        }

        private fun calculateSpan(): Long {
            return daysBetweenDates(firstPlayDateInMillis)
        }

        private fun daysBetweenDates(from: Long, to: Long = System.currentTimeMillis()): Long {
            return TimeUnit.DAYS.convert(to - from, TimeUnit.MILLISECONDS).coerceAtLeast(1)
        }
    }

    private inner class PlayerStats {
        var username = ""
            private set
        var description: String = ""
        var numberOfPlays = 0
        var numberOfWinnablePlays = 0
        var numberOfPlaysWon = 0
        private var numberOfPlaysWithScore = 0
        private var numberOfPlaysWonWithScore = 0
        private var winsTimesPlayers = 0
        private var totalScore: Double = 0.0
        private var winningScore: Double = 0.0
        var highScore: Double = Int.MIN_VALUE.toDouble()
            private set
        var lowScore: Double = Int.MAX_VALUE.toDouble()
            private set

        private val playsByPlayerCount = SparseIntArray()
        private val winnablePlaysByPlayerCount = SparseIntArray()
        private val winsByPlayerCount = SparseIntArray()

        val invalidScore = Int.MIN_VALUE.toDouble()

        fun add(play: PlayEntity, player: PlayPlayerEntity) {
            username = player.username
            description = player.description
            numberOfPlays += play.quantity
            addByPlayerCount(playsByPlayerCount, play.playerCount, play.quantity)
            if (play.isWinnable) {
                numberOfWinnablePlays += play.quantity
                addByPlayerCount(winnablePlaysByPlayerCount, play.playerCount, play.quantity)
                if (player.isWin) {
                    numberOfPlaysWon += play.quantity
                    winsTimesPlayers += play.quantity * play.playerCount
                    // addByPlayerCount(winsByPlayerCount, play.playerCount, play.quantity)
                    winsByPlayerCount.put(play.playerCount, winsByPlayerCount[play.playerCount] + play.quantity)
                }
            }
            player.numericScore?.let { score ->
                numberOfPlaysWithScore += play.quantity
                totalScore += score * play.quantity
                lowScore = min(lowScore, score)
                highScore = max(highScore, score)
                if (play.isWinnable) {
                    if (player.isWin) {
                        numberOfPlaysWonWithScore += play.quantity
                        winningScore += score * play.quantity
                    }
                }
            }
        }

        private fun addByPlayerCount(playerCountMap: SparseIntArray, playerCount: Int, quantity: Int) {
            playerCountMap.put(playerCount, playerCountMap[playerCount] + quantity)
        }

        fun getWinCountByPlayerCount(playerCount: Int): Int {
            return winsByPlayerCount[playerCount]
        }

        fun getWinnablePlayCountByPlayerCount(playerCount: Int): Int {
            return winnablePlaysByPlayerCount[playerCount]
        }

        fun getPlayCountByPlayerCount(playerCount: Int): Int {
            return playsByPlayerCount[playerCount]
        }

        val winSkill: Int
            get() = ((winsTimesPlayers.toDouble() / numberOfWinnablePlays.toDouble()) * 100).toInt()

        val averageScore: Double
            get() = if (numberOfPlaysWithScore == 0) invalidScore else totalScore / numberOfPlaysWithScore

        val averageWinScore: Double
            get() = if (numberOfPlaysWonWithScore == 0) invalidScore else winningScore / numberOfPlaysWonWithScore
    }

    val PlayEntity.date: String
        get() = FORMAT.format(this.dateInMillis)

    val PlayEntity.year: String
        get() = this.date.substring(0, 4)

    val PlayEntity.yearAndMonth: String
        get() = this.date.substring(0, 7)

    val PlayEntity.isWinnable: Boolean
        get() {
            return when {
                noWinStats -> false
                playerCount == 0 -> false
                deleteTimestamp > 0L -> false
                updateTimestamp > 0L -> true
                else -> playId > 0
            }
        }

    companion object {
        private val SCORE_FORMAT = DecimalFormat("0.##")
        private val DOUBLE_FORMAT = DecimalFormat("0.00")
        private val FORMAT: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_HEADER_COLOR = "HEADER_COLOR"

        fun newInstance(gameId: Int, @ColorInt headerColor: Int): GamePlayStatsFragment {
            return GamePlayStatsFragment().withArguments(
                    KEY_GAME_ID to gameId,
                    KEY_HEADER_COLOR to headerColor)
        }
    }
}