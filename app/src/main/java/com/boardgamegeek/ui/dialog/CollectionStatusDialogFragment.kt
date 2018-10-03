package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog.Builder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import com.boardgamegeek.R
import com.boardgamegeek.extensions.children
import kotlinx.android.synthetic.main.dialog_collection_status.*
import org.jetbrains.anko.support.v4.ctx

class CollectionStatusDialogFragment : DialogFragment() {
    private lateinit var layout: View
    private var listener: Listener? = null

    interface Listener {
        fun onSelectStatuses(selectedStatuses: List<String>, wishlistPriority: Int)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as? Listener
        if (listener == null) throw ClassCastException("$context must implement Listener")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        layout = LayoutInflater.from(ctx).inflate(R.layout.dialog_collection_status, null)

        val builder = Builder(ctx, R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.title_add_a_copy)
                .setView(layout)
                .setPositiveButton(R.string.ok) { _, _ ->
                    if (listener != null) {
                        val statuses = container.children().filterIsInstance<CheckBox>().filter {
                            it.isChecked
                        }.map { it.tag as String }
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
        wishlistPriorityView.adapter = WishlistPriorityAdapter(ctx)
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

    companion object {
        fun newInstance(): CollectionStatusDialogFragment {
            return CollectionStatusDialogFragment()
        }
    }
}
