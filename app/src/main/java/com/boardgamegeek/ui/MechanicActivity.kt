package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.MechanicViewModel
import org.jetbrains.anko.startActivity

class MechanicActivity : SimpleSinglePaneActivity() {
    private val viewModel: MechanicViewModel by lazy {
        ViewModelProviders.of(this).get(MechanicViewModel::class.java)
    }

    override fun onCreatePane(intent: Intent) = MechanicCollectionFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(KEY_MECHANIC_ID, BggContract.INVALID_ID)
        val name = intent.getStringExtra(KEY_MECHANIC_NAME)
        title = name
        setSubtitle(getString(R.string.title_mechanic))
        viewModel.setId(id)
    }

    companion object {
        private const val KEY_MECHANIC_ID = "MECHANIC_ID"
        private const val KEY_MECHANIC_NAME = "MECHANIC_NAME"

        fun start(context: Context, mechanicId: Int, mechanicName: String) {
            context.startActivity<MechanicActivity>(
                    KEY_MECHANIC_ID to mechanicId,
                    KEY_MECHANIC_NAME to mechanicName
            )
        }
    }
}