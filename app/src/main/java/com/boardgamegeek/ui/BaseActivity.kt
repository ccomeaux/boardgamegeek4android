package com.boardgamegeek.ui

import android.support.annotation.MenuRes
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.tasks.sync.SyncUserTask
import hugo.weaving.DebugLog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.act
import org.jetbrains.anko.ctx
import org.jetbrains.anko.toast

const val INVALID_MENU_ID = 0

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
    protected open val optionsMenuId: Int
        @MenuRes
        get() = INVALID_MENU_ID

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @DebugLog
    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onEvent(event: SyncUserTask.CompletedEvent) {
        if (event.username != null && event.username == AccountUtils.getUsername(ctx)) {
            toast(R.string.profile_updated)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.base, menu)
        if (optionsMenuId != INVALID_MENU_ID) menuInflater.inflate(optionsMenuId, menu)
        return true
    }

    @DebugLog
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_cancel_sync)?.isVisible = SyncService.isActiveOrPending(ctx)
        return super.onPrepareOptionsMenu(menu)
    }

    protected fun setSubtitle(text: String?) {
        supportActionBar?.subtitle = text ?: ""
    }

    @DebugLog
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (this is TopLevelActivity) {
                    // bug in ActionBarDrawerToggle
                    return false
                }
                NavUtils.navigateUpFromSameTask(act)
                true
            }
            R.id.menu_cancel_sync -> {
                SyncService.cancelSync(ctx)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}