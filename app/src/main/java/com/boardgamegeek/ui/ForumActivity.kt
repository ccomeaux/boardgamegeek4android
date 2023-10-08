package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.model.Forum
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.getSerializableCompat
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.ForumsActivity.Companion.startUp
import com.boardgamegeek.ui.GameActivity.Companion.startUp
import com.boardgamegeek.ui.PersonActivity.Companion.startUpForArtist
import com.boardgamegeek.ui.PersonActivity.Companion.startUpForDesigner
import com.boardgamegeek.ui.PersonActivity.Companion.startUpForPublisher
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForumActivity : SimpleSinglePaneActivity() {
    private var forumId = BggContract.INVALID_ID
    private var forumTitle = ""
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""
    private var objectType = Forum.Type.REGION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (forumTitle.isNotEmpty()) {
            if (objectName.isNotEmpty()) {
                supportActionBar?.title = objectName
            }
            supportActionBar?.subtitle = forumTitle
        }
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Forum")
                param(FirebaseAnalytics.Param.ITEM_ID, forumId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, forumTitle)
            }
        }
    }

    override fun readIntent(intent: Intent) {
        forumId = intent.getIntExtra(KEY_FORUM_ID, BggContract.INVALID_ID)
        forumTitle = intent.getStringExtra(KEY_FORUM_TITLE).orEmpty()
        objectId = intent.getIntExtra(KEY_OBJECT_ID, BggContract.INVALID_ID)
        objectType = intent.getSerializableCompat(KEY_OBJECT_TYPE) ?: Forum.Type.REGION
        objectName = intent.getStringExtra(KEY_OBJECT_NAME).orEmpty()
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return ForumFragment.newInstance(forumId, forumTitle, objectId, objectName, objectType)
    }

    override val optionsMenuId = R.menu.view

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                when (objectType) {
                    Forum.Type.REGION -> startUp(this)
                    Forum.Type.GAME -> startUp(this, objectId, objectName)
                    Forum.Type.ARTIST -> startUpForArtist(this, objectId, objectName)
                    Forum.Type.DESIGNER -> startUpForDesigner(this, objectId, objectName)
                    Forum.Type.PUBLISHER -> startUpForPublisher(this, objectId, objectName)
                }
                finish()
            }
            R.id.menu_view -> linkToBgg("forum/$forumId")
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    companion object {
        private const val KEY_FORUM_ID = "FORUM_ID"
        private const val KEY_FORUM_TITLE = "FORUM_TITLE"
        private const val KEY_OBJECT_ID = "OBJECT_ID"
        private const val KEY_OBJECT_NAME = "OBJECT_NAME"
        private const val KEY_OBJECT_TYPE = "OBJECT_TYPE"

        fun start(context: Context, forumId: Int, forumTitle: String, objectId: Int, objectName: String, objectType: Forum.Type) {
            context.startActivity(createIntent(context, forumId, forumTitle, objectId, objectName, objectType))
        }

        fun startUp(context: Context, forumId: Int, forumTitle: String, objectId: Int, objectName: String, objectType: Forum.Type) {
            context.startActivity(createIntent(context, forumId, forumTitle, objectId, objectName, objectType).clearTop())
        }

        private fun createIntent(
            context: Context,
            forumId: Int,
            forumTitle: String,
            objectId: Int,
            objectName: String,
            objectType: Forum.Type
        ): Intent {
            return context.intentFor<ForumActivity>(
                KEY_FORUM_ID to forumId,
                KEY_FORUM_TITLE to forumTitle,
                KEY_OBJECT_ID to objectId,
                KEY_OBJECT_NAME to objectName,
                KEY_OBJECT_TYPE to objectType,
            )
        }
    }
}
