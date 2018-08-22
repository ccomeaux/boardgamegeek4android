package com.boardgamegeek.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.graphics.Palette
import android.view.Menu
import android.view.MenuItem
import butterknife.OnClick
import com.boardgamegeek.R
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.events.CollectionItemChangedEvent
import com.boardgamegeek.events.CollectionItemDeletedEvent
import com.boardgamegeek.events.CollectionItemUpdatedEvent
import com.boardgamegeek.extensions.loadUrl
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.tasks.DeleteCollectionItemTask
import com.boardgamegeek.tasks.ResetCollectionItemTask
import com.boardgamegeek.tasks.sync.SyncCollectionByGameTask
import com.boardgamegeek.util.*
import com.boardgamegeek.util.ImageUtils.Callback
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.act
import org.jetbrains.anko.ctx
import org.jetbrains.anko.longToast
import org.jetbrains.anko.startActivity

class GameCollectionItemActivity : HeroActivity() {
    private var internalId: Long = BggContract.INVALID_ID.toLong()
    private var gameId: Int = BggContract.INVALID_ID
    private var gameName: String = ""
    private var collectionId: Int = BggContract.INVALID_ID
    private var collectionName: String = ""
    private var imageUrl: String = ""
    private var thumbnailUrl: String = ""
    private var heroImageUrl: String = ""
    private var yearPublished: Int = 0
    private var collectionYearPublished: Int = YEAR_UNKNOWN
    private var isInEditMode: Boolean = false
    private var isItemUpdated: Boolean = false

    override val optionsMenuId = R.menu.game_collection

    private val imageLoadCallback = object : Callback {
        override fun onSuccessfulImageLoad(palette: Palette?) {
            ScrimUtils.applyDarkScrim(scrimView)
            if (palette != null) {
                (fragment as GameCollectionItemFragment).onPaletteGenerated(palette)
                PresentationUtils.colorFab(fab, PaletteUtils.getIconSwatch(palette).rgb)
                fab.show()
            }

            Handler().post {
                val url = toolbarImage.getTag(R.id.url) as String?
                if (!url.isNullOrEmpty() &&
                        url != imageUrl &&
                        url != thumbnailUrl &&
                        url != heroImageUrl) {
                    val values = ContentValues()
                    values.put(Collection.COLLECTION_HERO_IMAGE_URL, url)
                    contentResolver.update(Collection.CONTENT_URI,
                            values,
                            "${Collection.COLLECTION_ID}=?",
                            arrayOf(collectionId.toString()))
                }
            }
        }

        override fun onFailedImageLoad() {
            fab.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            isInEditMode = savedInstanceState.getBoolean(KEY_STATE_IS_IN_EDIT_MODE)
        }

        safelySetTitle()

        if (savedInstanceState == null) {
            Answers.getInstance().logContentView(ContentViewEvent()
                    .putContentType("GameCollection")
                    .putContentId(collectionId.toString())
                    .putContentName(collectionName))
        }
        PresentationUtils.ensureFabIsShown(fab)
    }

