package com.boardgamegeek.ui

import android.content.ContentResolver
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.cancelSync
import com.boardgamegeek.provider.BggContract
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

/**
 * Registers/unregisters a sticky event bus
 * Shows a toast when user profile is updated
 * Subtitle setter
 * Provides common menu functions:
 * 1. Cancel sync
 * 2. Toggling navigation drawer
 * 3. Inflation helper.
 */
abstract class BaseActivity : AppCompatActivity() {
    protected lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = Firebase.analytics
    }

    protected open val optionsMenuId: Int
        @MenuRes
        get() = INVALID_MENU_ID

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.base, menu)
        if (optionsMenuId != INVALID_MENU_ID) menuInflater.inflate(optionsMenuId, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_cancel_sync)?.isVisible = isSyncActiveOrPending()
        return true
    }

    private fun isSyncActiveOrPending(): Boolean {
        val account = Authenticator.getAccount(this) ?: return false
        val syncActive = ContentResolver.isSyncActive(account, BggContract.CONTENT_AUTHORITY)
        val syncPending = ContentResolver.isSyncPending(account, BggContract.CONTENT_AUTHORITY)
        return syncActive || syncPending
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (this is TopLevelActivity) {
                    // bug in ActionBarDrawerToggle
                    return false
                }
                NavUtils.navigateUpFromSameTask(this)
                true
            }
            R.id.menu_cancel_sync -> {
                cancelSync()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val INVALID_MENU_ID = 0
    }
}
