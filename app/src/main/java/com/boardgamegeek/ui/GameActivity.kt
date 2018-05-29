package com.boardgamegeek.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.NavUtils
import android.support.v4.app.TaskStackBuilder
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.support.v7.graphics.Palette
import android.view.Menu
import android.view.MenuItem
import com.boardgamegeek.R
import com.boardgamegeek.applyDarkScrim
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.colorize
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.GamePagerAdapter
import com.boardgamegeek.ui.model.Status
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.util.ActivityUtils
import com.boardgamegeek.util.ImageUtils.Callback
import com.boardgamegeek.util.ImageUtils.safelyLoadImage
import com.boardgamegeek.util.PreferencesUtils
import com.boardgamegeek.util.ShortcutUtils
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import org.jetbrains.anko.*
import org.jetbrains.anko.design.snackbar
import timber.log.Timber

class GameActivity : HeroTabActivity() {
    private var gameId: Int = BggContract.INVALID_ID
    private var gameName: String = ""
    private var imageUrl = ""
    private var thumbnailUrl = ""
    private var heroImageUrl = ""
    private var isFavorite: Boolean = false

    private val viewModel: GameViewModel by lazy {
        ViewModelProviders.of(this).get(GameViewModel::class.java)
    }

    private val adapter: GamePagerAdapter by lazy {
        GamePagerAdapter(supportFragmentManager, this, gameId, intent.getStringExtra(KEY_GAME_NAME))
    }

    override val optionsMenuId: Int
        get() = R.menu.game

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        if (gameId == BggContract.INVALID_ID) {
            Timber.w("Received an invalid game ID.")
            finish()
        }

        initializeViewPager()

        changeName(intent.getStringExtra(KEY_GAME_NAME) ?: "")
        changeImage(intent.getStringExtra(KEY_IMAGE_URL),
                intent.getStringExtra(KEY_THUMBNAIL_URL),
                intent.getStringExtra(KEY_HERO_IMAGE_URL))

        viewModel.setId(gameId)

        viewModel.game.observe(this, Observer {
            when {
                it == null -> return@Observer
                it.status == Status.ERROR -> toast(if (it.message.isBlank()) getString(R.string.empty_game) else it.message)
                it.data == null -> return@Observer
                else -> {
                    it.data.apply {
                        changeName(name)
                        changeImage(imageUrl, thumbnailUrl, heroImageUrl)
                        this@GameActivity.isFavorite = isFavorite

                        adapter.gameName = name
                        adapter.imageUrl = imageUrl
                        adapter.thumbnailUrl = thumbnailUrl
                        adapter.heroImageUrl = heroImageUrl
                        adapter.arePlayersCustomSorted = customPlayerSort
                        adapter.iconColor = iconColor

                        if (fab.colorize(iconColor)) adapter.displayFab()
                    }
                }
            }
        })

        viewModel.updateLastViewed(System.currentTimeMillis())

        if (savedInstanceState == null) {
            Answers.getInstance().logContentView(ContentViewEvent()
                    .putContentType("Game")
                    .putContentId(gameId.toString())
                    .putContentName(gameName))
        }
    }

    override fun setUpViewPager() {
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                adapter.currentPosition = position
                adapter.displayFab()
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        findViewById<FloatingActionButton>(R.id.fab)?.setOnClickListener { adapter.onFabClicked() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.findItem(R.id.menu_log_play_quick)?.isVisible = PreferencesUtils.showQuickLogPlay(ctx)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_favorite)?.setTitle(if (isFavorite) R.string.menu_unfavorite else R.string.menu_favorite)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val upIntent = when {
                    Authenticator.isSignedIn(ctx) -> intentFor<CollectionActivity>()
                    else -> intentFor<HotnessActivity>()
                }
                if (shouldUpRecreateTask()) {
                    TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities()
                } else {
                    NavUtils.navigateUpTo(act, upIntent)
                }
                return true
            }
            R.id.menu_share -> {
                ActivityUtils.shareGame(act, gameId, gameName, "Game")
                return true
            }
            R.id.menu_favorite -> {
                isFavorite = !isFavorite
                viewModel.updateFavorite(isFavorite)
                return true
            }
            R.id.menu_shortcut -> {
                ShortcutUtils.createGameShortcut(ctx, gameId, gameName, thumbnailUrl)
                return true
            }
            R.id.menu_log_play_quick -> {
                snackbar(coordinator, R.string.msg_logging_play)
                ActivityUtils.logQuickPlay(ctx, gameId, gameName)
                return true
            }
            R.id.menu_view_image -> {
                ImageActivity.start(ctx, imageUrl)
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

    private fun changeImage(imageUrl: String?, thumbnailUrl: String?, heroImageUrl: String?) {
        if (this.imageUrl != imageUrl ||
                this.thumbnailUrl != thumbnailUrl ||
                this.heroImageUrl != heroImageUrl) {
            this.imageUrl = imageUrl ?: ""
            this.thumbnailUrl = thumbnailUrl ?: ""
            this.heroImageUrl = heroImageUrl ?: ""

            toolbarImage?.safelyLoadImage(this.imageUrl, this.thumbnailUrl, this.heroImageUrl, imageLoadCallback)
        } else {
            this.imageUrl = imageUrl
            this.thumbnailUrl = thumbnailUrl
            this.heroImageUrl = heroImageUrl
        }
    }

    private val imageLoadCallback = object : Callback {
        override fun onSuccessfulImageLoad(palette: Palette?) {
            viewModel.updateColors(palette)
            viewModel.updateHeroImageUrl(toolbarImage.getTag(R.id.url) as? String? ?: "")
            scrimView?.applyDarkScrim()
            adapter.displayFab()
        }

        override fun onFailedImageLoad() {
            adapter.displayFab()
        }
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_IMAGE_URL = "IMAGE_URL"
        private const val KEY_THUMBNAIL_URL = "THUMBNAIL_URL"
        private const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        private const val KEY_FROM_SHORTCUT = "FROM_SHORTCUT"

        @JvmOverloads
        @JvmStatic
        fun start(context: Context, gameId: Int, gameName: String, imageUrl: String = "", thumbnailUrl: String = "", heroImageUrl: String = "") {
            val intent = createIntent(context, gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl) ?: return
            context.startActivity(intent)
        }

        @JvmOverloads
        @JvmStatic
        fun startUp(context: Context, gameId: Int, gameName: String, imageUrl: String = "", thumbnailUrl: String = "", heroImageUrl: String = "") {
            val intent = createIntent(context, gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl) ?: return
            context.startActivity(intent.clearTask().clearTop())
        }

        @JvmStatic
        fun createIntentAsShortcut(context: Context, gameId: Int, gameName: String, thumbnailUrl: String): Intent? {
            val intent = createIntent(context, gameId, gameName, thumbnailUrl = thumbnailUrl) ?: return null
            return intent.putExtra(KEY_FROM_SHORTCUT, true).clearTop().newTask()
        }

        @JvmStatic
        fun createIntent(context: Context, gameId: Int, gameName: String, imageUrl: String = "", thumbnailUrl: String = "", heroImageUrl: String = ""): Intent? {
            if (gameId == BggContract.INVALID_ID) return null
            return context.intentFor<GameActivity>(
                    KEY_GAME_ID to gameId,
                    KEY_GAME_NAME to gameName,
                    KEY_IMAGE_URL to imageUrl,
                    KEY_THUMBNAIL_URL to thumbnailUrl,
                    KEY_HERO_IMAGE_URL to heroImageUrl
            )
        }
    }
}
