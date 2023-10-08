package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.palette.graphics.Palette
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameCollectionItemViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameCollectionItemActivity : HeroActivity() {
    private var internalId = BggContract.INVALID_ID.toLong()
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    private var collectionId = BggContract.INVALID_ID
    private var collectionName = ""
    private var thumbnailUrl = ""
    private var heroImageUrl = ""
    private var yearPublished = CollectionItem.YEAR_UNKNOWN
    private var collectionYearPublished = CollectionItem.YEAR_UNKNOWN
    private var isInEditMode = false
    private var isItemUpdated = false
    private var imageUrl: String? = null

    private val viewModel by viewModels<GameCollectionItemViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) {
            if (isInEditMode) {
                if (isItemUpdated) {
                    this@GameCollectionItemActivity.createDiscardDialog(
                        R.string.collection_item,
                        R.string.keep,
                        isNew = false,
                        finishActivity = false
                    ) {
                        viewModel.reset()
                        viewModel.disableEditMode()
                    }.show()
                } else {
                    viewModel.disableEditMode()
                }
            } else {
                finish()
            }
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
            if (isInEditMode) {
                if (isItemUpdated) {
                    viewModel.update()
                } else {
                    viewModel.disableEditMode()
                }
            } else {
                viewModel.enableEditMode()
            }
        }
        if (collectionId == BggContract.INVALID_ID) binding.fab.hide() else binding.fab.ensureShown()

        viewModel.setInternalId(internalId)
        viewModel.item.observe(this) { resource ->
            binding.swipeRefreshLayout.isRefreshing = (resource?.status == Status.REFRESHING)
            if (resource?.status == Status.SUCCESS) {
                resource.data?.let { collectionItem ->
                    collectionName = collectionItem.collectionName
                    collectionYearPublished = collectionItem.collectionYearPublished
                    thumbnailUrl = collectionItem.thumbnailUrl
                    heroImageUrl = collectionItem.heroImageUrl
                    safelySetTitle()
                    changeImage()
                }
            }
        }
        viewModel.isEditMode.observe(this) {
            isInEditMode = it
            enableSwipeRefreshLayout(!it)
            setFabImageResource(if (it) R.drawable.ic_baseline_check_24 else R.drawable.ic_baseline_edit_24)
        }
        viewModel.isEdited.observe(this) { isItemUpdated = it }
    }

    override fun readIntent(intent: Intent) {
        internalId = intent.getLongExtra(KEY_INTERNAL_ID, BggContract.INVALID_ID.toLong())
        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()
        collectionId = intent.getIntExtra(KEY_COLLECTION_ID, BggContract.INVALID_ID)
        collectionName = intent.getStringExtra(KEY_COLLECTION_NAME).orEmpty()
        thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL).orEmpty()
        heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL).orEmpty()
        yearPublished = intent.getIntExtra(KEY_YEAR_PUBLISHED, CollectionItem.YEAR_UNKNOWN)
        collectionYearPublished = intent.getIntExtra(KEY_COLLECTION_YEAR_PUBLISHED, CollectionItem.YEAR_UNKNOWN)
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
                if (gameId != BggContract.INVALID_ID) {
                    GameActivity.startUp(this, gameId, gameName, thumbnailUrl, heroImageUrl)
                }
                finish()
            }
            R.id.menu_view_image -> {
                ImageActivity.start(this, imageUrl)
            }
            R.id.menu_delete -> {
                this.createThemedBuilder()
                    .setMessage(R.string.are_you_sure_delete_collection_item)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        isItemUpdated = false
                        viewModel.delete()
                        longToast(R.string.msg_collection_item_deleted)
                        finish()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(true)
                    .show()
            }
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun safelySetTitle() {
        if (collectionYearPublished == CollectionItem.YEAR_UNKNOWN || collectionYearPublished == yearPublished)
            safelySetTitle(collectionName)
        else
            safelySetTitle("$collectionName ($collectionYearPublished)")
    }

    private fun changeImage() {
        val url = heroImageUrl.ifBlank { thumbnailUrl }
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

        fun start(
            context: Context,
            internalId: Long,
            gameId: Int,
            gameName: String,
            collectionId: Int,
            collectionName: String,
            thumbnailUrl: String,
            heroImageUrl: String,
            yearPublished: Int,
            collectionYearPublished: Int
        ) {
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
                KEY_COLLECTION_YEAR_PUBLISHED to collectionYearPublished,
            )
        }
    }
}
