package com.boardgamegeek.ui

import android.os.Bundle
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_BUDDIES
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.isCollectionSetToSync
import org.jetbrains.anko.defaultSharedPreferences
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
                defaultSharedPreferences.isCollectionSetToSync() -> intentFor<CollectionActivity>()
                defaultSharedPreferences[PREFERENCES_KEY_SYNC_PLAYS, false] == true -> intentFor<PlaysSummaryActivity>()
                defaultSharedPreferences[PREFERENCES_KEY_SYNC_BUDDIES, false] == true -> intentFor<BuddiesActivity>()
                else -> intentFor<HotnessActivity>()
            }
        } else {
            intentFor<HotnessActivity>()
        }

        startActivity(intent)
        finish()
    }
}