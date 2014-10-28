package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
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
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;

public class PlayStatsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = makeLogTag(PlayStatsFragment.class);

	@InjectView(R.id.progress) View mProgressView;
	@InjectView(R.id.empty) View mEmptyView;
	@InjectView(R.id.data) View mDataView;
	@InjectView(R.id.total_plays) TextView mTotalPlays;
	@InjectView(R.id.distinct_games) TextView mDistinctGames;
	@InjectView(R.id.quarters) TextView mQuarters;
	@InjectView(R.id.dimes) TextView mDimes;
	@InjectView(R.id.nickels) TextView mNickels;
	@InjectView(R.id.hindex) TextView mHIndex;

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
				loader = new CursorLoader(getActivity(), Collection.buildUniqueGameUri(), PlayCountQuery.PROJECTION,
					Games.NUM_PLAYS + ">0", null, Games.NUM_PLAYS + " DESC");
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
						LOGI(TAG, currentCount + " Plays: " + currentCounter);
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
				LOGI(TAG, currentCount + " Plays: " + currentCounter);

				// Populate UI
				mTotalPlays.setText(String.valueOf(numberOfPlays));
				mDistinctGames.setText(String.valueOf(numberOfGames));
				mQuarters.setText(String.valueOf(quarters));
				mDimes.setText(String.valueOf(dimes));
				mNickels.setText(String.valueOf(nickels));
				mHIndex.setText(String.valueOf(hIndex));

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

	private interface PlayCountQuery {
		int _TOKEN = 0x01;
		String[] PROJECTION = { Games._ID, Games.NUM_PLAYS };
		int NUM_PLAYS = 1;
	}
}
