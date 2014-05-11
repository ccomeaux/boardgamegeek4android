package com.boardgamegeek.ui;

import java.util.HashSet;
import java.util.Set;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.UIUtils;

public class PlayStatsFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private int mGameId;

	private int mPlayingTime;
	private double mRating;

	private TextView mPlayCountView;
	private TextView mPlayHoursView;
	private TextView mPlayMonthsView;
	private TextView mFhmView;
	private TextView mHhmView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Uri uri = UIUtils.fragmentArgumentsToIntent(getArguments()).getData();
		mGameId = Games.getGameId(uri);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_play_stats, null);
		mPlayCountView = (TextView) rootView.findViewById(R.id.play_count);
		mPlayHoursView = (TextView) rootView.findViewById(R.id.play_hours);
		mPlayMonthsView = (TextView) rootView.findViewById(R.id.play_months);
		mFhmView = (TextView) rootView.findViewById(R.id.fhm);
		mHhmView = (TextView) rootView.findViewById(R.id.hhm);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().restartLoader(GameQuery._TOKEN, getArguments(), this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		switch (id) {
			case PlayQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Plays.CONTENT_URI, PlayQuery.PROJECTION, PlayItems.OBJECT_ID
					+ "=?", new String[] { String.valueOf(mGameId) }, null);
				break;
			case GameQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Collection.CONTENT_URI, GameQuery.PROJECTION, "collection."
					+ Collection.GAME_ID + "=?", new String[] { String.valueOf(mGameId) }, null);
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
			return;
		}

		int token = loader.getId();
		switch (token) {
			case PlayQuery._TOKEN:
				Stats stats = new Stats(cursor);

				mPlayCountView.setText("Plays: " + stats.getPlays());
				mPlayHoursView.setText("Hours Played: " + stats.getHoursPlayed());
				mPlayMonthsView.setText("Months played: " + stats.getMonthsPlayed());

				mFhmView.setText("Friendless Happiness Metric (FHM): " + stats.calculateFhm());
				mHhmView.setText("Huber Happiness Metric (HHM): " + stats.calculateHhm());

				break;
			case GameQuery._TOKEN:
				mPlayingTime = cursor.getInt(GameQuery.PLAYING_TIME);
				mRating = cursor.getDouble(GameQuery.RATING);
				getLoaderManager().restartLoader(PlayQuery._TOKEN, getArguments(), this);
				break;
			default:
				cursor.close();
				break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	private class Stats {
		private int mPlays;
		private int mMinutesPlayed;
		private Set<String> mMonths = new HashSet<String>();

		Stats(Cursor cursor) {
			do {
				int quantity = cursor.getInt(PlayQuery.QUANTITY);
				int length = cursor.getInt(PlayQuery.LENGTH);
				String date = cursor.getString(PlayQuery.DATE);

				mPlays += quantity;
				if (length == 0) {
					mMinutesPlayed += mPlayingTime * quantity;
				} else {
					mMinutesPlayed += length;
				}

				if (!TextUtils.isEmpty(date)) {
					mMonths.add(date.substring(0, 7));
				}

			} while (cursor.moveToNext());
		}

		public int getPlays() {
			return mPlays;
		}

		public int getHoursPlayed() {
			return mMinutesPlayed / 60;
		}

		public int getMonthsPlayed() {
			return mMonths.size();
		}

		public int calculateFhm() {
			return (int) ((mRating * 5) + mPlays + (4 * getMonthsPlayed()) + getHoursPlayed());
		}

		public int calculateHhm() {
			return (int) ((mRating - 4.5) * getHoursPlayed());
		}
	}

	private interface PlayQuery {
		int _TOKEN = 0x01;
		String[] PROJECTION = { Plays._ID, Plays.PLAY_ID, Plays.DATE, PlayItems.NAME, PlayItems.OBJECT_ID,
			Plays.LOCATION, Plays.QUANTITY, Plays.LENGTH, Plays.SYNC_STATUS, "COUNT(" + PlayPlayers.USER_ID + ")",
			Games.THUMBNAIL_URL };
		int DATE = 2;
		int QUANTITY = 6;
		int LENGTH = 7;
	}

	private interface GameQuery {
		int _TOKEN = 0x02;
		String[] PROJECTION = { Games._ID, Collection.RATING, Games.PLAYING_TIME };
		int RATING = 1;
		int PLAYING_TIME = 2;
	}
}