package com.boardgamegeek.ui;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.widget.PlayStatView.Builder;
import com.boardgamegeek.util.PreferencesUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class PlayStatsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	@InjectView(R.id.progress) View mProgressView;
	@InjectView(R.id.empty) View mEmptyView;
	@InjectView(R.id.data) View mDataView;
	@InjectView(R.id.table) TableLayout mTable;
	@InjectView(R.id.table_hindex) TableLayout mHIndexTable;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_play_stats, container, false);

		ButterKnife.inject(this, rootView);

		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().restartLoader(PlayCountQuery._TOKEN, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		CursorLoader loader = null;
		switch (id) {
			case PlayCountQuery._TOKEN:
				Uri uri = Plays.CONTENT_URI.buildUpon()
					.appendQueryParameter(BggContract.QUERY_KEY_GROUP_BY, BggContract.PlayItems.OBJECT_ID)
					.build();
				loader = new CursorLoader(getActivity(), uri,
					PlayCountQuery.PROJECTION,
					getSelection(),
					getSelectionArgs(),
					Plays.SUM_QUANTITY + " DESC, " + Games.GAME_NAME + " ASC");
				loader.setUpdateThrottle(2000);
				break;
		}
		return loader;
	}

	@NonNull
	private String getSelection() {
		String selection = Plays.SYNC_STATUS + "=?";
		if (!PreferencesUtils.logPlayStatsIncomplete(getActivity())) {
			selection += " AND " + Plays.INCOMPLETE + "!=?";
		}
		if (!PreferencesUtils.logPlayStatsExpansions(getActivity()) &&
			!PreferencesUtils.logPlayStatsAccessories(getActivity())) {
			selection += " AND " + Games.SUBTYPE + "=?";
		} else if (!PreferencesUtils.logPlayStatsExpansions(getActivity()) ||
			!PreferencesUtils.logPlayStatsAccessories(getActivity())) {
			selection += " AND " + Games.SUBTYPE + "!=?";
		}
		return selection;
	}

	@NonNull
	private String[] getSelectionArgs() {
		List<String> args = new ArrayList<>();
		args.add(String.valueOf(Play.SYNC_STATUS_SYNCED));
		if (!PreferencesUtils.logPlayStatsIncomplete(getActivity())) {
			args.add("1");
		}

		if (!PreferencesUtils.logPlayStatsExpansions(getActivity()) &&
			!PreferencesUtils.logPlayStatsAccessories(getActivity())) {
			args.add(BggService.THING_SUBTYPE_BOARDGAME);
		} else if (!PreferencesUtils.logPlayStatsExpansions(getActivity())) {
			args.add(BggService.THING_SUBTYPE_BOARDGAME_EXPANSION);
		} else if (!PreferencesUtils.logPlayStatsAccessories(getActivity())) {
			args.add(BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
		}
		return args.toArray(new String[args.size()]);
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
			case PlayCountQuery._TOKEN:
				Stats stats = new Stats(cursor);
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

	private void bindUi(Stats stats) {
		mTable.removeAllViews();
		addStatRow(mTable, new Builder().labelId(R.string.play_stat_play_count).value(stats.numberOfPlays));
		addStatRow(mTable, new Builder().labelId(R.string.play_stat_distinct_games).value(stats.numberOfGames));
		addStatRow(mTable, new Builder().labelId(R.string.play_stat_quarters).value(stats.quarters));
		addStatRow(mTable, new Builder().labelId(R.string.play_stat_dimes).value(stats.dimes));
		addStatRow(mTable, new Builder().labelId(R.string.play_stat_nickels).value(stats.nickels));

		addStatRow(mHIndexTable, new Builder().labelId(R.string.play_stat_h_index).value(stats.hIndex).infoId(R.string.play_stat_h_index_info));
		addDivider(mHIndexTable);
		boolean addDivider = true;
		for (Pair<String, Integer> game : stats.hIndexGames) {
			final Builder builder = new Builder().labelText(game.first).value(game.second);
			if (game.second == stats.hIndex) {
				builder.backgroundResource(R.color.primary);
				addDivider = false;
			} else if (game.second < stats.hIndex && addDivider) {
				addDivider(mHIndexTable);
				addDivider = false;
			}
			addStatRow(mHIndexTable, builder);
		}
	}

	private void showEmpty() {
		mProgressView.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
		mEmptyView.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
		mProgressView.setVisibility(View.GONE);
		mEmptyView.setVisibility(View.VISIBLE);
		mDataView.setVisibility(View.GONE);
	}

	private void showData() {
		mProgressView.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
		mDataView.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
		mProgressView.setVisibility(View.GONE);
		mEmptyView.setVisibility(View.GONE);
		mDataView.setVisibility(View.VISIBLE);
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

	private static class Stats {
		private static final int MIN_H_INDEX_GAMES = 2;
		private static final int MAX_H_INDEX_GAMES = 6;
		int numberOfPlays = 0;
		int numberOfGames = 0;
		int quarters = 0;
		int dimes = 0;
		int nickels = 0;
		int hIndex = 0;
		int hIndexCounter = 1;
		List<Pair<String, Integer>> hIndexGames = new ArrayList<>();
		private Stack<Pair<String, Integer>> hIndexGamesStack = new Stack<>();
		private int postIndexCount = 0;
		private int priorPlayCount;

		public Stats(Cursor cursor) {
			init(cursor);
		}

		private void init(Cursor cursor) {
			do {
				int playCount = cursor.getInt(PlayCountQuery.SUM_QUANTITY);
				String gameName = cursor.getString(PlayCountQuery.GAME_NAME);

				numberOfPlays += playCount;
				numberOfGames++;

				if (playCount >= 25) {
					quarters++;
				} else if (playCount >= 10) {
					dimes++;
				} else if (playCount > 5) {
					nickels++;
				}

				if (hIndex == 0) {
					hIndexGamesStack.push(new Pair<>(gameName, playCount));
					if (hIndexCounter > playCount) {
						hIndex = hIndexCounter - 1;
						int preIndexCount = 0;
						while (!hIndexGamesStack.isEmpty()) {
							Pair<String, Integer> game = hIndexGamesStack.pop();
							if (preIndexCount < MIN_H_INDEX_GAMES) {
								hIndexGames.add(0, game);
								if (game.second != hIndex) {
									preIndexCount++;
									priorPlayCount = game.second;
								}
							} else if (preIndexCount >= MAX_H_INDEX_GAMES) {
								//do nothing
							} else if (game.second == priorPlayCount) {
								hIndexGames.add(0, game);
								preIndexCount++;
							}
						}
					}
					hIndexCounter++;
				} else {
					if (postIndexCount < MIN_H_INDEX_GAMES) {
						hIndexGames.add(new Pair<>(gameName, playCount));
						postIndexCount++;
						priorPlayCount = playCount;
					} else if (postIndexCount >= MAX_H_INDEX_GAMES) {
						// do nothing
					} else if (playCount == priorPlayCount) {
						hIndexGames.add(new Pair<>(gameName, playCount));
						postIndexCount++;
					}
				}
			} while (cursor.moveToNext());
		}
	}

	private interface PlayCountQuery {
		int _TOKEN = 0x01;
		String[] PROJECTION = { Plays.SUM_QUANTITY, Games.GAME_NAME };
		int SUM_QUANTITY = 0;
		int GAME_NAME = 1;
	}
}
