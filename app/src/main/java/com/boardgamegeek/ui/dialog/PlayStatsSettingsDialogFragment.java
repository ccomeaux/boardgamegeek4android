package com.boardgamegeek.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ScrollView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.PreferencesUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;

public class PlayStatsSettingsDialogFragment extends DialogFragment {
	private ViewGroup root;
	@BindView(R.id.scroll_container) ScrollView scrollContainer;
	@BindView(R.id.incomplete) CheckBox includeIncompleteGamesView;
	@BindView(R.id.expansions) CheckBox includeExpansionsView;
	@BindView(R.id.accessories) CheckBox includeAccessoriesView;

	@DebugLog
	public PlayStatsSettingsDialogFragment() {
	}

	@DebugLog
	public static PlayStatsSettingsDialogFragment newInstance(@Nullable ViewGroup root) {
		final PlayStatsSettingsDialogFragment fragment = new PlayStatsSettingsDialogFragment();
		fragment.root = root;
		return fragment;
	}

	@DebugLog
	@Override
	@NonNull
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
		View rootView = layoutInflater.inflate(R.layout.dialog_play_stats_settings, root, false);

		ButterKnife.bind(this, rootView);
		bindUi();

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
			.setView(rootView)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					PreferencesUtils.putPlayStatsIncomplete(getActivity(), includeIncompleteGamesView.isChecked());
					PreferencesUtils.putPlayStatsExpansions(getActivity(), includeExpansionsView.isChecked());
					PreferencesUtils.putPlayStatsAccessories(getActivity(), includeAccessoriesView.isChecked());
				}
			});
		builder.setTitle(R.string.title_settings);
		return builder.create();
	}

	private void bindUi() {
		includeIncompleteGamesView.setChecked(PreferencesUtils.logPlayStatsIncomplete(getActivity()));
		includeExpansionsView.setChecked(PreferencesUtils.logPlayStatsExpansions(getActivity()));
		includeAccessoriesView.setChecked(PreferencesUtils.logPlayStatsAccessories(getActivity()));
	}
}
