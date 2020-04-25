package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.boardgamegeek.R
import com.boardgamegeek.entities.GeekListCommentEntity
import com.boardgamegeek.entities.GeekListEntity
import com.boardgamegeek.entities.GeekListItemEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.GameActivity.Companion.start
import com.boardgamegeek.util.ActivityUtils
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import org.jetbrains.anko.startActivity
import java.util.*

class GeekListItemActivity : HeroTabActivity() {
    private var geekListId = 0
    private var geekListTitle = ""
    private var objectId = 0
    private var objectName = ""
    private var url: String? = null
    private var isBoardGame = false
    private var order = 0
    private var type = ""
    private var username = ""
    private var numberOfThumbs = 0
    private var postedDate: Long = 0
    private var editedDate: Long = 0
    private var body: String = ""
    private var comments: ArrayList<GeekListCommentEntity>? = null

    private val adapter: GeekListItemPagerAdapter by lazy {
        GeekListItemPagerAdapter(supportFragmentManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geekListTitle = intent.getStringExtra(KEY_TITLE).orEmpty()
        geekListId = intent.getIntExtra(KEY_ID, BggContract.INVALID_ID)
        objectId = intent.getIntExtra(KEY_OBJECT_ID, BggContract.INVALID_ID)
        objectName = intent.getStringExtra(KEY_NAME).orEmpty()
        url = intent.getStringExtra(KEY_OBJECT_URL)
        isBoardGame = intent.getBooleanExtra(KEY_IS_BOARD_GAME, false)
        order = intent.getIntExtra(KEY_ORDER, 0)
        type = intent.getStringExtra(KEY_TYPE).orEmpty()
        username = intent.getStringExtra(KEY_USERNAME).orEmpty()
        numberOfThumbs = intent.getIntExtra(KEY_THUMBS, 0)
        postedDate = intent.getLongExtra(KEY_POSTED_DATE, 0)
        editedDate = intent.getLongExtra(KEY_EDITED_DATE, 0)
        body = intent.getStringExtra(KEY_BODY).orEmpty()
        comments = intent.getParcelableArrayListExtra(KEY_COMMENTS)
        val imageId = intent.getIntExtra(KEY_IMAGE_ID, BggContract.INVALID_ID)
        initializeViewPager()
        safelySetTitle(objectName)
        if (savedInstanceState == null) {
            Answers.getInstance().logContentView(ContentViewEvent()
                    .putContentType("GeekListItem")
                    .putContentId(objectId.toString())
                    .putContentName(objectName))
        }
        loadToolbarImage(imageId)
    }

    override val optionsMenuId = R.menu.view

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                when {
                    geekListId != BggContract.INVALID_ID -> {
                        GeekListActivity.startUp(this, geekListId, geekListTitle)
                        finish()
                    }
                    else -> onBackPressed()
                }
                true
            }
            R.id.menu_view -> {
                when {
                    isBoardGame -> start(this, objectId, objectName)
                    else -> ActivityUtils.link(this, url)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun createAdapter() = adapter

    inner class GeekListItemPagerAdapter(fragmentManager: FragmentManager) :
            FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> getString(R.string.title_description)
                1 -> getString(R.string.title_comments)
                else -> ""
            }
        }

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> GeekListItemFragment.newInstance(order, geekListTitle, type, username, numberOfThumbs, postedDate, editedDate, body)
                1 -> GeekListCommentsFragment.newInstance(comments)
                else -> ErrorFragment.newInstance()
            }
        }

        override fun getCount() = 2
    }

    companion object {
        private const val KEY_ID = "GEEK_LIST_ID"
        private const val KEY_ORDER = "GEEK_LIST_ORDER"
        private const val KEY_TITLE = "GEEK_LIST_TITLE"
        private const val KEY_TYPE = "GEEK_LIST_TYPE"
        private const val KEY_USERNAME = "GEEK_LIST_USERNAME"
        private const val KEY_NAME = "GEEK_LIST_NAME"
        private const val KEY_THUMBS = "GEEK_LIST_THUMBS"
        private const val KEY_IMAGE_ID = "GEEK_LIST_IMAGE_ID"
        private const val KEY_POSTED_DATE = "GEEK_LIST_POSTED_DATE"
        private const val KEY_EDITED_DATE = "GEEK_LIST_EDITED_DATE"
        private const val KEY_BODY = "GEEK_LIST_BODY"
        private const val KEY_OBJECT_ID = "GEEK_LIST_OBJECT_ID"
        private const val KEY_OBJECT_URL = "GEEK_LIST_OBJECT_URL"
        private const val KEY_IS_BOARD_GAME = "GEEK_LIST_IS_BOARD_GAME"
        private const val KEY_COMMENTS = "GEEK_LIST_COMMENTS"

        @JvmStatic
        fun start(context: Context, geekList: GeekListEntity, item: GeekListItemEntity, order: Int) {
            context.startActivity<GeekListItemActivity>(
                    KEY_ID to geekList.id,
                    KEY_TITLE to geekList.title,
                    KEY_ORDER to order,
                    KEY_NAME to item.objectName,
                    KEY_TYPE to item.objectTypeDescription(context),
                    KEY_IMAGE_ID to item.imageId,
                    KEY_USERNAME to item.username,
                    KEY_THUMBS to item.numberOfThumbs,
                    KEY_POSTED_DATE to item.postDateTime,
                    KEY_EDITED_DATE to item.editDateTime,
                    KEY_BODY to item.body,
                    KEY_OBJECT_URL to item.objectUrl,
                    KEY_OBJECT_ID to item.objectId,
                    KEY_IS_BOARD_GAME to item.isBoardGame,
                    KEY_COMMENTS to item.comments
            )
        }
    }
}
