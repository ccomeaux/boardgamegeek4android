package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.extensions.createBggUri
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.share
import com.boardgamegeek.provider.BggContract
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import org.jetbrains.anko.clearTop
import org.jetbrains.anko.intentFor

class ThreadActivity : SimpleSinglePaneActivity() {
    private var threadId = BggContract.INVALID_ID
    private var threadSubject = ""
    private var forumId = BggContract.INVALID_ID
    private var forumTitle: String = ""
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""
    private var objectType = ForumEntity.ForumType.REGION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (objectName.isBlank()) {
            supportActionBar?.title = forumTitle
            supportActionBar?.subtitle = threadSubject
        } else {
            supportActionBar?.title = "$threadSubject - $forumTitle"
            supportActionBar?.subtitle = objectName
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Thread")
                param(FirebaseAnalytics.Param.ITEM_ID, threadId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, threadSubject)
            }
        }
    }

    override fun readIntent(intent: Intent) {
        threadId = intent.getIntExtra(KEY_THREAD_ID, BggContract.INVALID_ID)
        threadSubject = intent.getStringExtra(KEY_THREAD_SUBJECT) ?: ""
        forumId = intent.getIntExtra(KEY_FORUM_ID, BggContract.INVALID_ID)
        forumTitle = intent.getStringExtra(KEY_FORUM_TITLE) ?: ""
        objectId = intent.getIntExtra(KEY_OBJECT_ID, BggContract.INVALID_ID)
        objectName = intent.getStringExtra(KEY_OBJECT_NAME) ?: ""
        objectType = intent.getSerializableExtra(KEY_OBJECT_TYPE) as ForumEntity.ForumType
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return ThreadFragment.newInstance(threadId, forumId, forumTitle, objectId, objectName, objectType)
    }

    override val optionsMenuId = R.menu.view_share

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                ForumActivity.startUp(this, forumId, forumTitle, objectId, objectName, objectType)
                finish()
                return true
            }
            R.id.menu_view -> {
                linkToBgg("thread", threadId)
                return true
            }
            R.id.menu_share -> {
                val description = if (objectName.isBlank())
                    String.format(getString(R.string.share_thread_text), threadSubject, forumTitle)
                else
                    String.format(getString(R.string.share_thread_game_text), threadSubject, forumTitle, objectName)
                val link = createBggUri("thread", threadId).toString()
                share(getString(R.string.share_thread_subject), """
                    $description
                    
                    $link
                    """.trimIndent(), R.string.title_share)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "Thread")
                    param(FirebaseAnalytics.Param.ITEM_ID, threadId.toString())
                    param(FirebaseAnalytics.Param.ITEM_NAME, if (objectName.isBlank()) "$forumTitle | $threadSubject" else "$objectName | $forumTitle | $threadSubject")
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_FORUM_ID = "FORUM_ID"
        private const val KEY_FORUM_TITLE = "FORUM_TITLE"
        private const val KEY_OBJECT_ID = "OBJECT_ID"
        private const val KEY_OBJECT_NAME = "OBJECT_NAME"
        private const val KEY_OBJECT_TYPE = "OBJECT_TYPE"
        private const val KEY_THREAD_ID = "THREAD_ID"
        private const val KEY_THREAD_SUBJECT = "THREAD_SUBJECT"

        fun start(context: Context, threadId: Int, threadSubject: String, forumId: Int, forumTitle: String, objectId: Int, objectName: String, objectType: ForumEntity.ForumType) {
            context.startActivity(createIntent(context, threadId, threadSubject, forumId, forumTitle, objectId, objectName, objectType))
        }

        fun startUp(context: Context, threadId: Int, threadSubject: String, forumId: Int, forumTitle: String, objectId: Int, objectName: String, objectType: ForumEntity.ForumType) {
            context.startActivity(createIntent(context, threadId, threadSubject, forumId, forumTitle, objectId, objectName, objectType).clearTop())
        }

        private fun createIntent(context: Context, threadId: Int, threadSubject: String, forumId: Int, forumTitle: String, objectId: Int, objectName: String, objectType: ForumEntity.ForumType): Intent {
            return context.intentFor<ThreadActivity>(
                    KEY_THREAD_ID to threadId,
                    KEY_THREAD_SUBJECT to threadSubject,
                    KEY_FORUM_ID to forumId,
                    KEY_FORUM_TITLE to forumTitle,
                    KEY_OBJECT_ID to objectId,
                    KEY_OBJECT_NAME to objectName,
                    KEY_OBJECT_TYPE to objectType
            )
        }
    }
}