package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.support.v7.app.AlertDialog
import android.support.v7.app.AlertDialog.Builder
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import com.boardgamegeek.R
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.FavoriteFilterer

class FavoriteFilterDialog : CollectionFilterDialog {
    lateinit var layout: View
    private val favoriteButton: RadioButton by lazy { layout.findViewById<RadioButton>(R.id.favorite) }
    private val notFavoriteButton: RadioButton by lazy { layout.findViewById<RadioButton>(R.id.not_favorite) }

    @SuppressLint("InflateParams")
    override fun createDialog(context: Context, listener: CollectionFilterDialog.OnFilterChangedListener, filter: CollectionFilterer) {
        layout = LayoutInflater.from(context).inflate(R.layout.dialog_collection_filter_favorite, null)
        initializeUi(filter)
        createAlertDialog(context, listener, layout).show()
    }

    private fun initializeUi(filter: CollectionFilterer) {
        val favoriteFilterer = filter as FavoriteFilterer?
        if (favoriteFilterer?.isFavorite == true) {
            favoriteButton.isChecked = true
        } else if (favoriteFilterer?.isFavorite == false) {
            notFavoriteButton.isChecked = true
        }
    }

    private fun createAlertDialog(context: Context, listener: CollectionFilterDialog.OnFilterChangedListener?, layout: View): AlertDialog {
        return Builder(context, R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.menu_favorite)
                .setPositiveButton(R.string.set) { _, _ ->
                    val filterer = FavoriteFilterer(context)
                    filterer.isFavorite = favoriteButton.isChecked
                    listener?.addFilter(filterer)
                }
                .setNegativeButton(R.string.clear) { _, _ ->
                    listener?.removeFilter(getType(context))
                }
                .setView(layout)
                .create()
    }

    override fun getType(context: Context) = FavoriteFilterer(context).type
}
