package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.PlayViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayActivity : SimpleSinglePaneActivity() {
    private var internalId = BggContract.INVALID_ID.toLong()
    private val viewModel by viewModels<PlayViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
                param(FirebaseAnalytics.Param.ITEM_ID, internalId.toString())
            }
        }

        if (internalId == BggContract.INVALID_ID.toLong()) finish()

        viewModel.setId(internalId)
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    override fun readIntent() {
        internalId = intent.getLongExtra(KEY_ID, BggContract.INVALID_ID.toLong())
    }

    override fun createPane() = PlayFragment()

    companion object {
        private const val KEY_ID = "ID"

        fun start(context: Context, internalId: Long) {
            context.startActivity(createIntent(context, internalId))
        }

        fun createIntent(context: Context, internalId: Long): Intent {
            return context.intentFor<PlayActivity>(KEY_ID to internalId)
        }
    }
}
