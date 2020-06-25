package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.Observer
import androidx.palette.graphics.Palette
import androidx.viewpager2.widget.ViewPager2
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.showQuickLogPlay
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.GamePagerAdapter
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment
import com.boardgamegeek.ui.dialog.GameUsersDialogFragment
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.util.ActivityUtils
import com.boardgamegeek.util.ShortcutUtils
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import kotlinx.android.synthetic.main.activity_hero_tab.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.snackbar
import timber.log.Timber

class GameActivity : HeroTabActivity(), CollectionStatusDialogFragment.Listener {
    private var gameId: Int = BggContract.INVALID_ID
    private var gameName: String = ""
    private var heroImageUrl = ""
    private var thumbnailUrl = ""
    private var isFavorite: Boolean = false
    private var isUserMenuEnabled = false
    private val prefs: SharedPreferences by lazy { this.preferences() }
    private val viewModel by viewModels<GameViewModel>()

    private val adapter: GamePagerAdapter by lazy {
        GamePagerAdapter(this, gameId, intent.getStringExtra(KEY_GAME_NAME))
    }

    override val optionsMenuId = R.menu.game

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        if (gameId == BggContract.INVALID_ID) {
            Timber.w("Received an invalid game ID.")
            finish()
        }

        initializeViewPager()

        changeName(intent.getStringExtra(KEY_GAME_NAME) ?: "")
        changeImage(intent.getStringExtra(KEY_HERO_IMAGE_URL), intent.getStringExtra(KEY_THUMBNAIL_URL))

        viewModel.setId(gameId)

        viewModel.game.observe(this, Observer {
            when {
                it == null -> return@Observer
                it.status == Status.ERROR -> toast(if (it.message.isBlank()) getString(R.string.empty_game) else it.message)
                it.data == null -> return@Observer
                else -> {
                    it.data.apply {
                        changeName(name)
                        changeImage(heroImageUrl, thumbnailUrl)

                        this@GameActivity.isFavorite = isFavorite
                        this@GameActivity.isUserMenuEnabled = maxUsers > 0
                        this@GameActivity.thumbnailUrl = thumbnailUrl
                    }
                }
            }
        })

        viewModel.updateLastViewed(System.currentTimeMillis())

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Gmae")
                param(FirebaseAnalytics.Param.ITEM_ID, gameId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, gameName)
            }
        }
    }

    override fun createAdapter(): GamePagerAdapter {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                adapter.currentPosition = position
            }
        })
        return adapter
    }

    override fun getPageTitle(position: Int) = adapter.getPageTitle(position)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.findItem(R.id.menu_log_play_quick)?.isVisible = prefs.showQuickLogPlay()
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_favorite)?.setTitle(if (isFavorite) R.string.menu_unfavorite else R.string.menu_favorite)
        menu.findItem(R.id.menu_users)?.isEnabled = isUserMenuEnabled
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val upIntent = when {
                    Authenticator.isSignedIn(this) -> intentFor<CollectionActivity>()
                    else -> intentFor<HotnessActivity>()
                }
                if (shouldUpRecreateTask()) {
                    TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities()
                } else {
                    NavUtils.navigateUpTo(this, upIntent)
                }
                return true
            }
            R.id.menu_share -> {
                ActivityUtils.shareGame(this, gameId, gameName, "Game")
                return true
            }
            R.id.menu_favorite -> {
                isFavorite = !isFavorite
                viewModel.updateFavorite(isFavorite)
                return true
            }
            R.id.menu_shortcut -> {
                ShortcutUtils.createGameShortcut(this, gameId, gameName, thumbnailUrl)
                return true
            }
            R.id.menu_log_play_quick -> {
                getCoordinatorLayout().snackbar(R.string.msg_logging_play)
                ActivityUtils.logQuickPlay(this, gameId, gameName)
                return true
            }
            R.id.menu_view_image -> {
                ImageActivity.start(this, heroImageUrl)
                return true
            }
            R.id.menu_users -> {
                GameUsersDialogFragment.launch(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun shouldUpRecreateTask(): Boolean {
        return intent.getBooleanExtra(KEY_FROM_SHORTCUT, false)
    }

    private fun changeName(gameName: String) {
        if (gameName != this.gameName) {
            this.gameName = gameName
            intent.putExtra(KEY_GAME_NAME, gameName)
            safelySetTitle(gameName)
        }
    }

    private fun changeImage(heroImageUrl: String, thumbnailUrl: String) {
        val url = if (heroImageUrl.isBlank()) thumbnailUrl else heroImageUrl
        if (this.heroImageUrl != url) {
            this.heroImageUrl = url
            loadToolbarImage(url)
        }
    }

    override fun onPaletteLoaded(palette: Palette?) {
        viewModel.updateGameColors(palette)
    }

    override fun onSelectStatuses(selectedStatuses: List<String>, wishlistPriority: Int) {
        viewModel.addCollectionItem(selectedStatuses, wishlistPriority)
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_THUMBNAIL_URL = "THUMBNAIL_URL"
        private const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        private const val KEY_FROM_SHORTCUT = "FROM_SHORTCUT"

        @JvmOverloads
        @JvmStatic
        fun start(context: Context, gameId: Int, gameName: String, thumbnailUrl: String = "", heroImageUrl: String = "") {
            val intent = createIntent(context, gameId, gameName, thumbnailUrl, heroImageUrl)
                    ?: return
            context.startActivity(intent)
        }

        @JvmOverloads
        @JvmStatic
        fun startUp(context: Context, gameId: Int, gameName: String, thumbnailUrl: String = "", heroImageUrl: String = "") {
            val intent = createIntent(context, gameId, gameName, thumbnailUrl, heroImageUrl)
                    ?: return
            context.startActivity(intent.clearTask().clearTop())
        }

        @JvmStatic
        fun createIntentAsShortcut(context: Context, gameId: Int, gameName: String, thumbnailUrl: String): Intent? {
            val intent = createIntent(context, gameId, gameName, thumbnailUrl) ?: return null
            intent.action = Intent.ACTION_VIEW
            return intent.putExtra(KEY_FROM_SHORTCUT, true).clearTop().newTask()
        }

        @JvmStatic
        fun createIntent(context: Context, gameId: Int, gameName: String, thumbnailUrl: String = "", heroImageUrl: String = ""): Intent? {
            if (gameId == BggContract.INVALID_ID) return null
            return context.intentFor<GameActivity>(
                    KEY_GAME_ID to gameId,
                    KEY_GAME_NAME to gameName,
                    KEY_THUMBNAIL_URL to thumbnailUrl,
                    KEY_HERO_IMAGE_URL to heroImageUrl
            )
        }
    }
}
