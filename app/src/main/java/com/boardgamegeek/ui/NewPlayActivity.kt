package com.boardgamegeek.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.extensions.cancel
import com.boardgamegeek.extensions.createDiscardDialog
import com.boardgamegeek.extensions.howManyMinutesOld
import com.boardgamegeek.extensions.launchPlayingNotification
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import com.boardgamegeek.ui.widget.SelfUpdatingView
import com.boardgamegeek.util.NotificationUtils
import kotlinx.android.synthetic.main.activity_new_play.*
import org.jetbrains.anko.startActivity

class NewPlayActivity : AppCompatActivity() {
    private val viewModel: NewPlayViewModel by lazy {
        ViewModelProviders.of(this).get(NewPlayViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        val gameName = intent.getStringExtra(KEY_GAME_NAME)

        setContentView(R.layout.activity_new_play)

        setSupportActionBar(toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.menu_cancel)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = gameName
        supportActionBar?.setSubtitle(R.string.title_new_play)

        viewModel.insertedId.observe(this, Observer {
            if ((viewModel.startTime.value ?: 0L) == 0L) {
                this.cancel(NotificationUtils.TAG_PLAY_TIMER, it)
                SyncService.sync(this, SyncService.FLAG_SYNC_PLAYS_UPLOAD)
            } else {
                launchPlayingNotification(it,
                        gameId,
                        gameName,
                        viewModel.location.value ?: "",
                        viewModel.addedPlayers.value?.size ?: 0,
                        viewModel.startTime.value ?: 0L)
                // TODO: add thumbnailUrl, imageUrl, heroImageUrl
            }
            setResult(Activity.RESULT_OK)
            finish()
        })

        viewModel.startTime.observe(this, Observer {
            updateSummary()
        })

        viewModel.location.observe(this, Observer { updateSummary() })

        viewModel.currentStep.observe(this, Observer {
            when (it) {
                NewPlayViewModel.STEP_LOCATION -> {
                    supportFragmentManager
                            .beginTransaction()
                            .add(R.id.fragmentContainer, NewPlayLocationsFragment.newInstance())
                            .commit()
                }
                NewPlayViewModel.STEP_PLAYERS -> {
                    supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, NewPlayAddPlayersFragment.newInstance())
                            .addToBackStack(null)
                            .commit()
                }
                NewPlayViewModel.STEP_COMMENTS -> {
                    supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, NewPlayCommentsFragment.newInstance())
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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                maybeDiscard()
                true
            }
            R.id.timer -> {
                viewModel.startTimer()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 0) {
            maybeDiscard()
        } else {
            super.onBackPressed()
        }
    }

    private fun maybeDiscard() {
        // TODO - improve cancel logic (like detect if there have been changes)
        createDiscardDialog(this, R.string.play, false).show()
    }

    private fun updateSummary() {
        //val summary = createSummary()

        val summaryView = findViewById<PlaySummary>(R.id.summaryView)
        summaryView.step = viewModel.currentStep.value ?: NewPlayViewModel.STEP_LOCATION
        summaryView.startTime = viewModel.startTime.value ?: 0L
        summaryView.location = viewModel.location.value ?: ""
        summaryView.playerCount = viewModel.addedPlayers.value?.size ?: 0

        summaryView.updateText()
    }
    
    class PlaySummary @JvmOverloads constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = android.R.attr.textViewStyle
    ) : SelfUpdatingView(context, attrs, defStyleAttr) {
        var step = NewPlayViewModel.STEP_LOCATION
        var startTime = 0L
        var location = ""
        var playerCount = 0

        override fun updateText() {
            text = createSummary()
            isVisible = text.isNotBlank()
        }

        private fun createSummary(): String {
            var summary = ""
            if (startTime > 0L) {
                val length = startTime.howManyMinutesOld()
                summary += " ${context.getString(R.string.for_)} $length ${context.getString(R.string.minutes_abbr)}"
            }

            summary += when {
                step == NewPlayViewModel.STEP_LOCATION -> " ${context.getString(R.string.at)}"
                step > NewPlayViewModel.STEP_LOCATION -> if (location.isNotBlank()) " ${context.getString(R.string.at)} $location" else ""
                else -> ""
            }
            summary += when {
                step == NewPlayViewModel.STEP_PLAYERS -> " ${context.getString(R.string.with)}"
                step > NewPlayViewModel.STEP_PLAYERS -> " ${context.getString(R.string.with)} $playerCount ${context.getString(R.string.players)}"
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
