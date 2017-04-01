package com.boardgamegeek.ui;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.dialog.PlayStatsIncludeSettingsDialogFragment;
import com.boardgamegeek.ui.model.PlayStats;
import com.boardgamegeek.ui.widget.PlayStatView.Builder;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class PlayStatsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
	SharedPreferences.OnSharedPreferenceChangeListener {
	private static final int TOKEN = 0x01;
	private Unbinder unbinder;
	@BindView(R.id.progress) View progressView;
	@BindView(R.id.empty) View emptyView;
	@BindView(R.id.data) ViewGroup dataView;
	@BindView(R.id.table) TableLayout table;
	@BindView(R.id.table_hindex) TableLayout hIndexTable;
	@BindView(R.id.collection_status_container) ViewGroup collectionStatusContainer;
	@BindView(R.id.accuracy_container) ViewGroup accuracyContainer;
	@BindView(R.id.accuracy_message) TextView accuracyMessage;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_play_stats, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		bindCollectionStatusMessage();
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
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
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
				PreferencesUtils.updateHIndex(getActivity(), stats.getHIndex());
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

	private void bindCollectionStatusMessage() {
		boolean syncOwned = PreferencesUtils.isSyncStatus(getContext(), BggService.COLLECTION_QUERY_STATUS_OWN);
		boolean syncPlayed = PreferencesUtils.isSyncStatus(getContext(), BggService.COLLECTION_QUERY_STATUS_PLAYED);
		collectionStatusContainer.setVisibility(syncOwned && syncPlayed ? View.GONE : View.VISIBLE);
	}

	private void bindAccuracyMessage() {
		List<String> things = new ArrayList<>(3);
		if (!PreferencesUtils.logPlayStatsIncomplete(getContext())) {
			things.add(getString(R.string.incomplete_games).toLowerCase());
		}
		if (!PreferencesUtils.logPlayStatsExpansions(getContext())) {
			things.add(getString(R.string.expansions).toLowerCase());
		}
		if (!PreferencesUtils.logPlayStatsAccessories(getContext())) {
			things.add(getString(R.string.accessories).toLowerCase());
		}
		accuracyContainer.setVisibility(things.size() == 0 ? View.GONE : View.VISIBLE);
		accuracyMessage.setText(getString(R.string.play_stat_accuracy,
			StringUtils.formatList(things, getString(R.string.or).toLowerCase(), ",")));
	}

	private void bindUi(PlayStats stats) {
		table.removeAllViews();
		addStatRow(table, new Builder().labelId(R.string.play_stat_play_count).value(stats.getNumberOfPlays()));
		addStatRow(table, new Builder().labelId(R.string.play_stat_distinct_games).value(stats.getNumberOfGames()));
		addStatRow(table, new Builder().labelId(R.string.play_stat_quarters).value(stats.getNumberOfQuarters()));
		addStatRow(table, new Builder().labelId(R.string.play_stat_dimes).value(stats.getNumberOfDimes()));
		addStatRow(table, new Builder().labelId(R.string.play_stat_nickels).value(stats.getNumberOfNickels()));
		addStatRow(table, new Builder().labelId(R.string.play_stat_top_100).value(stats.getTop100Count() + "%"));

		hIndexTable.removeAllViews();
		addStatRow(hIndexTable, new Builder().labelId(R.string.play_stat_h_index).value(stats.getHIndex()).infoId(R.string.play_stat_h_index_info));
		addDivider(hIndexTable);
		boolean addDivider = true;
		for (Pair<String, Integer> game : stats.getHIndexGames()) {
			final Builder builder = new Builder().labelText(game.first).value(game.second);
			if (game.second == stats.getHIndex()) {
				builder.backgroundResource(R.color.light_blue_transparent);
				addDivider = false;
			} else if (game.second < stats.getHIndex() && addDivider) {
				addDivider(hIndexTable);
				addDivider = false;
			}
			addStatRow(hIndexTable, builder);
		}
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
		view.setBackgroundResource(R.color.dark_blue);
		container.addView(view);
	}

	@OnClick(R.id.settings_collection_status)
	void onSettingsCollectionStatusClick() {
		DialogUtils.createConfirmationDialog(getContext(), R.string.play_stat_msg_collection_status, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				PreferencesUtils.addSyncStatus(getContext(), BggService.COLLECTION_QUERY_STATUS_OWN);
				PreferencesUtils.addSyncStatus(getContext(), BggService.COLLECTION_QUERY_STATUS_PLAYED);
				SyncService.sync(getContext(), SyncService.FLAG_SYNC_COLLECTION);
				bindCollectionStatusMessage();
			}
		}).show();
	}

	@OnClick(R.id.settings_include)
	void onSettingsIncludeClick() {
		PlayStatsIncludeSettingsDialogFragment df = PlayStatsIncludeSettingsDialogFragment.newInstance(accuracyContainer);
		DialogUtils.showFragment(getActivity(), df, "play_stats_settings_include");
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.startsWith(PreferencesUtils.LOG_PLAY_STATS_PREFIX)) {
			bindAccuracyMessage();
			getLoaderManager().restartLoader(TOKEN, null, this);
		}
	}
}
