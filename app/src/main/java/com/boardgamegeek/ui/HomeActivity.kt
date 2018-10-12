package com.boardgamegeek.ui

import android.os.Bundle
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.util.PreferencesUtils
import org.jetbrains.anko.intentFor

class HomeActivity : TopLevelActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = if (Authenticator.isSignedIn(this)) {
            when {
                Authenticator.isOldAuth(this) -> {
                    Authenticator.signOut(this)
                    intentFor<HotnessActivity>()
                }
                PreferencesUtils.isCollectionSetToSync(this) -> intentFor<CollectionActivity>()
                PreferencesUtils.getSyncPlays(this) -> intentFor<PlaysSummaryActivity>()
                PreferencesUtils.getSyncBuddies(this) -> intentFor<BuddiesActivity>()
                else -> intentFor<HotnessActivity>()
            }
        } else {
            intentFor<HotnessActivity>()
        }

        startActivity(intent)
        finish()
    }
}