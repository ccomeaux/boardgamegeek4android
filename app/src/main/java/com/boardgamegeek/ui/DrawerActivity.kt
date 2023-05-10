package com.boardgamegeek.ui

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.databinding.ActivityDrawerBaseBinding
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.pref.SettingsActivity
import com.boardgamegeek.ui.viewmodel.SelfUserViewModel
import com.boardgamegeek.ui.viewmodel.SyncViewModel
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity that displays the navigation drawer and allows for content in the root_container FrameLayout.
 */
@AndroidEntryPoint
abstract class DrawerActivity : BaseActivity() {
    private lateinit var binding: ActivityDrawerBaseBinding
    lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    var rootContainer: ViewGroup? = null

    private val viewModel by viewModels<SelfUserViewModel>()
    private val syncViewModel by viewModels<SyncViewModel>()

    protected open val navigationItemId: Int
        get() = 0

    protected open fun bindLayout() {
        binding = ActivityDrawerBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindLayout()

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation)
        toolbar = findViewById(R.id.toolbar)
        rootContainer = findViewById(R.id.root_container)

        setSupportActionBar(toolbar)

        onBackPressedDispatcher.addCallback(this) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                finish() // TODO - register and deregister?
            }
        }

        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
        drawerLayout.setStatusBarBackgroundColor(ContextCompat.getColor(this, R.color.primary_dark))

        navigationView.setNavigationItemSelectedListener { menuItem ->
            selectItem(menuItem.itemId)
            true
        }

        navigationView.getHeaderView(0).findViewById<Button>(R.id.signInButton)?.let {
            it.setOnClickListener { startActivity<LoginActivity>() }
        }

        viewModel.user.observe(this) {
            navigationView.menu.setGroupVisible(R.id.personal, Authenticator.isSignedIn(this))
            refreshHeader(it?.data)
        }

        syncViewModel.currentSyncTimestamp.observe(this) {
            invalidateOptionsMenu()
        }
        syncViewModel.username.observe(this) {
            viewModel.setUsername(it)
        }
    }

    override fun onStart() {
        super.onStart()
        if (preferences()[KEY_HAS_SEEN_NAV_DRAWER, false] != true) {
            drawerLayout.openDrawer(GravityCompat.START)
            preferences()[KEY_HAS_SEEN_NAV_DRAWER] = true
        }
    }

    override fun onResume() {
        super.onResume()
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

    private fun refreshHeader(user: UserEntity?) {
        val view = navigationView.getHeaderView(0)
        val primaryView = view.findViewById<TextView>(R.id.accountInfoPrimaryView)
        val secondaryView = view.findViewById<TextView>(R.id.accountInfoSecondaryView)
        val imageView = view.findViewById<ImageView>(R.id.accountImageView)
        val signedInGroup = view.findViewById<Group>(R.id.signedInGroup)
        val signInButton = view.findViewById<Button>(R.id.signInButton)

        if (Authenticator.isSignedIn(this) && user != null) {
            if (user.fullName.isNotBlank() && user.userName.isNotBlank()) {
                primaryView.text = user.fullName
                secondaryView.text = user.userName
                primaryView.setOnClickListener { }
                secondaryView.setOnClickListener { linkToBgg("user/${user.userName}") }
            } else if (user.userName.isNotBlank()) {
                primaryView.text = user.userName
                secondaryView.text = ""
                primaryView.setOnClickListener { linkToBgg("user/${user.userName}") }
                secondaryView.setOnClickListener { }
            } else {
                Authenticator.getAccount(this)?.let { viewModel.setUsername(it.name) }
            }
            if (user.avatarUrl.isBlank()) {
                imageView.isVisible = false
            } else {
                imageView.isVisible = true
                imageView.loadImage(user.avatarUrl, R.drawable.person_image_empty)
            }
            signedInGroup.isVisible = true
            signInButton.isVisible = false
        } else {
            signedInGroup.isVisible = false
            signInButton.isVisible = true
        }
    }
}
