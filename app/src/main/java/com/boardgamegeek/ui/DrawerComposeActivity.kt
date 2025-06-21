package com.boardgamegeek.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.databinding.ActivityDrawerComposeBinding
import com.boardgamegeek.model.User
import com.boardgamegeek.extensions.*
import com.boardgamegeek.pref.SettingsActivity
import com.boardgamegeek.ui.viewmodel.SelfUserViewModel
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity that displays the navigation drawer and allows for content in the root_container FrameLayout.
 */
@AndroidEntryPoint
abstract class DrawerComposeActivity : BaseActivity() {
    private lateinit var binding: ActivityDrawerComposeBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    protected var composeView: ComposeView? = null

    private val selfUserViewModel by viewModels<SelfUserViewModel>()

    @Suppress("SameReturnValue")
    protected open val navigationItemId: Int
        get() = ResourcesCompat.ID_NULL

    protected open fun bindLayout() {
        binding = ActivityDrawerComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindLayout()

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation)
        composeView = findViewById(R.id.composeView)

        onBackPressedDispatcher.addCallback(this) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                finish()
            }
        }

        drawerLayout.setStatusBarBackgroundColor(ContextCompat.getColor(this, R.color.primary_dark))

        navigationView.setNavigationItemSelectedListener { menuItem ->
            selectItem(menuItem.itemId)
            true
        }

        navigationView.getHeaderView(0).findViewById<Button>(R.id.signInButton)?.let {
            it.setOnClickListener { startActivity<LoginActivity>() }
        }

        selfUserViewModel.user.observe(this) {
            navigationView.menu.setGroupVisible(R.id.personal, Authenticator.isSignedIn(this))
            refreshHeader(it)
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
                R.id.sync -> startActivity<SyncActivity>()
                R.id.data -> startActivity<DataActivity>()
                R.id.settings -> startActivity<SettingsActivity>()
            }
        }
        drawerLayout.closeDrawer(navigationView)
    }

    private fun refreshHeader(user: User?) {
        val view = navigationView.getHeaderView(0)
        val primaryView = view.findViewById<TextView>(R.id.accountInfoPrimaryView)
        val secondaryView = view.findViewById<TextView>(R.id.accountInfoSecondaryView)
        val imageView = view.findViewById<ImageView>(R.id.accountImageView)
        val signedInGroup = view.findViewById<Group>(R.id.signedInGroup)
        val signInButton = view.findViewById<Button>(R.id.signInButton)

        if (Authenticator.isSignedIn(this) && user != null) {
            if (user.fullName.isNotBlank() && user.username.isNotBlank()) {
                primaryView.text = user.fullName
                secondaryView.text = user.username
                primaryView.setOnClickListener { }
                secondaryView.setOnClickListener { linkToBgg("user/${user.username}") }
            } else if (user.username.isNotBlank()) {
                primaryView.text = user.username
                secondaryView.clearText()
                primaryView.setOnClickListener { linkToBgg("user/${user.username}") }
                secondaryView.setOnClickListener { }
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
