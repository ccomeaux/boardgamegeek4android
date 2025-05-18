package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogEditConditionTextBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollectionDetailsConditionDialogFragment : DialogFragment() {
    private var _binding: DialogEditConditionTextBinding? = null
    private val binding get() = _binding!!
    private var originalText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditConditionTextBinding.inflate(layoutInflater)

        val internalId = arguments?.getLong(INTERNAL_ID) ?: INVALID_ID.toLong()
        val viewModel = ViewModelProvider(requireActivity())[CollectionDetailsViewModel::class.java]
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                viewModel.updateCondition(internalId, binding.editText.text?.toString()?.trim().orEmpty())
            }

        return builder.create().apply {
            requestFocus(binding.editText)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.gameNameView.setTextOrHide(arguments?.getString(KEY_GAME_NAME))
        if (savedInstanceState == null) {
            originalText = arguments?.getString(KEY_TEXT)
            binding.editText.setAndSelectExistingText(originalText)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val INTERNAL_ID = "INTERNAL_ID"
        private const val KEY_TEXT = "TEXT"

        fun show(fragmentManager: FragmentManager, gameName: String?, collectionItemInternalId: Long, conditionText: String) {
            CollectionDetailsConditionDialogFragment().apply {
                arguments = bundleOf(
                    KEY_TEXT to conditionText,
                    INTERNAL_ID to collectionItemInternalId,
                ).apply {
                    if (!gameName.isNullOrBlank()) {
                        putString(KEY_GAME_NAME, gameName)
                    }
                }
            }.show(fragmentManager, CollectionDetailsCommentDialogFragment.TAG)
        }
    }
}
