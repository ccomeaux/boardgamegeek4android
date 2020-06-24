package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.palette.graphics.Palette
import com.boardgamegeek.R
import com.boardgamegeek.entities.Status
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.extensions.createDiscardDialog
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.extensions.ensureShown
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.viewmodel.GameCollectionItemViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import kotlinx.android.synthetic.main.activity_hero.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.startActivity

class GameCollectionItemActivity : HeroActivity() {
    private var internalId = BggContract.INVALID_ID.toLong()
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    private var collectionId = BggContract.INVALID_ID
    private var collectionName = ""
    private var thumbnailUrl = ""
    private var heroImageUrl = ""
    private var yearPublished = YEAR_UNKNOWN
    private var collectionYearPublished = YEAR_UNKNOWN
    private var isInEditMode = false
    private var isItemUpdated = false
    private var imageUrl: String? = null

    private val viewModel by viewModels<GameCollectionItemViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            isInEditMode = savedInstanceState.getBoolean(KEY_STATE_IS_IN_EDIT_MODE)
        }

        safelySetTitle()
        changeImage()

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GameCollectionItem")
                param(FirebaseAnalytics.Param.ITEM_ID, collectionId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, collectionName)
            }
        }

        fabOnClickListener = View.OnClickListener {
            if (isInEditMode && isItemUpdated) {
                SyncService.sync(this, SyncService.FLAG_SYNC_COLLECTION_UPLOAD)
                isItemUpdated = false
            }
            toggleEditMode()
        }
        if (collectionId == BggContract.INVALID_ID) fab.hide() else fab.ensureShown()

        viewModel.setId(collectionId)
        viewModel.item.observe(this, Observer { (status, data, _) ->
            swipeRefreshLayout.isRefreshing = (status == Status.REFRESHING)
            if (status == Status.SUCCESS) {
                data?.let { entity ->
                    collectionName = entity.collectionName
                    collectionYearPublished = entity.yearPublished
                    thumbnailUrl = entity.thumbnailUrl
                    heroImageUrl = entity.heroImageUrl
                    safelySetTitle()
                    changeImage()
                }
            }
        })
        viewModel.isEdited.observe(this, Observer { isItemUpdated = it })
    }

    override fun readIntent(intent: Intent) {
        internalId = intent.getLongExtra(KEY_INTERNAL_ID, BggContract.INVALID_ID.toLong())
        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME) ?: ""
        collectionId = intent.getIntExtra(KEY_COLLECTION_ID, BggContract.INVALID_ID)
        collectionName = intent.getStringExtra(KEY_COLLECTION_NAME) ?: ""
        thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL)
        heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL)
        yearPublished = intent.getIntExtra(KEY_YEAR_PUBLISHED, YEAR_UNKNOWN)
        collectionYearPublished = intent.getIntExtra(KEY_COLLECTION_YEAR_PUBLISHED, YEAR_UNKNOWN)
    }

    override fun onResume() {
        super.onResume()
        displayEditMode()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_STATE_IS_IN_EDIT_MODE, isInEditMode)
    }

    override fun onBackPressed() {
        if (isInEditMode) {
            if (isItemUpdated) {
                createDiscardDialog(
                        this@GameCollectionItemActivity,
                        R.string.collection_item,
                        R.string.keep,
                        isNew = false,
                        finishActivity = false) {
                    viewModel.reset()
                    toggleEditMode()
                }.show()
            } else {
                toggleEditMode()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreatePane(): Fragment {
        return GameCollectionItemFragment.newInstance(gameId, collectionId)
    }

    override val optionsMenuId = R.menu.game_collection

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_view_image)?.isEnabled = !imageUrl.isNullOrBlank()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (gameId == BggContract.INVALID_ID) {
                    onBackPressed()
                } else {
                    GameActivity.startUp(this, gameId, gameName, thumbnailUrl, heroImageUrl)
                }
                finish()
                return true
            }
            R.id.menu_view_image -> {
                ImageActivity.start(this, imageUrl)
                return true
            }
            R.id.menu_delete -> {
                this.createThemedBuilder()
                        .setMessage(R.string.are_you_sure_delete_collection_item)
                        .setPositiveButton(R.string.delete) { _, _ ->
                            isItemUpdated = false
                            viewModel.delete()
                            longToast(R.string.msg_collection_item_deleted)
                            SyncService.sync(this, SyncService.FLAG_SYNC_COLLECTION_UPLOAD)
                            finish()
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .setCancelable(true)
                        .show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun safelySetTitle() {
        if (collectionYearPublished == YEAR_UNKNOWN || collectionYearPublished == yearPublished)
            safelySetTitle(collectionName)
        else
            safelySetTitle("$collectionName ($collectionYearPublished)")
    }

    private fun changeImage() {
        val url = if (heroImageUrl.isBlank()) thumbnailUrl else heroImageUrl
        if (url != imageUrl) {
            imageUrl = url
            loadToolbarImage(url)
        }
    }

    override fun onPaletteGenerated(palette: Palette?) {
        viewModel.updateGameColors(palette)
    }

    override fun onRefresh() {
        if (!isInEditMode) viewModel.refresh()
    }

    private fun toggleEditMode() {
        isInEditMode = !isInEditMode
        displayEditMode()
    }

    private fun displayEditMode() {
        enableSwipeRefreshLayout(!isInEditMode)
        (fragment as GameCollectionItemFragment?)?.enableEditMode(isInEditMode)
        setFabImageResource(if (isInEditMode) R.drawable.fab_done else R.drawable.fab_edit)
    }

    companion object {
        private const val KEY_INTERNAL_ID = "_ID"
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_COLLECTION_ID = "COLLECTION_ID"
        private const val KEY_COLLECTION_NAME = "COLLECTION_NAME"
        private const val KEY_THUMBNAIL_URL = "THUMBNAIL_URL"
        private const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        private const val KEY_YEAR_PUBLISHED = "YEAR_PUBLISHED"
        private const val KEY_COLLECTION_YEAR_PUBLISHED = "COLLECTION_YEAR_PUBLISHED"
        private const val KEY_STATE_IS_IN_EDIT_MODE = "STATE_IS_IN_EDIT_MODE"

        fun start(context: Context,
                  internalId: Long,
                  gameId: Int,
                  gameName: String,
                  collectionId: Int,
                  collectionName: String,
                  thumbnailUrl: String,
                  heroImageUrl: String,
                  yearPublished: Int,
                  collectionYearPublished: Int) {
            if (internalId == BggContract.INVALID_ID.toLong()) return
            return context.startActivity<GameCollectionItemActivity>(
                    KEY_INTERNAL_ID to internalId,
                    KEY_GAME_ID to gameId,
                    KEY_GAME_NAME to gameName,
                    KEY_COLLECTION_ID to collectionId,
                    KEY_COLLECTION_NAME to collectionName,
                    KEY_THUMBNAIL_URL to thumbnailUrl,
                    KEY_HERO_IMAGE_URL to heroImageUrl,
                    KEY_YEAR_PUBLISHED to yearPublished,
                    KEY_COLLECTION_YEAR_PUBLISHED to collectionYearPublished
            )
        }
    }
}
