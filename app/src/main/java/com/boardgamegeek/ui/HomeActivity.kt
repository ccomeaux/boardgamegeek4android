package com.boardgamegeek.ui

import android.os.Bundle
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.util.PreferencesUtils
import org.jetbrains.anko.ctx
import org.jetbrains.anko.intentFor

class HomeActivity : TopLevelActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = if (Authenticator.isSignedIn(ctx)) {
            when {
                Authenticator.isOldAuth(ctx) -> {
                    Authenticator.signOut(ctx)
                    intentFor<HotnessActivity>()
                }
                PreferencesUtils.isCollectionSetToSync(ctx) -> intentFor<CollectionActivity>()
                PreferencesUtils.getSyncPlays(ctx) -> intentFor<PlaysSummaryActivity>()
                PreferencesUtils.getSyncBuddies(ctx) -> intentFor<BuddiesActivity>()
                else -> intentFor<HotnessActivity>()
            }
        } else {
            intentFor<HotnessActivity>()
        }

        startActivity(intent)
        finish()
    }
}