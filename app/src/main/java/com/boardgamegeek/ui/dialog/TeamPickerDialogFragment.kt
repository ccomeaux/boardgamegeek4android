package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogTeamsBinding
import com.boardgamegeek.databinding.RowTeamBinding
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import kotlin.properties.Delegates

class TeamPickerDialogFragment : DialogFragment() {
    private var _binding: DialogTeamsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTeamsBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert).setView(binding.root)
        val playerName = arguments?.getString(KEY_PLAYER_NAME).orEmpty()
        if (playerName.isBlank()) {
            builder.setTitle(R.string.team_color)
        } else {
            builder.setTitle(getString(R.string.team_color_prefix, playerName))
        }
        return builder.create()
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val requestCode = arguments?.getInt(KEY_REQUEST_CODE) ?: 0
        val playerTeam = arguments?.getString(KEY_PLAYER_TEAM).orEmpty()

        val viewModel by activityViewModels<NewPlayViewModel>()
        val adapter = TeamAdapter(this, viewModel, requestCode, playerTeam)
        binding.recyclerView.adapter = adapter

        binding.addButton.setOnClickListener {
            NewPlayAddTeamColorDialogFragment.newInstance(requestCode).show(parentFragmentManager, "")
            dismiss()
        }

        viewModel.gameColors.observe(this) {
            it?.let { adapter.teams = it }
        }
        viewModel.selectedColors.observe(this) {
            adapter.selectedColors = it
        }
    }

    companion object {
        private const val KEY_REQUEST_CODE = "request_code"
        private const val KEY_PLAYER_NAME = "player_name"
        private const val KEY_PLAYER_TEAM = "player_team"

        fun launch(activity: FragmentActivity, playerIndex: Int, playerName: String, playerTeam: String) {
            val df = TeamPickerDialogFragment().apply {
                arguments = bundleOf(
                    KEY_REQUEST_CODE to playerIndex,
                    KEY_PLAYER_NAME to playerName,
                    KEY_PLAYER_TEAM to playerTeam,
                )
            }
            activity.showAndSurvive(df)
        }
    }

    class TeamAdapter(
        private val df: DialogFragment,
        private val viewModel: NewPlayViewModel,
        private val playerIndex: Int,
        private val playerTeam: String
    ) : RecyclerView.Adapter<TeamAdapter.ViewHolder>(), AutoUpdatableAdapter {

        private val textColor = ContextCompat.getColor(df.requireContext(), R.color.primary_text)
        private val disabledTextColor = ContextCompat.getColor(df.requireContext(), R.color.disabled_text)

        var selectedColors: List<String>? = null
        var teams: List<String> by Delegates.observable(emptyList()) { _, old, new ->
            autoNotify(old, new) { o, n -> o == n }
        }

        override fun getItemCount() = teams.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(parent.inflate(R.layout.row_team))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(teams.getOrNull(position))
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val binding = RowTeamBinding.bind(itemView)

            fun bind(team: String?) {
                team?.let {
                    binding.nameView.text = it
                    itemView.setOnClickListener { _ ->
                        viewModel.addColorToPlayer(playerIndex, it)
                        df.dismiss()
                    }
                    when {
                        it == playerTeam -> {
                            binding.nameView.setTypeface(binding.nameView.typeface, Typeface.BOLD)
                            binding.nameView.setTextColor(textColor)
                        }
                        selectedColors?.contains(it) == true -> {
                            binding.nameView.setTypeface(binding.nameView.typeface, Typeface.NORMAL)
                            binding.nameView.setTextColor(disabledTextColor)
                        }
                        else -> {
                            binding.nameView.setTypeface(binding.nameView.typeface, Typeface.NORMAL)
                            binding.nameView.setTextColor(textColor)
                        }
                    }
                }
            }
        }
    }
}
