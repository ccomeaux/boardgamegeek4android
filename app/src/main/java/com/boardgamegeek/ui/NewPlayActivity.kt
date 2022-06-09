package com.boardgamegeek.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import com.boardgamegeek.R
import com.boardgamegeek.databinding.ActivityNewPlayBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import com.boardgamegeek.ui.widget.SelfUpdatingView

class NewPlayActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewPlayBinding
    private var startTime = 0L
    private var gameName = ""
    private val viewModel by viewModels<NewPlayViewModel>()
    private var thumbnailUrl: String? = null
    private var imageUrl: String? = null
    private var heroImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameId = intent.getIntExtra(KEY_GAME_ID, INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()

        binding = ActivityNewPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_baseline_clear_24)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.insertedId.observe(this) {
            if ((viewModel.startTime.value ?: 0L) == 0L) {
                this.cancelNotification(TAG_PLAY_TIMER, it)
                SyncService.sync(this, SyncService.FLAG_SYNC_PLAYS_UPLOAD)
            } else {
                launchPlayingNotification(
                    it,
                    gameName,
                    viewModel.location.value.orEmpty(),
                    viewModel.addedPlayers.value?.size ?: 0,
                    startTime,
                    thumbnailUrl.orEmpty(),
                    heroImageUrl.orEmpty(),
                    imageUrl.orEmpty(),
                )
            }
            setResult(Activity.RESULT_OK)
            finish()
        }

        viewModel.game.observe(this) {
            it?.let { entity ->
                gameName = entity.name
                thumbnailUrl = entity.thumbnailUrl
                imageUrl = entity.imageUrl
                heroImageUrl = entity.heroImageUrl

                updateSummary()

                val summaryView = findViewById<PlaySummary>(R.id.summaryView)
                binding.thumbnailView.loadUrl(entity.heroImageUrl, object : ImageLoadCallback {
                    override fun onSuccessfulImageLoad(palette: Palette?) {
                        summaryView.setBackgroundResource(R.color.black_overlay_light)
                    }

                    override fun onFailedImageLoad() {
                        summaryView.setBackgroundResource(0)
                    }
                })
            }
        }

        viewModel.startTime.observe(this) {
            startTime = viewModel.startTime.value ?: 0L
            updateSummary()
            invalidateOptionsMenu()
        }

        viewModel.length.observe(this) { updateSummary() }

        viewModel.location.observe(this) { updateSummary() }

        viewModel.playDate.observe(this) { updateSummary() }

        viewModel.currentStep.observe(this) {
            val fragment = when (it) {
                NewPlayViewModel.Step.DATE -> NewPlayDateFragment()
                NewPlayViewModel.Step.LOCATION, null -> NewPlayLocationsFragment()
                NewPlayViewModel.Step.ADD_PLAYERS -> NewPlayAddPlayersFragment()
                NewPlayViewModel.Step.PLAYERS_COLOR -> NewPlayPlayerColorsFragment()
                NewPlayViewModel.Step.PLAYERS_SORT -> NewPlayPlayerSortFragment()
                NewPlayViewModel.Step.PLAYERS_NEW -> NewPlayPlayerIsNewFragment()
                NewPlayViewModel.Step.PLAYERS_WIN -> NewPlayPlayerWinFragment()
                NewPlayViewModel.Step.COMMENTS -> NewPlayCommentsFragment()
            }
            if (it == null || it == NewPlayViewModel.INITIAL_STEP) {
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragmentContainer, fragment)
                    .commit()
            } else {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
            updateSummary()
        }

        viewModel.setGame(gameId, gameName)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.new_play, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.timer)?.setIcon(if (startTime > 0L) R.drawable.ic_outline_timer_off_24 else R.drawable.ic_outline_timer_24)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (!maybeDiscard()) {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
                true
            }
            R.id.timer -> {
                viewModel.toggleTimer()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 0) {
            if (!maybeDiscard()) super.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }

    private fun maybeDiscard(): Boolean {
        return if ((viewModel.startTime.value ?: 0) > 0 ||
            viewModel.location.value.orEmpty().isNotBlank() ||
            viewModel.addedPlayers.value.orEmpty().isNotEmpty() ||
            viewModel.comments.isNotBlank()
        ) {
            createDiscardDialog(R.string.play, isNew = false).show()
            true
        } else false
    }

    private fun updateSummary() {
        val summaryView = findViewById<PlaySummary>(R.id.summaryView)
        summaryView.gameName = gameName
        summaryView.date = viewModel.playDate.value
        summaryView.step = viewModel.currentStep.value ?: NewPlayViewModel.INITIAL_STEP
        summaryView.startTime = startTime
        summaryView.length = viewModel.length.value ?: 0
        summaryView.location = viewModel.location.value.orEmpty()
        summaryView.playerCount = viewModel.addedPlayers.value?.size ?: 0
        summaryView.updateText()
    }

    class PlaySummary @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.textViewStyle
    ) : SelfUpdatingView(context, attrs, defStyleAttr) {
        var gameName = ""
        var step = NewPlayViewModel.INITIAL_STEP
        var date: Long? = null
        var startTime = 0L
        var length = 0
        var location = ""
        var playerCount = 0

        override fun updateText() {
            text = createSummary()
            isVisible = text.isNotBlank()
        }

        private fun createSummary(): String {
            var summary = gameName

            if (step == NewPlayViewModel.Step.DATE) {
                summary += " ${context.getString(R.string.on)}..."
            } else {
                date?.let {
                    val d = DateUtils.formatDateTime(
                        context, it, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_WEEKDAY or DateUtils.FORMAT_SHOW_WEEKDAY
                    )
                    summary += " ${context.getString(R.string.on)} $d"
                }
            }

            if (startTime > 0L || length > 0) {
                val totalLength = length + if (startTime > 0L) startTime.howManyMinutesOld() else 0
                summary += " ${context.getString(R.string.for_)} $totalLength ${context.getString(R.string.minutes_abbr)}"
            }

            summary += when {
                step == NewPlayViewModel.Step.LOCATION -> " ${context.getString(R.string.at)}..."
                step > NewPlayViewModel.Step.LOCATION -> if (location.isNotBlank()) " ${context.getString(R.string.at)} $location" else ""
                else -> ""
            }
            summary += when {
                step in NewPlayViewModel.Step.ADD_PLAYERS..NewPlayViewModel.Step.COMMENTS -> " ${context.getString(R.string.with)}..."
                step > NewPlayViewModel.Step.PLAYERS_WIN -> " ${context.getString(R.string.with)} $playerCount ${context.getString(R.string.players)}"
                else -> ""
            }

            return summary.trim()
        }
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"

        fun start(context: Context, gameId: Int, gameName: String) {
            context.startActivity<NewPlayActivity>(
                KEY_GAME_ID to gameId,
                KEY_GAME_NAME to gameName,
            )
        }
    }
}
