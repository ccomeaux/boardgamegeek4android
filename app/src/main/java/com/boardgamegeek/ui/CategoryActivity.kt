package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.CategoryViewModel
import org.jetbrains.anko.startActivity

class CategoryActivity : SimpleSinglePaneActivity() {
    private val viewModel: CategoryViewModel by lazy {
        ViewModelProviders.of(this).get(CategoryViewModel::class.java)
    }

    override fun onCreatePane(intent: Intent) = CategoryCollectionFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(KEY_CATEGORY_ID, BggContract.INVALID_ID)
        val name = intent.getStringExtra(KEY_CATEGORY_NAME)
        title = name
        setSubtitle(getString(R.string.title_category))
        viewModel.setId(id)
    }

    companion object {
        private const val KEY_CATEGORY_ID = "CATEGORY_ID"
        private const val KEY_CATEGORY_NAME = "CATEGORY_NAME"

        fun start(context: Context, categoryId: Int, categoryName: String) {
            context.startActivity<CategoryActivity>(
                    KEY_CATEGORY_ID to categoryId,
                    KEY_CATEGORY_NAME to categoryName
            )
        }
    }
}