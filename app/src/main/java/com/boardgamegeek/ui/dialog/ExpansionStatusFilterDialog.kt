package com.boardgamegeek.ui.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.boardgamegeek.R
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.ExpansionStatusFilterer

class ExpansionStatusFilterDialog : CollectionFilterDialog {
    private var selectedSubtype = 0

    override fun createDialog(context: Context, listener: CollectionFilterDialog.OnFilterChangedListener?, filter: CollectionFilterer?) {
        selectedSubtype = (filter as ExpansionStatusFilterer?)?.selectedSubtype ?: 0
        AlertDialog.Builder(context, R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.menu_expansion_status)
                .setSingleChoiceItems(R.array.expansion_status_filter, selectedSubtype) { _, which -> selectedSubtype = which }
                .setPositiveButton(R.string.ok) { _, _ ->
                    if (listener != null) {
                        if (selectedSubtype == 0) {
                            listener.removeFilter(getType(context))
                        } else {
                            val filterer = ExpansionStatusFilterer(context)
                            filterer.selectedSubtype = selectedSubtype
                            listener.addFilter(filterer)
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show()
    }

    override fun getType(context: Context): Int {
        return ExpansionStatusFilterer(context).type
    }
}
