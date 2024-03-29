package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import com.boardgamegeek.R
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.MechanicViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MechanicActivity : SimpleSinglePaneActivity() {
    private var id = BggContract.INVALID_ID

    private val viewModel by viewModels<MechanicViewModel>()

    override fun onCreatePane(intent: Intent) = MechanicCollectionFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = intent.getIntExtra(KEY_MECHANIC_ID, BggContract.INVALID_ID)
        title = intent.getStringExtra(KEY_MECHANIC_NAME)
        supportActionBar?.subtitle = getString(R.string.title_mechanic)
        viewModel.setId(id)
    }

    override val optionsMenuId = R.menu.mechanic

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_view -> linkToBgg("boardgamemechanic", id)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    companion object {
        private const val KEY_MECHANIC_ID = "MECHANIC_ID"
        private const val KEY_MECHANIC_NAME = "MECHANIC_NAME"

        fun start(context: Context, mechanicId: Int, mechanicName: String) {
            context.startActivity<MechanicActivity>(
                KEY_MECHANIC_ID to mechanicId,
                KEY_MECHANIC_NAME to mechanicName,
            )
        }
    }
}
