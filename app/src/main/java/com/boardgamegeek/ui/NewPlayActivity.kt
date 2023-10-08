package com.boardgamegeek.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.palette.graphics.Palette
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.boardgamegeek.R
import com.boardgamegeek.databinding.ActivityNewPlayBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import com.boardgamegeek.ui.widget.SelfUpdatingView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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

        onBackPressedDispatcher.addCallback(this) {
            if (binding.pager.currentItem > 0)
                viewModel.previousPage()
            else
                if (!maybeDiscard()) finish()
        }

        val gameId = intent.getIntExtra(KEY_GAME_ID, INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()

        binding = ActivityNewPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_baseline_clear_24)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.pager.adapter = WizardAdapter(this)
        binding.pager.isUserInputEnabled = false

        viewModel.insertedId.observe(this) {
            if ((viewModel.startTime.value ?: 0L) == 0L) {
                this.cancelNotification(TAG_PLAY_TIMER, it)
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
            it?.let { game ->
                gameName = game.name
                thumbnailUrl = game.thumbnailUrl
                imageUrl = game.imageUrl
                heroImageUrl = game.heroImageUrl

                updateSummary()

                val summaryView = findViewById<PlaySummary>(R.id.summaryView)
                binding.thumbnailView.loadImage(game.heroImageUrl, callback = object : ImageLoadCallback {
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
            binding.pager.currentItem = it?.let {
                when (it) {
                    NewPlayViewModel.Step.DATE -> 0
                    NewPlayViewModel.Step.LOCATION -> 1
                    NewPlayViewModel.Step.ADD_PLAYERS -> 2
                    NewPlayViewModel.Step.PLAYERS_COLOR -> 3
                    NewPlayViewModel.Step.PLAYERS_SORT -> 4
                    NewPlayViewModel.Step.PLAYERS_NEW -> 5
                    NewPlayViewModel.Step.PLAYERS_WIN -> 6
                    NewPlayViewModel.Step.COMMENTS -> 7
                }
            } ?: 0
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

    private inner class WizardAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 8

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> NewPlayDateFragment()
                1 -> NewPlayLocationsFragment()
                2 -> NewPlayAddPlayersFragment()
                3 -> NewPlayPlayerColorsFragment()
                4 -> NewPlayPlayerSortFragment()
                5 -> NewPlayPlayerIsNewFragment()
                6 -> NewPlayPlayerWinFragment()
                7 -> NewPlayCommentsFragment()
                else -> ErrorFragment()
            }
        }
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
            } else if (step > NewPlayViewModel.Step.DATE) {
                date?.let { summary += " ${context.getString(R.string.on)} ${it.asPastDaySpan(context)}" }
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
