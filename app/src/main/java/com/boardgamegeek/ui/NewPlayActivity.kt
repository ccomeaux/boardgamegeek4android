package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.extensions.createDiscardDialog
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
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
