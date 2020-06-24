package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.entities.ArticleEntity
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.extensions.link
import com.boardgamegeek.extensions.share
import com.boardgamegeek.provider.BggContract
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import org.jetbrains.anko.startActivity
import timber.log.Timber

class ArticleActivity : SimpleSinglePaneActivity() {
    private var threadId = BggContract.INVALID_ID
    private var threadSubject = ""
    private var forumId = BggContract.INVALID_ID
    private var forumTitle = ""
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""
    private var objectType = ForumEntity.ForumType.REGION
    private var article = ArticleEntity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (article.id == BggContract.INVALID_ID) {
            Timber.w("Invalid article ID")
            finish()
        }

        if (objectName.isBlank()) {
            supportActionBar?.title = forumTitle
            supportActionBar?.subtitle = threadSubject
        } else {
            supportActionBar?.title = "$threadSubject - $forumTitle"
            supportActionBar?.subtitle = objectName
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Article")
                param(FirebaseAnalytics.Param.ITEM_ID, article.id.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, threadSubject)
            }
        }
    }

    override fun readIntent(intent: Intent) {
        threadId = intent.getIntExtra(KEY_THREAD_ID, BggContract.INVALID_ID)
        threadSubject = intent.getStringExtra(KEY_THREAD_SUBJECT) ?: ""
        forumId = intent.getIntExtra(KEY_FORUM_ID, BggContract.INVALID_ID)
        forumTitle = intent.getStringExtra(KEY_FORUM_TITLE)
        objectId = intent.getIntExtra(KEY_OBJECT_ID, BggContract.INVALID_ID)
        objectName = intent.getStringExtra(KEY_OBJECT_NAME) ?: ""
        objectType = intent.getSerializableExtra(KEY_OBJECT_TYPE) as ForumEntity.ForumType
        article = intent.getParcelableExtra(KEY_ARTICLE) ?: ArticleEntity()
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return ArticleFragment.newInstance(article)
    }

    override val optionsMenuId = R.menu.view_share

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                ThreadActivity.startUp(this, threadId, threadSubject, forumId, forumTitle, objectId, objectName, objectType)
                finish()
                return true
            }
            R.id.menu_view -> {
                link(article.link)
                return true
            }
            R.id.menu_share -> {
                val description = if (objectName.isEmpty())
                    String.format(getString(R.string.share_thread_article_text), threadSubject, forumTitle)
                else
                    String.format(getString(R.string.share_thread_article_object_text), threadSubject, forumTitle, objectName)
                val message = """
                    $description

                    ${article.link}""".trimIndent()
                share(getString(R.string.share_thread_subject), message, R.string.title_share)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
                    param(FirebaseAnalytics.Param.ITEM_ID, article.id.toString())
                    param(FirebaseAnalytics.Param.ITEM_NAME, if (objectName.isEmpty()) "$forumTitle | $threadSubject" else "$objectName | $forumTitle | $threadSubject")
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "Article")
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
        private const val KEY_ARTICLE = "ARTICLE"

        fun start(context: Context, threadId: Int, threadSubject: String?, forumId: Int, forumTitle: String?, objectId: Int, objectName: String?, objectType: ForumEntity.ForumType?, article: ArticleEntity?) {
            context.startActivity<ArticleActivity>(
                    KEY_THREAD_ID to threadId,
                    KEY_THREAD_SUBJECT to threadSubject,
                    KEY_FORUM_ID to forumId,
                    KEY_FORUM_TITLE to forumTitle,
                    KEY_OBJECT_ID to objectId,
                    KEY_OBJECT_NAME to objectName,
                    KEY_OBJECT_TYPE to objectType,
                    KEY_ARTICLE to article
            )
        }
    }
}