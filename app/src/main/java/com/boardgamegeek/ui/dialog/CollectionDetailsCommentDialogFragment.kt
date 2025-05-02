package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogEditTextWithTitlesBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollectionDetailsCommentDialogFragment : DialogFragment() {
    private var _binding: DialogEditTextWithTitlesBinding? = null
    private val binding get() = _binding!!
    private var originalText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditTextWithTitlesBinding.inflate(layoutInflater)

        val internalId = arguments?.getLong(INTERNAL_ID) ?: INVALID_ID.toLong()
        val viewModel = ViewModelProvider(requireActivity())[CollectionDetailsViewModel::class.java]
        val builder = requireContext().createThemedBuilder()
            //.setTitle(R.string.comment)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                viewModel.updateComment(internalId, binding.editText.text?.toString()?.trim().orEmpty())
            }

        return builder.create().apply {
            requestFocus(binding.editText)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val titleResId = arguments?.getInt(KEY_TITLE) ?: Resources.ID_NULL
        if (titleResId != Resources.ID_NULL) binding.titleView.setText(titleResId)
        binding.subtitleView.setTextOrHide(arguments?.getString(KEY_SUBTITLE))

        binding.editText.inputType = binding.editText.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
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
        private const val KEY_TITLE = "TITLE"
        private const val KEY_SUBTITLE = "SUBTITLE"
        private const val INTERNAL_ID = "INTERNAL_ID"
        private const val KEY_TEXT = "text"

        fun show(fragmentManager: FragmentManager, titleResId: Int, subtitle: String?, internalId: Long, text: String) {
            CollectionDetailsCommentDialogFragment().apply {
                arguments = bundleOf(
                    KEY_TEXT to text,
                    KEY_TITLE to titleResId,
                    INTERNAL_ID to internalId,
                ).apply {
                    if (!subtitle.isNullOrBlank()) {
                        putString(KEY_SUBTITLE, subtitle)
                    }
                }
            }.show(fragmentManager, CollectionDetailsCommentDialogFragment.TAG)
        }
    }
}
