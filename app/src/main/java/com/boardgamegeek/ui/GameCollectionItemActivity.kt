package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.palette.graphics.Palette
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.events.CollectionItemChangedEvent
import com.boardgamegeek.events.CollectionItemDeletedEvent
import com.boardgamegeek.events.CollectionItemUpdatedEvent
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.tasks.*
import com.boardgamegeek.tasks.sync.SyncCollectionByGameTask
import com.boardgamegeek.ui.dialog.EditCollectionTextDialogFragment
import com.boardgamegeek.ui.dialog.NumberPadDialogFragment
import com.boardgamegeek.ui.dialog.PrivateInfoDialogFragment
import com.boardgamegeek.ui.model.PrivateInfo
import com.boardgamegeek.util.DialogUtils
import com.boardgamegeek.util.ImageUtils.Callback
import com.boardgamegeek.util.TaskUtils
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.longToast
import org.jetbrains.anko.startActivity
import java.util.concurrent.atomic.AtomicBoolean

class GameCollectionItemActivity : HeroActivity(),
        PrivateInfoDialogFragment.PrivateInfoDialogListener,
        EditCollectionTextDialogFragment.EditCollectionTextDialogListener,
        NumberPadDialogFragment.Listener {
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
    private val isLoadingHeroImage = AtomicBoolean()
    private var imageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            isInEditMode = savedInstanceState.getBoolean(KEY_STATE_IS_IN_EDIT_MODE)
        }

        safelySetTitle()
        changeImage()

        if (savedInstanceState == null) {
            Answers.getInstance().logContentView(ContentViewEvent()
                    .putContentType("GameCollection")
                    .putContentId(collectionId.toString())
                    .putContentName(collectionName))
        }

        fab.setOnClickListener {
            if (isInEditMode) (fragment as GameCollectionItemFragment?)?.syncChanges()
            toggleEditMode()
        }

        fab.ensureShown()
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
                DialogUtils.createDiscardDialog(this, R.string.collection_item, false, false) {
                    TaskUtils.executeAsyncTask(ResetCollectionItemTask(this, internalId))
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
                DialogUtils.createThemedBuilder(this)
                        .setMessage(R.string.are_you_sure_delete_collection_item)
                        .setPositiveButton(R.string.delete) { _, _ ->
                            TaskUtils.executeAsyncTask(DeleteCollectionItemTask(this, internalId))
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: CollectionItemChangedEvent) {
        collectionName = event.collectionName
        collectionYearPublished = event.yearPublished
        thumbnailUrl = event.thumbnailUrl
        heroImageUrl = event.heroImageUrl
        safelySetTitle()
        changeImage()
        GameCollectionRepository(application as BggApplication).maybeRefreshHeroImageUrl(internalId, thumbnailUrl, heroImageUrl, isLoadingHeroImage)
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
            toolbarImage?.loadUrl(url, object : Callback {
                override fun onSuccessfulImageLoad(palette: Palette?) {
                    scrimView.applyDarkScrim()
                    if (palette != null) {
                        (fragment as GameCollectionItemFragment?)?.onPaletteGenerated(palette)
                        fab.colorize(palette.getIconSwatch().rgb)
                    }
                    fab.show()
                }

                override fun onFailedImageLoad() {
                    fab.show()
                }
            })
        }
    }

    override fun onRefresh() {
        when {
            isInEditMode -> updateRefreshStatus(false)
            (fragment as GameCollectionItemFragment?)?.triggerRefresh() == true -> updateRefreshStatus(true)
            else -> updateRefreshStatus(false)
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
            SyncService.sync(this, SyncService.FLAG_SYNC_COLLECTION_UPLOAD)
            isItemUpdated = false
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: CollectionItemUpdatedEvent) {
        if (internalId == event.internalId) {
            isItemUpdated = true
        }
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

    override fun onPrivateInfoChanged(privateInfo: PrivateInfo) {
        TaskUtils.executeAsyncTask(UpdateCollectionItemPrivateInfoTask(this, gameId, collectionId, internalId, privateInfo))
    }

    override fun onEditCollectionText(text: String, textColumn: String, timestampColumn: String) {
        val task = UpdateCollectionItemTextTask(this, gameId, collectionId, internalId, text, textColumn, timestampColumn)
        TaskUtils.executeAsyncTask(task)
    }

    override fun onNumberPadDone(output: Double, requestCode: Int) {
        val task = UpdateCollectionItemRatingTask(this, gameId, collectionId, internalId, output)
        TaskUtils.executeAsyncTask(task)
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
