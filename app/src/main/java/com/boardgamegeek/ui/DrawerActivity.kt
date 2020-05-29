package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.events.SignInEvent
import com.boardgamegeek.events.SignOutEvent
import com.boardgamegeek.events.SyncCompleteEvent
import com.boardgamegeek.events.SyncEvent
import com.boardgamegeek.extensions.*
import com.boardgamegeek.pref.SettingsActivity
import com.boardgamegeek.ui.viewmodel.SelfUserViewModel
import com.google.android.material.navigation.NavigationView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.startActivity

/**
 * Activity that displays the navigation drawer and allows for content in the root_container FrameLayout.
 */
abstract class DrawerActivity : BaseActivity() {
    lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    var rootContainer: ViewGroup? = null

    private val viewModel by viewModels<SelfUserViewModel>()

    protected open val navigationItemId: Int
        get() = 0

    protected open val layoutResId: Int
        @LayoutRes
        get() = R.layout.activity_drawer_base

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutResId)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation)
        toolbar = findViewById(R.id.toolbar)
        rootContainer = findViewById(R.id.root_container)

        setSupportActionBar(toolbar)

        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
        drawerLayout.setStatusBarBackgroundColor(ContextCompat.getColor(this, R.color.primary_dark))

        navigationView.setNavigationItemSelectedListener { menuItem ->
            selectItem(menuItem.itemId)
            true
        }

        viewModel.user.observe(this, Observer {
            refreshDrawer()
        })
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        if (defaultSharedPreferences[KEY_HAS_SEEN_NAV_DRAWER, false] != true) {
            drawerLayout.openDrawer(GravityCompat.START)
            defaultSharedPreferences[KEY_HAS_SEEN_NAV_DRAWER] = true
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDrawer()
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: SignInEvent) {
        viewModel.setUsername(event.username)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: SignOutEvent) {
        viewModel.setUsername("")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: SyncEvent) {
        invalidateOptionsMenu()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: SyncCompleteEvent) {
        invalidateOptionsMenu()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun refreshDrawer() {
        navigationView.menu.setGroupVisible(R.id.personal, Authenticator.isSignedIn(this))
        refreshHeader()
        navigationView.setCheckedItem(navigationItemId)
    }

    private fun selectItem(menuItemId: Int) {
        if (menuItemId != navigationItemId) {
            when (menuItemId) {
                R.id.collection -> startActivity<CollectionActivity>()
                R.id.collection_details -> startActivity<CollectionDetailsActivity>()
                R.id.search -> startActivity<SearchResultsActivity>()
                R.id.hotness -> startActivity<HotnessActivity>()
                R.id.top_games -> startActivity<TopGamesActivity>()
                R.id.geeklists -> startActivity<GeekListsActivity>()
                R.id.plays -> startActivity<PlaysSummaryActivity>()
                R.id.geek_buddies -> startActivity<BuddiesActivity>()
                R.id.forums -> startActivity<ForumsActivity>()
                R.id.data -> startActivity<DataActivity>()
                R.id.settings -> startActivity<SettingsActivity>()
            }
        }
        drawerLayout.closeDrawer(navigationView)
    }

    private fun refreshHeader() {
        val view = navigationView.getHeaderView(0)
        val primaryView = view.findViewById<TextView>(R.id.account_info_primary)
        val secondaryView = view.findViewById<TextView>(R.id.account_info_secondary)
        val imageView = view.findViewById<ImageView>(R.id.account_image)
        val signedInGroup = view.findViewById<Group>(R.id.signed_in)
        val signInButton = view.findViewById<Button>(R.id.singInButton)

        if (Authenticator.isSignedIn(this)) {
            val fullName = AccountUtils.getFullName(this)
            val username = AccountUtils.getUsername(this)
            if (fullName.isNullOrBlank()) {
                if (username.isNullOrBlank()) {
                    val account = Authenticator.getAccount(this)
                    if (account != null) viewModel.setUsername(account.name)
                    return
                } else {
                    primaryView.text = username
                    secondaryView.text = ""
                    viewModel.setUsername(username)
                }
            } else {
                primaryView.text = fullName
                secondaryView.text = username
                viewModel.setUsername(username)
            }
            secondaryView.setOnClickListener {
                linkToBgg("user/$username")
            }
            val avatarUrl = AccountUtils.getAvatarUrl(this)
            if (avatarUrl.isNullOrBlank()) {
                imageView.visibility = View.GONE
            } else {
                imageView.visibility = View.VISIBLE
                imageView.loadThumbnail(avatarUrl, R.drawable.person_image_empty)
            }
            signedInGroup.isVisible = true
            signInButton.isVisible = false
        } else {
            signedInGroup.isVisible = false
            signInButton.isVisible = true
            signInButton.setOnClickListener { startActivity<LoginActivity>() }
        }
    }
}
