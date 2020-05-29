package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.tasks.RenameLocationTask
import com.boardgamegeek.ui.dialog.EditLocationNameDialogFragment
import com.boardgamegeek.ui.viewmodel.PlaysViewModel
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.startActivity

class LocationActivity : SimpleSinglePaneActivity() {
    private val viewModel by viewModels<PlaysViewModel>()

    private var locationName = ""
    private var playCount = -1

    override val optionsMenuId: Int
        get() = R.menu.location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSubtitle()

        if (savedInstanceState == null) {
            Answers.getInstance().logContentView(ContentViewEvent()
                    .putContentType("Location")
                    .putContentName(locationName))
        }

        viewModel.plays.observe(this, Observer {
            playCount = it.data?.sumBy { play -> play.quantity } ?: 0
            invalidateOptionsMenu()
        })
    }

    override fun readIntent(intent: Intent) {
        locationName = intent.getStringExtra(KEY_LOCATION_NAME) ?: ""
        viewModel.setLocation(locationName)
    }

    private fun setSubtitle() {
        supportActionBar?.subtitle = if (locationName.isBlank()) getString(R.string.no_location) else locationName
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RenameLocationTask.Event) {
        locationName = event.locationName
        intent.putExtra(KEY_LOCATION_NAME, locationName)
        setSubtitle()
        viewModel.setLocation(locationName)
        // recreate fragment to load the list with the new location
        recreateFragment()

        if (event.message.isNotBlank()) {
            rootContainer?.snackbar(event.message)
        }
    }

    companion object {
        private const val KEY_LOCATION_NAME = "LOCATION_NAME"

        fun start(context: Context, locationName: String) {
            context.startActivity<LocationActivity>(
                    KEY_LOCATION_NAME to locationName
            )
        }
    }
}
