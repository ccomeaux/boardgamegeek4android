package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.longSnackbar
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.ui.dialog.EditLocationNameDialogFragment
import com.boardgamegeek.ui.viewmodel.PlaysViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocationActivity : SimpleSinglePaneActivity() {
    private val viewModel by viewModels<PlaysViewModel>()

    private var locationName = ""
    private var playCount = -1
    private var snackbar: Snackbar? = null

    override val optionsMenuId: Int
        get() = R.menu.location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSubtitle()

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Location")
                param(FirebaseAnalytics.Param.ITEM_NAME, locationName)
            }
        }

        viewModel.location.observe(this) {
            locationName = it
            intent.putExtra(KEY_LOCATION_NAME, locationName)
            setSubtitle()
        }
        viewModel.plays.observe(this) {
            playCount = it?.sumOf { play -> play.quantity } ?: 0
            invalidateOptionsMenu()
        }
        viewModel.updateMessage.observe(this) {
            it.getContentIfNotHandled()?.let { content ->
                if (content.isBlank()) {
                    snackbar?.dismiss()
                } else {
                    snackbar = rootContainer?.longSnackbar(content)
                }
            }
        }
        viewModel.setLocation(locationName)
    }

    override fun readIntent(intent: Intent) {
        locationName = intent.getStringExtra(KEY_LOCATION_NAME).orEmpty()
    }

    private fun setSubtitle() {
        supportActionBar?.subtitle = locationName.ifBlank { getString(R.string.no_location) }
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return PlaysFragment.newInstanceForLocation()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.setActionBarCount(R.id.menu_list_count, playCount)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_edit) {
            showAndSurvive(EditLocationNameDialogFragment.newInstance(locationName))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_LOCATION_NAME = "LOCATION_NAME"

        fun start(context: Context, locationName: String) {
            context.startActivity<LocationActivity>(KEY_LOCATION_NAME to locationName)
        }
    }
}
