package com.boardgamegeek.ui;

import android.database.Cursor;
import android.os.Bundle;
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

import com.boardgamegeek.R;
import com.boardgamegeek.ui.model.PlayStats;
import com.boardgamegeek.ui.widget.PlayStatView.Builder;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class PlayStatsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final int TOKEN = 0x01;
	@SuppressWarnings("unused") @InjectView(R.id.progress) View progressView;
	@SuppressWarnings("unused") @InjectView(R.id.empty) View emptyView;
	@SuppressWarnings("unused") @InjectView(R.id.data) View dataView;
	@SuppressWarnings("unused") @InjectView(R.id.table) TableLayout table;
	@SuppressWarnings("unused") @InjectView(R.id.table_hindex) TableLayout hIndexTable;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_play_stats, container, false);
		ButterKnife.inject(this, rootView);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().restartLoader(TOKEN, null, this);
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
}
