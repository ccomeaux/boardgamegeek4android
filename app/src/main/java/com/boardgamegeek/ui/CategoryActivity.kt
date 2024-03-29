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
import com.boardgamegeek.ui.viewmodel.CategoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryActivity : SimpleSinglePaneActivity() {
    private var id = BggContract.INVALID_ID

    private val viewModel by viewModels<CategoryViewModel>()

    override fun onCreatePane(intent: Intent) = CategoryCollectionFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = intent.getIntExtra(KEY_CATEGORY_ID, BggContract.INVALID_ID)
        title = intent.getStringExtra(KEY_CATEGORY_NAME)
        supportActionBar?.subtitle = getString(R.string.title_category)
        viewModel.setId(id)
    }

    override val optionsMenuId = R.menu.category

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_view -> linkToBgg("boardgamecategory", id)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    companion object {
        private const val KEY_CATEGORY_ID = "CATEGORY_ID"
        private const val KEY_CATEGORY_NAME = "CATEGORY_NAME"

        fun start(context: Context, categoryId: Int, categoryName: String) {
            context.startActivity<CategoryActivity>(
                KEY_CATEGORY_ID to categoryId,
                KEY_CATEGORY_NAME to categoryName,
            )
        }
    }
}
