package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import kotlinx.android.synthetic.main.dialog_teams.view.*
import kotlinx.android.synthetic.main.row_team.view.*
import org.jetbrains.anko.support.v4.withArguments
import kotlin.properties.Delegates

class TeamPickerDialogFragment : DialogFragment() {
    private lateinit var layout: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        layout = LayoutInflater.from(context).inflate(R.layout.dialog_teams, null)

        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert).setView(layout)
        val playerName = arguments?.getString(KEY_PLAYER_NAME) ?: ""
        if (playerName.isBlank()) {
            builder.setTitle(R.string.team_color)
        } else {
            builder.setTitle(getString(R.string.team_color_prefix, playerName))
        }
        return builder.create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val requestCode = arguments?.getInt(KEY_REQUEST_CODE) ?: 0
        val playerTeam = arguments?.getString(KEY_PLAYER_TEAM) ?: ""

        val viewModel by activityViewModels<NewPlayViewModel>()
        val adapter = TeamAdapter(this, viewModel, requestCode, playerTeam)
        layout.findViewById<RecyclerView>(R.id.recyclerView).adapter = adapter

        layout.addButton.setOnClickListener {
            NewPlayAddTeamColorDialogFragment.newInstance(requestCode).show(parentFragmentManager, "")
            dismiss()
        }

        viewModel.gameColors.observe(this, Observer {
            adapter.teams = it
        })
        viewModel.selectedColors.observe(this, Observer {
            adapter.selectedColors = it
        })
    }

    companion object {
        private const val KEY_REQUEST_CODE = "request_code"
        private const val KEY_PLAYER_NAME = "player_name"
        private const val KEY_PLAYER_TEAM = "player_team"

        fun launch(activity: FragmentActivity, playerIndex: Int, playerName: String, playerTeam: String) {
            val df = TeamPickerDialogFragment().withArguments(
                    KEY_REQUEST_CODE to playerIndex,
                    KEY_PLAYER_NAME to playerName,
                    KEY_PLAYER_TEAM to playerTeam
            )
            activity.showAndSurvive(df)
        }
    }

    class TeamAdapter(private val df: DialogFragment, private val viewModel: NewPlayViewModel, private val playerIndex: Int, private val playerTeam: String)
        : RecyclerView.Adapter<TeamAdapter.ViewHolder>(), AutoUpdatableAdapter {

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
            fun bind(team: String?) {
                team?.let {
                    itemView.nameView.text = it
                    itemView.setOnClickListener { _ ->
                        viewModel.addColorToPlayer(playerIndex, it)
                        df.dismiss()
                    }
                    when {
                        it == playerTeam -> {
                            itemView.nameView.setTypeface(itemView.nameView.typeface, Typeface.BOLD)
                            itemView.nameView.setTextColor(textColor)
                        }
                        selectedColors?.contains(it) == true -> {
                            itemView.nameView.setTypeface(itemView.nameView.typeface, Typeface.NORMAL)
                            itemView.nameView.setTextColor(disabledTextColor)
                        }
                        else -> {
                            itemView.nameView.setTypeface(itemView.nameView.typeface, Typeface.NORMAL)
                            itemView.nameView.setTextColor(textColor)
                        }
                    }
                }
            }
        }
    }
}