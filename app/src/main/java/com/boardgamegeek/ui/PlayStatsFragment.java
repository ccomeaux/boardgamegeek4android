package com.boardgamegeek.ui;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TableLayout;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.widget.PlayStatView;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.PreferencesUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

public class PlayStatsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	public static final String STATUS_PLAYED = "played";
	@InjectView(R.id.progress) View mProgressView;
	@InjectView(R.id.empty) View mEmptyView;
	@InjectView(R.id.data) View mDataView;
	@InjectView(R.id.table) TableLayout mTable;
	@InjectView(R.id.sync_message) View mSyncMessage;

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
		setMessageVisibility();
	}

	@Override
	public void onResume() {
		super.onResume();
		setMessageVisibility();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		CursorLoader loader = null;
		switch (id) {
			case PlayCountQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Collection.buildUniqueGameUri(), PlayCountQuery.PROJECTION,
					Games.NUM_PLAYS + ">0", null, Games.NUM_PLAYS + " DESC");
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
			case PlayCountQuery._TOKEN:
				// Calculate data
				int numberOfPlays = 0;
				int numberOfGames = 0;
				int quarters = 0;
				int dimes = 0;
				int nickels = 0;
				int currentCount = Integer.MAX_VALUE;
				int currentCounter = 0;
				int hIndex = 0;
				int hIndexCounter = 1;
				do {
					int playCount = cursor.getInt(PlayCountQuery.NUM_PLAYS);
					numberOfPlays += playCount;
					numberOfGames++;

					if (playCount != currentCount) {
						Timber.i(currentCount + " Plays: " + currentCounter);
						currentCount = playCount;
						currentCounter = 1;
					} else {
						currentCounter++;
					}

					if (playCount >= 25) {
						quarters++;
					} else if (playCount >= 10) {
						dimes++;
					} else if (playCount > 5) {
						nickels++;
					}

					if (hIndex == 0 && hIndexCounter > playCount) {
						hIndex = hIndexCounter - 1;
					}
					hIndexCounter++;

				} while (cursor.moveToNext());
				Timber.i(currentCount + " Plays: " + currentCounter);

				// Populate UI
				mTable.removeAllViews();
				addStatRow(R.string.play_stat_play_count, numberOfPlays);
				addStatRow(R.string.play_stat_distinct_games, numberOfGames);
				addStatRow(R.string.play_stat_quarters, quarters);
				addStatRow(R.string.play_stat_dimes, dimes);
				addStatRow(R.string.play_stat_nickels, nickels);
				addStatRow(R.string.play_stat_h_index, hIndex, R.string.play_stat_h_index_info);

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

	@OnClick(R.id.sync_message)
	public void onMessageClick(View v) {
		DialogUtils.createConfirmationDialog(getActivity(), R.string.play_stat_status_played_not_synced_message,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					PreferencesUtils.addSyncStatus(getActivity(), STATUS_PLAYED);
					setMessageVisibility();
					SyncService.clearCollection(getActivity());
					SyncService.sync(getActivity(), SyncService.FLAG_SYNC_COLLECTION);
				}
			}).show();
	}

	private void setMessageVisibility() {
		if (getActivity() != null && mSyncMessage != null) {
			final boolean arePlaysSynced = PreferencesUtils.isSyncStatus(getActivity(), STATUS_PLAYED);
			mSyncMessage.setVisibility(arePlaysSynced ? View.GONE : View.VISIBLE);
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

	private void addStatRow(int labelId, int value) {
		PlayStatView view = new PlayStatView(getActivity());
		view.setLabel(labelId);
		view.setValue(String.valueOf(value));
		mTable.addView(view);
	}

	private void addStatRow(int labelId, int value, int infoId) {
		PlayStatView view = new PlayStatView(getActivity());
		view.setLabel(labelId);
		view.setValue(String.valueOf(value));
		view.setInfoText(infoId);
		mTable.addView(view);
	}

	private interface PlayCountQuery {
		int _TOKEN = 0x01;
		String[] PROJECTION = { Games._ID, Games.NUM_PLAYS };
		int NUM_PLAYS = 1;
	}
}
