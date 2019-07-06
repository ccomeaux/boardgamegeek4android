package com.boardgamegeek.ui

import android.os.Bundle
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.getSyncBuddies
import com.boardgamegeek.extensions.isCollectionSetToSync
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
                isCollectionSetToSync() -> intentFor<CollectionActivity>()
                PreferencesUtils.getSyncPlays(this) -> intentFor<PlaysSummaryActivity>()
                getSyncBuddies() -> intentFor<BuddiesActivity>()
                else -> intentFor<HotnessActivity>()
            }
        } else {
            intentFor<HotnessActivity>()
        }

        startActivity(intent)
        finish()
    }
}