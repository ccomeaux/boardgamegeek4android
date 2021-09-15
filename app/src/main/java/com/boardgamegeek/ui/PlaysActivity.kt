package com.boardgamegeek.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.DatePicker
import androidx.activity.viewModels
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.tasks.sync.SyncPlaysByDateTask
import com.boardgamegeek.ui.viewmodel.PlaysViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import java.util.*

class PlaysActivity : SimpleSinglePaneActivity(), DatePickerDialog.OnDateSetListener {
    val viewModel by viewModels<PlaysViewModel>()

    override val optionsMenuId: Int
        get() = R.menu.plays

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Plays")
            }
        }

        viewModel.plays.observe(this, Observer {
            invalidateOptionsMenu()
        })

        viewModel.filterType.observe(this, Observer { type ->
            supportActionBar?.subtitle = when (type) {
                PlaysViewModel.FilterType.PENDING -> getString(R.string.menu_plays_filter_pending)
                PlaysViewModel.FilterType.DIRTY -> getString(R.string.menu_plays_filter_in_progress)
                else -> ""
            }
            invalidateOptionsMenu()
        })

        viewModel.sortType.observe(this, Observer {
            invalidateOptionsMenu()
        })

        viewModel.setFilter(PlaysViewModel.FilterType.ALL)
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return PlaysFragment.newInstance()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        val playCount = viewModel.plays.value?.data?.sumBy { play -> play.quantity } ?: 0
        val sortName = when (viewModel.sortType.value) {
            PlaysViewModel.SortType.DATE -> getString(R.string.menu_plays_sort_date)
            PlaysViewModel.SortType.LOCATION -> getString(R.string.menu_plays_sort_location)
            PlaysViewModel.SortType.GAME -> getString(R.string.menu_plays_sort_game)
            PlaysViewModel.SortType.LENGTH -> getString(R.string.menu_plays_sort_length)
            null -> getString(R.string.text_unknown)
        }
        menu.setActionBarCount(R.id.menu_list_count, playCount, getString(R.string.by_prefix, sortName))

        menu.findItem(when (viewModel.filterType.value) {
            PlaysViewModel.FilterType.DIRTY -> R.id.menu_filter_in_progress
            PlaysViewModel.FilterType.PENDING -> R.id.menu_filter_pending
            PlaysViewModel.FilterType.ALL -> R.id.menu_filter_all
            else -> R.id.menu_filter_all
        })?.isChecked = true
        menu.findItem(when (viewModel.sortType.value) {
            PlaysViewModel.SortType.DATE -> R.id.menu_sort_date
            PlaysViewModel.SortType.GAME -> R.id.menu_sort_game
            PlaysViewModel.SortType.LENGTH -> R.id.menu_sort_length
            PlaysViewModel.SortType.LOCATION -> R.id.menu_sort_location
            else -> R.id.menu_sort_date
        })?.isChecked = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_date -> {
                setSort(PlaysViewModel.SortType.DATE)
                return true
            }
            R.id.menu_sort_location -> {
                setSort(PlaysViewModel.SortType.LOCATION)
                return true
            }
            R.id.menu_sort_game -> {
                setSort(PlaysViewModel.SortType.GAME)
                return true
            }
            R.id.menu_sort_length -> {
                setSort(PlaysViewModel.SortType.LENGTH)
                return true
            }
            R.id.menu_filter_all -> {
                filter(PlaysViewModel.FilterType.ALL)
                return true
            }
            R.id.menu_filter_in_progress -> {
                filter(PlaysViewModel.FilterType.DIRTY)
                return true
            }
            R.id.menu_filter_pending -> {
                filter(PlaysViewModel.FilterType.PENDING)
                return true
            }
            R.id.menu_refresh_on -> {
                val datePickerFragment = DatePickerFragment()
                datePickerFragment.setListener(this)
                datePickerFragment.show(supportFragmentManager, "datePicker")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun setSort(type: PlaysViewModel.SortType) {
        firebaseAnalytics.logEvent("Sort") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Plays")
            param("SortBy", type.toString())
        }
        viewModel.setSort(type)
    }

    fun filter(type: PlaysViewModel.FilterType) {
        firebaseAnalytics.logEvent("Filter") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Plays")
            bundle.putString("FilterBy", type.toString())
        }
        viewModel.setFilter(type)
    }

    class DatePickerFragment : DialogFragment() {
        private var listener: DatePickerDialog.OnDateSetListener? = null

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val calendar = Calendar.getInstance()
            return DatePickerDialog(requireContext(),
                    listener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH))
        }

        fun setListener(listener: DatePickerDialog.OnDateSetListener) {
            this.listener = listener
        }
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        (fragment as PlaysFragment?)?.isSyncing(true)
        SyncPlaysByDateTask(application as BggApplication, year, month, day).executeAsyncTask()
    }
}
