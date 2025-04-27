package com.boardgamegeek.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    protected open val optionsMenuId: Int
        @MenuRes
        get() = INVALID_MENU_ID

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        if (optionsMenuId != INVALID_MENU_ID) menuInflater.inflate(optionsMenuId, menu)
        return true
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    companion object {
        const val INVALID_MENU_ID = 0
    }
}
