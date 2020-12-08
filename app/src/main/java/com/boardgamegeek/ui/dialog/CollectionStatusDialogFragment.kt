package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog.Builder
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import com.boardgamegeek.R
import kotlinx.android.synthetic.main.dialog_collection_status.*

class CollectionStatusDialogFragment : DialogFragment() {
    private lateinit var layout: View
    private var listener: Listener? = null

    interface Listener {
        fun onSelectStatuses(selectedStatuses: List<String>, wishlistPriority: Int)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? Listener
        if (listener == null) throw ClassCastException("$context must implement Listener")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        layout = LayoutInflater.from(context).inflate(R.layout.dialog_collection_status, null)

        val builder = Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.title_add_a_copy)
                .setView(layout)
                .setPositiveButton(R.string.ok) { _, _ ->
                    if (listener != null) {
                        val statuses = container.children.filterIsInstance<CheckBox>().filter {
                            it.isChecked
                        }.map { it.tag as String }.toList()
                        val wishlistPriority = if (wishlistView.isChecked) wishlistPriorityView.selectedItemPosition + 1 else 0
                        listener?.onSelectStatuses(statuses, wishlistPriority)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
        return builder.create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        wishlistPriorityView.adapter = WishlistPriorityAdapter(requireContext())
        wishlistPriorityView.isEnabled = wishlistView.isChecked
        wishlistView.setOnClickListener {
            wishlistPriorityView.isEnabled = wishlistView.isChecked
        }
    }

    private class WishlistPriorityAdapter(context: Context)
        : ArrayAdapter<String>(context,
            R.layout.spinner_textview,
            context.resources.getStringArray(R.array.wishlist_priority_finite)) {
        init {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }
}
