package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.boardgamegeek.provider.BggContract
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import org.jetbrains.anko.intentFor

class PlayActivity : SimpleSinglePaneActivity() {
    private var internalId = BggContract.INVALID_ID.toLong()
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    private var thumbnailUrl = ""
    private var imageUrl = ""
    private var heroImageUrl = ""
    private var customPlayerSort = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
                param(FirebaseAnalytics.Param.ITEM_ID, internalId.toString())
            }
        }
    }

    override fun readIntent(intent: Intent) {
        internalId = intent.getLongExtra(KEY_ID, BggContract.INVALID_ID.toLong())
        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()
        thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL).orEmpty()
        imageUrl = intent.getStringExtra(KEY_IMAGE_URL).orEmpty()
        heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL).orEmpty()
        customPlayerSort = intent.getBooleanExtra(KEY_CUSTOM_PLAYER_SORT, false)

        if (internalId == BggContract.INVALID_ID.toLong()) finish()
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return PlayFragment.newInstance(internalId, gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl, customPlayerSort)
    }

    // TODO finish activity when delete is successful

    companion object {
        private const val KEY_ID = "ID"
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_IMAGE_URL = "IMAGE_URL"
        private const val KEY_THUMBNAIL_URL = "THUMBNAIL_URL"
        private const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        private const val KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT"

        fun start(context: Context, internalId: Long, gameId: Int, gameName: String?, thumbnailUrl: String?, imageUrl: String?, heroImageUrl: String?, customPlayerSort: Boolean) {
            context.startActivity(createIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, customPlayerSort))
        }

        @JvmStatic
        fun createIntent(context: Context, internalId: Long, gameId: Int, gameName: String?, thumbnailUrl: String?, imageUrl: String?, heroImageUrl: String?, customPlayerSort: Boolean): Intent {
            return context.intentFor<PlayActivity>(
                    KEY_ID to internalId,
                    KEY_GAME_ID to gameId,
                    KEY_GAME_NAME to gameName,
                    KEY_THUMBNAIL_URL to thumbnailUrl,
                    KEY_IMAGE_URL to imageUrl,
                    KEY_HERO_IMAGE_URL to heroImageUrl,
                    KEY_CUSTOM_PLAYER_SORT to customPlayerSort,
            )
        }
    }
}