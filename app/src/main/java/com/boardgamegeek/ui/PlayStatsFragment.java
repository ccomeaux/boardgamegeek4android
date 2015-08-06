package com.boardgamegeek.ui;

import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
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
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.widget.PlayStatView.Builder;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.PreferencesUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

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
				Uri uri = Plays.CONTENT_SIMPLE_URI.buildUpon()
					.appendQueryParameter(BggContract.QUERY_KEY_GROUP_BY, BggContract.PlayItems.OBJECT_ID)
					.build();
				loader = new CursorLoader(getActivity(), uri,
					PlayCountQuery.PROJECTION,
					Plays.SYNC_STATUS + "=?",
					new String[] { String.valueOf(Play.SYNC_STATUS_SYNCED) },
					Plays.SUM_QUANTITY + " DESC");
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
				int hIndex = 0;
				int hIndexCounter = 1;
				do {
					int playCount = cursor.getInt(PlayCountQuery.SUM_QUANTITY);
					numberOfPlays += playCount;
					numberOfGames++;

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

				// Populate UI
				mTable.removeAllViews();
				addStatRow(new Builder().labelId(R.string.play_stat_play_count).value(numberOfPlays));
				addStatRow(new Builder().labelId(R.string.play_stat_distinct_games).value(numberOfGames));
				addStatRow(new Builder().labelId(R.string.play_stat_quarters).value(quarters));
				addStatRow(new Builder().labelId(R.string.play_stat_dimes).value(dimes));
				addStatRow(new Builder().labelId(R.string.play_stat_nickels).value(nickels));
				addStatRow(new Builder().labelId(R.string.play_stat_h_index).value(hIndex).infoId(R.string.play_stat_h_index_info));

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

	private void addStatRow(Builder builder) {
		mTable.addView(builder.build(getActivity()));
	}

	private interface PlayCountQuery {
		int _TOKEN = 0x01;
		String[] PROJECTION = { Plays._ID, Plays.SUM_QUANTITY, PlayItems.OBJECT_ID };
		int SUM_QUANTITY = 1;
	}
}
