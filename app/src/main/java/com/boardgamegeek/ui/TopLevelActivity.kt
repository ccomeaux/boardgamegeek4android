package com.boardgamegeek.ui

import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.view.View
import com.boardgamegeek.R
import org.jetbrains.anko.act

abstract class TopLevelActivity : DrawerActivity() {
    private lateinit var activityTitle: CharSequence
    private lateinit var drawerTitle: CharSequence
    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        drawerTitle = getString(R.string.app_name)
        activityTitle = title

        drawerToggle = object : ActionBarDrawerToggle(act, drawerLayout, R.string.drawer_open, R.string.drawer_close) {
            // TODO: finish and start CAB with the drawer open/close
            override fun onDrawerClosed(drawerView: View) {
                supportActionBar?.title = activityTitle
            }

            override fun onDrawerOpened(drawerView: View) {
                supportActionBar?.title = drawerTitle
            }
        }
        drawerLayout.addDrawerListener(drawerToggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun setTitle(title: CharSequence) {
        activityTitle = title
        supportActionBar?.title = activityTitle
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
    }
}
