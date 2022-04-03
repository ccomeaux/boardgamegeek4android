package com.boardgamegeek.ui.dialog

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
import com.boardgamegeek.databinding.DialogCollectionStatusBinding

class CollectionStatusDialogFragment : DialogFragment() {
    private var _binding: DialogCollectionStatusBinding? = null
    private val binding get() = _binding!!
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
        _binding = DialogCollectionStatusBinding.inflate(layoutInflater)

        val builder = Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
            .setTitle(R.string.title_add_a_copy)
            .setView(binding.root)
            .setPositiveButton(R.string.ok) { _, _ ->
                if (listener != null) {
                    val statuses = binding.container.children.filterIsInstance<CheckBox>().filter {
                        it.isChecked
                    }.map { it.tag as String }.toList()
                    val wishlistPriority = if (binding.wishlistView.isChecked) binding.wishlistPriorityView.selectedItemPosition + 1 else 0
                    listener?.onSelectStatuses(statuses, wishlistPriority)
                }
            }
            .setNegativeButton(R.string.cancel, null)
        return builder.create()
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.wishlistPriorityView.adapter = WishlistPriorityAdapter(requireContext())
        binding.wishlistPriorityView.isEnabled = binding.wishlistView.isChecked
        binding.wishlistView.setOnClickListener {
            binding.wishlistPriorityView.isEnabled = binding.wishlistView.isChecked
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class WishlistPriorityAdapter(context: Context) : ArrayAdapter<String>(
        context,
        R.layout.spinner_textview,
        context.resources.getStringArray(R.array.wishlist_priority_finite)
    ) {
        init {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }
}
