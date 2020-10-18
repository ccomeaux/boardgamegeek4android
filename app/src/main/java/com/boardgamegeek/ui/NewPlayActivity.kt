package com.boardgamegeek.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import com.boardgamegeek.ui.widget.SelfUpdatingView
import com.boardgamegeek.util.ImageUtils
import com.boardgamegeek.util.NotificationUtils
import kotlinx.android.synthetic.main.activity_new_play.*
import org.jetbrains.anko.startActivity

class NewPlayActivity : AppCompatActivity() {
    private var startTime = 0L
    private var gameName = ""
    private val viewModel by viewModels<NewPlayViewModel>()
    private var thumbnailUrl: String? = null
    private var imageUrl: String? = null
    private var heroImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME)

        setContentView(R.layout.activity_new_play)

        setSupportActionBar(toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.menu_cancel)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.insertedId.observe(this, {
            if ((viewModel.startTime.value ?: 0L) == 0L) {
                this.cancel(NotificationUtils.TAG_PLAY_TIMER, it)
                SyncService.sync(this, SyncService.FLAG_SYNC_PLAYS_UPLOAD)
            } else {
                launchPlayingNotification(it,
                        gameId,
                        gameName,
                        viewModel.location.value.orEmpty(),
                        viewModel.addedPlayers.value?.size ?: 0,
                        startTime,
                        thumbnailUrl.orEmpty(),
                        imageUrl.orEmpty(),
                        heroImageUrl.orEmpty())
            }
            setResult(Activity.RESULT_OK)
            finish()
        })

        viewModel.game.observe(this, {
            it.data?.let { entity ->
                gameName = entity.name
                thumbnailUrl = entity.thumbnailUrl
                imageUrl = entity.imageUrl
                heroImageUrl = entity.heroImageUrl

                updateSummary()

                val summaryView = findViewById<PlaySummary>(R.id.summaryView)
                thumbnailView.loadUrl(entity.heroImageUrl, object : ImageUtils.Callback {
                    override fun onSuccessfulImageLoad(palette: Palette?) {
                        summaryView.setBackgroundResource(R.color.black_overlay_light)
                    }

                    override fun onFailedImageLoad() {
                        summaryView.setBackgroundResource(0)
                    }
                })
            }
        })

        viewModel.startTime.observe(this, {
            startTime = viewModel.startTime.value ?: 0L
            updateSummary()
            invalidateOptionsMenu()
        })

        viewModel.length.observe(this, { updateSummary() })

        viewModel.location.observe(this, { updateSummary() })

        viewModel.currentStep.observe(this, {
            when (it) {
                NewPlayViewModel.Step.LOCATION, null -> {
                    supportFragmentManager
                            .beginTransaction()
                            .add(R.id.fragmentContainer, NewPlayLocationsFragment())
                            .commit()
                }
                NewPlayViewModel.Step.PLAYERS -> {
                    supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, NewPlayAddPlayersFragment())
                            .addToBackStack(null)
                            .commit()
                }
                NewPlayViewModel.Step.PLAYERS_COLOR -> {
                    supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, NewPlayPlayerColorsFragment())
                            .addToBackStack(null)
                            .commit()
                }
                NewPlayViewModel.Step.PLAYERS_SORT -> {
                    supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, NewPlayPlayerSortFragment())
                            .addToBackStack(null)
                            .commit()
                }
                NewPlayViewModel.Step.PLAYERS_NEW -> {
                    supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, NewPlayPlayerIsNewFragment())
                            .addToBackStack(null)
                            .commit()
                }
                NewPlayViewModel.Step.PLAYERS_WIN -> {
                    supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, NewPlayPlayerWinFragment())
                            .addToBackStack(null)
                            .commit()
                }
                NewPlayViewModel.Step.COMMENTS -> {
                    supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, NewPlayCommentsFragment())
                            .addToBackStack(null)
                            .commit()
                }
            }
            updateSummary()
        })

        viewModel.setGame(gameId, gameName)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.new_play, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.timer)?.setIcon(if (startTime > 0L) R.drawable.menu_timer_off else R.drawable.menu_timer)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
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
                viewModel.comments.isNotBlank()) {
            createDiscardDialog(this, R.string.play, isNew = false).show()
            true
        } else false
    }

    private fun updateSummary() {
        val summaryView = findViewById<PlaySummary>(R.id.summaryView)
        summaryView.gameName = gameName
        summaryView.step = viewModel.currentStep.value ?: NewPlayViewModel.Step.LOCATION
        summaryView.startTime = startTime
        summaryView.length = viewModel.length.value ?: 0
        summaryView.location = viewModel.location.value ?: ""
        summaryView.playerCount = viewModel.addedPlayers.value?.size ?: 0

        summaryView.updateText()
    }

    class PlaySummary @JvmOverloads constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = android.R.attr.textViewStyle
    ) : SelfUpdatingView(context, attrs, defStyleAttr) {
        var gameName = ""
        var step = NewPlayViewModel.Step.LOCATION
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
            if (startTime > 0L || length > 0) {
                val totalLength = length + if (startTime > 0L) startTime.howManyMinutesOld() else 0
                summary += " ${context.getString(R.string.for_)} $totalLength ${context.getString(R.string.minutes_abbr)}"
            }

            summary += when {
                step == NewPlayViewModel.Step.LOCATION -> " ${context.getString(R.string.at)}"
                step > NewPlayViewModel.Step.LOCATION -> if (location.isNotBlank()) " ${context.getString(R.string.at)} $location" else ""
                else -> ""
            }
            summary += when {
                step == NewPlayViewModel.Step.PLAYERS || step == NewPlayViewModel.Step.PLAYERS_COLOR || step == NewPlayViewModel.Step.PLAYERS_SORT -> " ${context.getString(R.string.with)}"
                step > NewPlayViewModel.Step.PLAYERS -> " ${context.getString(R.string.with)} $playerCount ${context.getString(R.string.players)}"
                else -> ""
            }

            return summary.trim()
        }
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"

        @JvmStatic
        fun start(context: Context, gameId: Int, gameName: String) {
            context.startActivity<NewPlayActivity>(
                    KEY_GAME_ID to gameId,
                    KEY_GAME_NAME to gameName
            )
        }
    }
}
