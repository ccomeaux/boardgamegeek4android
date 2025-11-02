package com.boardgamegeek.ui

import android.os.Bundle
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.*

class HomeActivity : TopLevelActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = preferences()
        val intent = if (Authenticator.isSignedIn(this)) {
            when {
                Authenticator.isOldAuth(this) -> {
                    Authenticator.signOut(this)
                    intentFor<HotnessActivity>()
                }
                prefs.isCollectionSetToSync() -> {
                    val startScreen = prefs[KEY_START_SCREEN, START_SCREEN_LEGACY]
                    if (startScreen == START_SCREEN_SHELVES)
                        intentFor<CollectionDetailsActivity>()
                    else
                        intentFor<CollectionActivity>()
                }
                prefs[PREFERENCES_KEY_SYNC_PLAYS, false] == true -> intentFor<PlaysSummaryActivity>()
                prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] == true -> intentFor<BuddiesActivity>()
                else -> intentFor<HotnessActivity>()
            }
        } else {
            intentFor<HotnessActivity>()
        }

        startActivity(intent)
        finish()
    }
}
