package com.boardgamegeek.ui;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TableLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.dialog.PlayStatsSettingsDialogFragment;
import com.boardgamegeek.ui.model.PlayStats;
import com.boardgamegeek.ui.widget.PlayStatView.Builder;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PlayStatsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
	SharedPreferences.OnSharedPreferenceChangeListener {
	private static final int TOKEN = 0x01;
	@SuppressWarnings("unused") @Bind(R.id.progress) View progressView;
	@SuppressWarnings("unused") @Bind(R.id.empty) View emptyView;
	@SuppressWarnings("unused") @Bind(R.id.data) ViewGroup dataView;
	@SuppressWarnings("unused") @Bind(R.id.table) TableLayout table;
	@SuppressWarnings("unused") @Bind(R.id.table_hindex) TableLayout hIndexTable;
	@SuppressWarnings("unused") @Bind(R.id.accuracy_message) TextView accuracyMessage;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_play_stats, container, false);
		ButterKnife.bind(this, rootView);
		bindAccuracyMessage();
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().restartLoader(TOKEN, null, this);
	}

	@Override
	public void onResume() {
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		CursorLoader loader = null;
		switch (id) {
			case TOKEN:
				loader = new CursorLoader(getActivity(),
					PlayStats.getUri(),
					PlayStats.PROJECTION,
					PlayStats.getSelection(getActivity()),
					PlayStats.getSelectionArgs(getActivity()),
					PlayStats.getSortOrder());
				loader.setUpdateThrottle(2000);
				break;
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (cursor == null || !cursor.moveToFirst()) {
			showEmpty();
			return;
		}

		int token = loader.getId();
		switch (token) {
			case TOKEN:
				PlayStats stats = PlayStats.fromCursor(cursor);
				bindUi(stats);
				showData();
				break;
			default:
				cursor.close();
				break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	private void bindAccuracyMessage() {
		List<String> things = new ArrayList<>(3);
		if (!PreferencesUtils.logPlayStatsIncomplete(getActivity())) {
			things.add(getString(R.string.incomplete_games).toLowerCase());
		}
		if (!PreferencesUtils.logPlayStatsExpansions(getActivity())) {
			things.add(getString(R.string.expansions).toLowerCase());
		}
		if (!PreferencesUtils.logPlayStatsAccessories(getActivity())) {
			things.add(getString(R.string.accessories).toLowerCase());
		}
		accuracyMessage.setVisibility(things.size() == 0 ? View.GONE : View.VISIBLE);
		accuracyMessage.setText(getString(R.string.play_stat_status_accuracy,
			StringUtils.formatList(things, getString(R.string.or).toLowerCase(), ",")));
	}

	private void bindUi(PlayStats stats) {
		table.removeAllViews();
		addStatRow(table, new Builder().labelId(R.string.play_stat_play_count).value(stats.getNumberOfPlays()));
		addStatRow(table, new Builder().labelId(R.string.play_stat_distinct_games).value(stats.getNumberOfGames()));
		addStatRow(table, new Builder().labelId(R.string.play_stat_quarters).value(stats.getNumberOfQuarters()));
		addStatRow(table, new Builder().labelId(R.string.play_stat_dimes).value(stats.getNumberOfDimes()));
		addStatRow(table, new Builder().labelId(R.string.play_stat_nickels).value(stats.getNumberOfNickels()));

		hIndexTable.removeAllViews();
		addStatRow(hIndexTable, new Builder().labelId(R.string.play_stat_h_index).value(stats.getHIndex()).infoId(R.string.play_stat_h_index_info));
		addDivider(hIndexTable);
		boolean addDivider = true;
		for (Pair<String, Integer> game : stats.getHIndexGames()) {
			final Builder builder = new Builder().labelText(game.first).value(game.second);
			if (game.second == stats.getHIndex()) {
				builder.backgroundResource(R.color.primary);
				addDivider = false;
			} else if (game.second < stats.getHIndex() && addDivider) {
				addDivider(hIndexTable);
				addDivider = false;
			}
			addStatRow(hIndexTable, builder);
		}

		PreferencesUtils.updateHIndex(getActivity(), stats.getHIndex());
	}

	private void showEmpty() {
		progressView.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
		emptyView.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
		progressView.setVisibility(View.GONE);
		emptyView.setVisibility(View.VISIBLE);
		dataView.setVisibility(View.GONE);
	}

	private void showData() {
		progressView.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
		dataView.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
		progressView.setVisibility(View.GONE);
		emptyView.setVisibility(View.GONE);
		dataView.setVisibility(View.VISIBLE);
	}

	private void addStatRow(ViewGroup container, Builder builder) {
		container.addView(builder.build(getActivity()));
	}

	private void addDivider(ViewGroup container) {
		View view = new View(getActivity());
		view.setLayoutParams(new TableLayout.LayoutParams(0, 1));
		view.setBackgroundResource(R.color.primary_dark);
		container.addView(view);
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.settings)
	void onSettingsClick(@SuppressWarnings("UnusedParameters") View v) {
		PlayStatsSettingsDialogFragment df = PlayStatsSettingsDialogFragment.newInstance(dataView);
		DialogUtils.showFragment(getActivity(), df, "play_stats_settings");
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.startsWith(PreferencesUtils.LOG_PLAY_STATS_PREFIX)) {
			bindAccuracyMessage();
			getLoaderManager().restartLoader(TOKEN, null, this);
		}
	}
}
