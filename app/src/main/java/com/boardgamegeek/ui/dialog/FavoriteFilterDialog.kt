package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.FavoriteFilterer
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel

class FavoriteFilterDialog : CollectionFilterDialog {
    lateinit var layout: View
    private val favoriteButton: RadioButton by lazy { layout.findViewById(R.id.favorite) }
    private val notFavoriteButton: RadioButton by lazy { layout.findViewById(R.id.not_favorite) }

    @SuppressLint("InflateParams")
    override fun createDialog(activity: FragmentActivity, filter: CollectionFilterer?) {
        val viewModel by lazy { ViewModelProvider(activity)[CollectionViewViewModel::class.java] }
        layout = LayoutInflater.from(activity).inflate(R.layout.dialog_collection_filter_favorite, null)
        (filter as? FavoriteFilterer)?.let {
            if (it.isFavorite) {
                favoriteButton.isChecked = true
            } else {
                notFavoriteButton.isChecked = true
            }
        }
        activity.createThemedBuilder()
            .setTitle(R.string.menu_favorite)
            .setPositiveButton(R.string.set) { _, _ ->
                viewModel.addFilter(FavoriteFilterer(activity).apply {
                    isFavorite = favoriteButton.isChecked
                })
            }
            .setNegativeButton(R.string.clear) { _, _ ->
                viewModel.removeFilter(getType(activity))
            }
            .setView(layout)
            .create()
            .show()
    }

    override fun getType(context: Context) = FavoriteFilterer(context).type
}
