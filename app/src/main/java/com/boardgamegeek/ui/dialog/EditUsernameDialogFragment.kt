package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doBeforeTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogEditUsernameBinding
import com.boardgamegeek.extensions.getTextColor
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.ui.viewmodel.BuddyViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditUsernameDialogFragment : DialogFragment() {
    private var _binding: DialogEditUsernameBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<BuddyViewModel>()
    private var isValid: Boolean? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditUsernameBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
            .setTitle(R.string.title_add_username)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                viewModel.addUsernameToPlayer(getUsername())
            }

        return builder.create().apply {
            requestFocus(binding.editText)
        }
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.editText.inputType = binding.editText.inputType or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        binding.editTextContainer.hint = getString(R.string.username)
        binding.editText.doBeforeTextChanged { _, _, _, _ ->
            isValid = null
            bindButton()
        }
        binding.editText.setOnEditorActionListener { _, _, event ->
            if (event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                resetValidation()
                true
            } else false
        }
        binding.button.setOnClickListener {
            resetValidation()
        }

        viewModel.isUsernameValid.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                binding.progressView.isInvisible = true
                isValid = it
                bindButton()
            }
        }
    }

    private fun resetValidation() {
        binding.progressView.isVisible = true
        isValid = null
        bindButton()
        viewModel.validateUsername(getUsername())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getUsername() = binding.editText.text?.toString().orEmpty().trim()

    private fun bindButton() {
        when (isValid) {
            null -> {
                binding.button.setText(R.string.validate)
                setButtonColor(Color.TRANSPARENT)
                binding.button.setIconResource(R.drawable.ic_baseline_sync_24)
            }
            true -> {
                binding.button.setText(R.string.valid)
                setButtonColor(ContextCompat.getColor(requireContext(), R.color.valid))
                binding.button.setIconResource(R.drawable.ic_baseline_check_circle_24)
            }
            else -> {
                binding.button.setText(R.string.not_found)
                setButtonColor(ContextCompat.getColor(requireContext(), R.color.invalid))
                binding.button.setIconResource(R.drawable.ic_baseline_cancel_24)
            }
        }
    }

    private fun setButtonColor(backgroundColor: Int) {
        binding.button.setBackgroundColor(backgroundColor)
        binding.button.iconTint = ColorStateList.valueOf(backgroundColor.getTextColor())
        binding.button.setTextColor(backgroundColor.getTextColor())
    }
}