    override fun readIntent(intent: Intent) {
        internalId = intent.getLongExtra(KEY_INTERNAL_ID, BggContract.INVALID_ID.toLong())
        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME) ?: ""
        collectionId = intent.getIntExtra(KEY_COLLECTION_ID, BggContract.INVALID_ID)
        collectionName = intent.getStringExtra(KEY_COLLECTION_NAME) ?: ""
        imageUrl = intent.getStringExtra(KEY_IMAGE_URL)
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
                DialogUtils.createDiscardDialog(act, R.string.collection_item, false, false) {
                    TaskUtils.executeAsyncTask(ResetCollectionItemTask(ctx, internalId))
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

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_view_image)?.isEnabled = imageUrl.isNotBlank()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (gameId == BggContract.INVALID_ID) {
                    onBackPressed()
                } else {
                    GameActivity.startUp(ctx, gameId, gameName, thumbnailUrl, heroImageUrl)
                }
                finish()
                return true
            }
            R.id.menu_view_image -> {
                ImageActivity.start(ctx, imageUrl)
                return true
            }
            R.id.menu_delete -> {
                DialogUtils.createThemedBuilder(ctx)
                        .setMessage(R.string.are_you_sure_delete_collection_item)
                        .setPositiveButton(R.string.delete) { _, _ ->
                            TaskUtils.executeAsyncTask(DeleteCollectionItemTask(ctx, internalId))
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: CollectionItemChangedEvent) {
        collectionName = event.collectionName
        collectionYearPublished = event.yearPublished
        if (event.imageUrl != imageUrl ||
                event.thumbnailUrl != thumbnailUrl ||
                event.heroImageUrl != heroImageUrl) {
            imageUrl = event.imageUrl
            thumbnailUrl = event.thumbnailUrl
            heroImageUrl = event.heroImageUrl
            toolbarImage.loadUrl(imageUrl, imageLoadCallback)
        } else {
            imageUrl = event.imageUrl
            thumbnailUrl = event.thumbnailUrl
            heroImageUrl = event.heroImageUrl
        }
        safelySetTitle()
    }

    override fun onRefresh() {
        if (isInEditMode) {
            updateRefreshStatus(false)
        } else if ((fragment as GameCollectionItemFragment?)?.triggerRefresh() == true) {
            updateRefreshStatus(true)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: SyncCollectionByGameTask.CompletedEvent) {
        if (event.gameId == gameId) {
            updateRefreshStatus(false)
            if (event.errorMessage.isNotBlank()) {
                longToast(event.errorMessage)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: CollectionItemDeletedEvent) {
        if (internalId == event.internalId) {
            longToast(R.string.msg_collection_item_deleted)
            SyncService.sync(ctx, SyncService.FLAG_SYNC_COLLECTION_UPLOAD)
            isItemUpdated = false
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: CollectionItemUpdatedEvent) {
        if (internalId == event.internalId) {
            isItemUpdated = true
        }
    }

    @OnClick(R.id.fab)
    fun onFabClicked() {
        if (isInEditMode) (fragment as GameCollectionItemFragment?)?.syncChanges()
        toggleEditMode()
    }

    private fun toggleEditMode() {
        isInEditMode = !isInEditMode
        displayEditMode()
    }

    private fun displayEditMode() {
        swipeRefreshLayout.isEnabled = !isInEditMode
        (fragment as GameCollectionItemFragment?)?.enableEditMode(isInEditMode)
        fab.setImageResource(if (isInEditMode) R.drawable.fab_done else R.drawable.fab_edit)
    }

    companion object {
        private const val KEY_INTERNAL_ID = "_ID"
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_COLLECTION_ID = "COLLECTION_ID"
        private const val KEY_COLLECTION_NAME = "COLLECTION_NAME"
        private const val KEY_IMAGE_URL = "IMAGE_URL"
        private const val KEY_YEAR_PUBLISHED = "YEAR_PUBLISHED"
        private const val KEY_COLLECTION_YEAR_PUBLISHED = "COLLECTION_YEAR_PUBLISHED"
        private const val KEY_STATE_IS_IN_EDIT_MODE = "KEY_STATE_IS_IN_EDIT_MODE"

        fun start(context: Context, internalId: Long, gameId: Int, gameName: String, collectionId: Int, collectionName: String, imageUrl: String, yearPublished: Int, collectionYearPublished: Int) {
            if (internalId == BggContract.INVALID_ID.toLong()) return
            return context.startActivity<GameCollectionItemActivity>(
                    KEY_INTERNAL_ID to internalId,
                    KEY_GAME_ID to gameId,
                    KEY_GAME_NAME to gameName,
                    KEY_COLLECTION_ID to collectionId,
                    KEY_COLLECTION_NAME to collectionName,
                    KEY_IMAGE_URL to imageUrl,
                    KEY_YEAR_PUBLISHED to yearPublished,
                    KEY_COLLECTION_YEAR_PUBLISHED to collectionYearPublished
            )
        }
    }
}
