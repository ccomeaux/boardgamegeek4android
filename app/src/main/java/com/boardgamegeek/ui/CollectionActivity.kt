package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatSpinner
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.extensions.CollectionView
import com.boardgamegeek.extensions.get
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.CollectionViewAdapter
import com.boardgamegeek.ui.dialog.CollectionFilterDialogFragment
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import org.jetbrains.anko.*

class CollectionActivity : TopLevelSinglePaneActivity(), CollectionFilterDialogFragment.Listener {
    private var viewId: Long = 0
    private var isCreatingShortcut = false
    private var changingGamePlayId: Long = BggContract.INVALID_ID.toLong()
    private var hideNavigation = false

    private val viewModel by viewModels<CollectionViewViewModel>()

    private val adapter: CollectionViewAdapter by lazy {
        CollectionViewAdapter(this@CollectionActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.let {
            if (hideNavigation) {
                it.setHomeButtonEnabled(false)
                it.setDisplayHomeAsUpEnabled(false)
                it.setTitle(R.string.app_name)
            } else {
                it.setDisplayShowTitleEnabled(false)
                it.setDisplayShowCustomEnabled(true)
                it.setCustomView(R.layout.actionbar_collection)
            }
        }
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Collection")
            }
        }

        viewModel.selectedViewId.observe(this, { id: Long -> viewId = id })
        if (savedInstanceState == null) {
            if (hideNavigation) {
                viewModel.selectView(CollectionView.DEFAULT_DEFAULT_ID)
            } else {
                val defaultId = defaultSharedPreferences[CollectionView.PREFERENCES_KEY_DEFAULT_ID, CollectionView.DEFAULT_DEFAULT_ID]
                        ?: CollectionView.DEFAULT_DEFAULT_ID
                val viewId = intent.getLongExtra(KEY_VIEW_ID, defaultId)
                viewModel.selectView(viewId)
            }
        }
    }

    override fun readIntent(intent: Intent) {
        isCreatingShortcut = Intent.ACTION_CREATE_SHORTCUT == getIntent().action
        changingGamePlayId = getIntent().getLongExtra(KEY_CHANGING_GAME_PLAY_ID, BggContract.INVALID_ID.toLong())
        hideNavigation = isCreatingShortcut || changingGamePlayId != BggContract.INVALID_ID.toLong()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        findViewById<AppCompatSpinner>(R.id.menu_spinner)?.let {
            it.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                        param(FirebaseAnalytics.Param.CONTENT_TYPE, "CollectionView")
                    }
                    viewModel.selectView(id)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) { // Do nothing
                }
            }
            it.adapter = adapter
            viewModel.views.observe(this, { collectionViews: List<CollectionViewEntity?> ->
                if (collectionViews.isNotEmpty()) {
                    adapter.clear()
                    adapter.addAll(collectionViews)
                }
                it.setSelection(adapter.findIndexOf(viewId))
            })
        }
        return true
    }

    override val navigationItemId: Int = R.id.collection

    override val optionsMenuId: Int = R.menu.search

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (hideNavigation) {
            menu.findItem(R.id.menu_search)?.isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_search) {
            startActivity(intentFor<SearchResultsActivity>())
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onCreatePane(): Fragment {
        return if (changingGamePlayId != BggContract.INVALID_ID.toLong()) {
            CollectionFragment.newInstanceForPlayGameChange(changingGamePlayId)
        } else {
            CollectionFragment.newInstance(isCreatingShortcut)
        }
    }

    override fun onFilterSelected(filterType: Int) {
        (fragment as CollectionFragment).launchFilterDialog(filterType)
    }

    companion object {
        private const val KEY_VIEW_ID = "VIEW_ID"
        private const val KEY_CHANGING_GAME_PLAY_ID = "KEY_CHANGING_GAME_PLAY_ID"

        fun createIntentAsShortcut(context: Context, viewId: Long): Intent {
            return context.intentFor<CollectionActivity>(KEY_VIEW_ID to viewId)
                    .clearTask()
                    .newTask()
                    .apply {
                        action = Intent.ACTION_VIEW
                    }
        }

        fun startForGameChange(context: Context, playId: Long) {
            context.startActivity<CollectionActivity>(KEY_CHANGING_GAME_PLAY_ID to playId)
        }
    }
}