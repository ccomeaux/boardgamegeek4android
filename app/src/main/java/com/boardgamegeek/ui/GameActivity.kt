package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.palette.graphics.Palette
import androidx.viewpager2.widget.ViewPager2
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.databinding.ActivityHeroTabBinding
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.GamePagerAdapter
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment
import com.boardgamegeek.ui.dialog.GameUsersDialogFragment
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.util.ShortcutUtils
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import timber.log.Timber

class GameActivity : HeroTabActivity(), CollectionStatusDialogFragment.Listener {
    private var gameId: Int = BggContract.INVALID_ID
    private var gameName: String = ""
    private var heroImageUrl = ""
    private var thumbnailUrl = ""
    private var imageUrl = ""
    private var arePlayersCustomSorted = false
    private var isFavorite: Boolean = false
    private var isUserMenuEnabled = false
    private lateinit var binding: ActivityHeroTabBinding
    private val viewModel by viewModels<GameViewModel>()

    private val adapter: GamePagerAdapter by lazy {
        GamePagerAdapter(this, gameId, intent.getStringExtra(KEY_GAME_NAME).orEmpty())
    }

    override val optionsMenuId = R.menu.game

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHeroTabBinding.bind(findViewById(R.id.drawer_layout))

        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        if (gameId == BggContract.INVALID_ID) {
            Timber.w("Received an invalid game ID.")
            finish()
        }

        initializeViewPager()

        changeName(intent.getStringExtra(KEY_GAME_NAME).orEmpty())
        changeImage(intent.getStringExtra(KEY_HERO_IMAGE_URL).orEmpty(), intent.getStringExtra(KEY_THUMBNAIL_URL).orEmpty())

        viewModel.setId(gameId)

        viewModel.game.observe(this) {
            it?.let { (status, data, message) ->
                if (status == Status.ERROR) toast(if (message.isBlank()) getString(R.string.empty_game) else message)
                data?.let { game ->
                    changeName(game.name)
                    changeImage(game.heroImageUrl, game.thumbnailUrl)
                    isFavorite = game.isFavorite
                    isUserMenuEnabled = game.maxUsers > 0
                    thumbnailUrl = game.thumbnailUrl
                    imageUrl = game.imageUrl
                    arePlayersCustomSorted = game.customPlayerSort
                }
            }
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Game")
                param(FirebaseAnalytics.Param.ITEM_ID, gameId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, gameName)
            }
        }
    }

    override fun createAdapter(): GamePagerAdapter {
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                adapter.currentPosition = position
            }
        })
        return adapter
    }

    override fun getPageTitle(position: Int) = adapter.getPageTitle(position)

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
            }
            R.id.menu_share -> shareGame(gameId, gameName, "Game", firebaseAnalytics)
            R.id.menu_favorite -> {
                isFavorite = !isFavorite
                viewModel.updateFavorite(isFavorite)
            }
            R.id.menu_shortcut -> ShortcutUtils.createGameShortcut(this, gameId, gameName, thumbnailUrl)
            R.id.menu_log_play_quick -> {
                getCoordinatorLayout().snackbar(R.string.msg_logging_play)
                viewModel.logQuickPlay(gameId, gameName)
            }
            R.id.menu_log_play -> LogPlayActivity.logPlay(this, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, arePlayersCustomSorted)
            R.id.menu_log_play_wizard -> NewPlayActivity.start(this, gameId, gameName)
            R.id.menu_view_image -> ImageActivity.start(this, heroImageUrl)
            R.id.menu_users -> GameUsersDialogFragment.launch(this)
            R.id.menu_view -> linkToBgg("boardgame", gameId)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
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

        fun start(context: Context, gameId: Int, gameName: String, thumbnailUrl: String = "", heroImageUrl: String = "") {
            val intent = createIntent(context, gameId, gameName, thumbnailUrl, heroImageUrl) ?: return
            context.startActivity(intent)
        }

        fun startUp(context: Context, gameId: Int, gameName: String, thumbnailUrl: String = "", heroImageUrl: String = "") {
            val intent = createIntent(context, gameId, gameName, thumbnailUrl, heroImageUrl) ?: return
            context.startActivity(intent.clearTask().clearTop())
        }

        fun createIntentAsShortcut(context: Context, gameId: Int, gameName: String, thumbnailUrl: String): Intent? {
            val intent = createIntent(context, gameId, gameName, thumbnailUrl) ?: return null
            intent.action = Intent.ACTION_VIEW
            return intent.putExtra(KEY_FROM_SHORTCUT, true).clearTop().newTask()
        }

        fun createIntent(context: Context, gameId: Int, gameName: String, thumbnailUrl: String = "", heroImageUrl: String = ""): Intent? {
            if (gameId == BggContract.INVALID_ID) return null
            return context.intentFor<GameActivity>(
                KEY_GAME_ID to gameId,
                KEY_GAME_NAME to gameName,
                KEY_THUMBNAIL_URL to thumbnailUrl,
                KEY_HERO_IMAGE_URL to heroImageUrl,
            )
        }
    }
}
