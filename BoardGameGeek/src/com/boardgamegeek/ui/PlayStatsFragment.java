package com.boardgamegeek.ui;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.UIUtils;

public class PlayStatsFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private int mGameId;

	private int mPlayingTime;
	private double mRating;

	private View mProgress;
	private View mEmpty;
	private View mData;
	private TextView mPlayCountView;
	private TextView mPlayHoursView;
	private TextView mPlayMonthsView;
	private TextView mFhmView;
	private TextView mHhmView;
	private TextView mRuhmView;
	private TextView mUtilization;
	private View mNickelRoot;
	private TextView mNickel;
	private View mDimeRoot;
	private TextView mDime;
	private View mQuarterRoot;
	private TextView mQuarter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Uri uri = UIUtils.fragmentArgumentsToIntent(getArguments()).getData();
		mGameId = Games.getGameId(uri);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_play_stats, container, false);
		mProgress = rootView.findViewById(R.id.progress);
		mEmpty = rootView.findViewById(R.id.empty);
		mData = rootView.findViewById(R.id.data);

		mFhmView = (TextView) rootView.findViewById(R.id.fhm);
		mHhmView = (TextView) rootView.findViewById(R.id.hhm);
		mRuhmView = (TextView) rootView.findViewById(R.id.ruhm);
		mUtilization = (TextView) rootView.findViewById(R.id.utilization);

		mPlayCountView = (TextView) rootView.findViewById(R.id.play_count);
		mNickelRoot = rootView.findViewById(R.id.nickel_container);
		mNickel = (TextView) rootView.findViewById(R.id.nickel);
		mDimeRoot = rootView.findViewById(R.id.dime_container);
		mDime = (TextView) rootView.findViewById(R.id.dime);
		mQuarterRoot = rootView.findViewById(R.id.quarter_container);
		mQuarter = (TextView) rootView.findViewById(R.id.quarter);
		mPlayHoursView = (TextView) rootView.findViewById(R.id.play_hours);
		mPlayMonthsView = (TextView) rootView.findViewById(R.id.play_months);
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
					+ "=? AND " + Plays.SYNC_STATUS + "=?", new String[] { String.valueOf(mGameId),
					String.valueOf(Play.SYNC_STATUS_SYNCED) }, Plays.DATE + " ASC");
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
			mProgress.setVisibility(View.GONE);
			mEmpty.setVisibility(View.VISIBLE);
			mData.setVisibility(View.GONE);
			return;
		}

		int token = loader.getId();
		switch (token) {
			case PlayQuery._TOKEN:
				DecimalFormat doubleFormat = new DecimalFormat("0.00");
				DecimalFormat percentageFormat = new DecimalFormat("0.0");

				Stats stats = new Stats(cursor);

				mFhmView.setText(String.valueOf(stats.calculateFhm()));
				mHhmView.setText(String.valueOf(stats.calculateHhm()));
				mRuhmView.setText(doubleFormat.format(stats.calculateRuhm()));
				mUtilization.setText(percentageFormat.format(stats.calculateUtilization() * 100) + "%");

				String pcd = null;// stats.getPlayCountDescription();
				mPlayCountView.setText(String.valueOf(stats.getPlayCount())
					+ (!TextUtils.isEmpty(pcd) ? " - " + pcd : ""));
				mPlayHoursView.setText(String.valueOf(stats.getHoursPlayed()));
				mPlayMonthsView.setText(String.valueOf(stats.getMonthsPlayed()));
				if (stats.mNickelDate == null) {
					mNickelRoot.setVisibility(View.GONE);
				} else {
					mNickelRoot.setVisibility(View.VISIBLE);
					mNickel.setText(stats.mNickelDate);
				}
				if (stats.mDimeDate == null) {
					mDimeRoot.setVisibility(View.GONE);
				} else {
					mDimeRoot.setVisibility(View.VISIBLE);
					mDime.setText(stats.mDimeDate);
				}
				if (stats.mQuarterDate == null) {
					mQuarterRoot.setVisibility(View.GONE);
				} else {
					mQuarterRoot.setVisibility(View.VISIBLE);
					mQuarter.setText(stats.mQuarterDate);
				}

				mProgress.setVisibility(View.GONE);
				mEmpty.setVisibility(View.GONE);
				mData.setVisibility(View.VISIBLE);
				break;
			case GameQuery._TOKEN:
				mPlayingTime = cursor.getInt(GameQuery.PLAYING_TIME);
				double ratingSum = 0;
				int ratingCount = 0;
				do {
					double rating = cursor.getDouble(GameQuery.RATING);
					if (rating > 0) {
						ratingSum += rating;
						ratingCount++;
					}
				} while (cursor.moveToNext());
				mRating = ratingSum / ratingCount;
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
		final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		private double mLambda;
		private String mCurrentYear;

		private String mFirstPlayDate;
		private String mLastPlayDate;
		private String mNickelDate;
		private String mDimeDate;
		private String mQuarterDate;
		private int mPlayCount;
		private int mPlayCountThisYear;
		private int mMinutesPlayed;
		private Set<String> mMonths = new HashSet<String>();

		public Stats(Cursor cursor) {
			mLambda = Math.log(0.1) / -10;
			mCurrentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

			mFirstPlayDate = null;
			mLastPlayDate = null;
			mNickelDate = null;
			mDimeDate = null;
			mQuarterDate = null;
			mPlayCount = 0;
			mPlayCountThisYear = 0;
			mMinutesPlayed = 0;
			mMonths.clear();

			do {
				int quantity = cursor.getInt(PlayQuery.QUANTITY);
				int length = cursor.getInt(PlayQuery.LENGTH);
				String date = cursor.getString(PlayQuery.DATE);

				if (mFirstPlayDate == null) {
					mFirstPlayDate = date;
				}
				mLastPlayDate = date;

				if (mPlayCount < 5 && (mPlayCount + quantity) >= 5) {
					mNickelDate = date;
				}
				if (mPlayCount < 10 && (mPlayCount + quantity) >= 10) {
					mDimeDate = date;
				}
				if (mPlayCount < 25 && (mPlayCount + quantity) >= 25) {
					mQuarterDate = date;
				}
				mPlayCount += quantity;
				if (date.substring(0, 4).equals(mCurrentYear)) {
					mPlayCountThisYear += quantity;
				}

				if (length == 0) {
					mMinutesPlayed += mPlayingTime * quantity;
				} else {
					mMinutesPlayed += length;
				}

				mMonths.add(date.substring(0, 7));

			} while (cursor.moveToNext());
		}

		public int getPlayCount() {
			return mPlayCount;
		}

		public String getPlayCountDescription() {
			int playCount = getPlayCount();
			if (playCount > 25) {
				return "Quarter";
			} else if (playCount > 10) {
				return "Dime";
			} else if (playCount > 5) {
				return "Nickel";
			}
			return "";
		}

		public int getHoursPlayed() {
			return mMinutesPlayed / 60;
		}

		public int getMonthsPlayed() {
			return mMonths.size();
		}

		public double calculateUtilization() {
			return 1 - Math.exp(-mLambda * mPlayCount);
		}

		public int calculateFhm() {
			return (int) ((mRating * 5) + mPlayCount + (4 * getMonthsPlayed()) + getHoursPlayed());
		}

		public int calculateHhm() {
			return (int) ((mRating - 4.5) * getHoursPlayed());
		}

		public double calculateRuhm() {
			double raw = (((double) calculateFlash()) / calculateLag()) * getMonthsPlayed() * mRating;
			if (raw == 0) {
				return 0;
			}
			return Math.log(raw);
		}

		// public int getMonthsPerPlay() {
		// long days = calculateSpan();
		// int months = (int) (days / 365.25 * 12);
		// return months / mPlayCount;
		// }

		public double calculateGrayHotness(int intervalPlayCount) {
			// http://matthew.gray.org/2005/10/games_16.html
			double S = 1 + (intervalPlayCount / mPlayCount);
			// TODO: need to get HHM for the interval _only_
			return S * S * Math.sqrt(intervalPlayCount) * calculateHhm();
		}

		public int calculateWhitemoreScore() {
			// http://www.boardgamegeek.com/geeklist/37832/my-favorite-designers
			int score = (int) (mRating * 2 - 13);
			if (score < 0) {
				return 0;
			}
			return score;
		}

		public double calculateZefquaaviusScore() {
			// http://boardgamegeek.com/user/zefquaavius
			double neutralRating = 5.5;
			double abs = (mRating - neutralRating);
			double squared = abs * abs;
			if (mRating < neutralRating) {
				squared *= -1;
			}
			return squared / 2.025;
		}

		public double calculateZefquaaviusHotness(int intervalPlayCount) {
			return calculateGrayHotness(intervalPlayCount) * calculateZefquaaviusScore();
		}

		private long calculateFlash() {
			return daysBetweenDates(mFirstPlayDate, mLastPlayDate);
		}

		private long calculateLag() {
			return daysBetweenDates(mLastPlayDate, null);
		}

		private long calculateSpan() {
			return daysBetweenDates(mFirstPlayDate, null);
		}

		private long daysBetweenDates(String first, String second) {
			try {
				long f = System.currentTimeMillis();
				long s = System.currentTimeMillis();
				if (!TextUtils.isEmpty(first)) {
					f = FORMAT.parse(first).getTime();
				}
				if (!TextUtils.isEmpty(second)) {
					s = FORMAT.parse(second).getTime();
				}
				long days = TimeUnit.DAYS.convert(s - f, TimeUnit.MILLISECONDS);
				if (days < 1) {
					return days;
				}
				return days;
			} catch (ParseException e) {
				return 1;
			}
		}
	}

	private interface PlayQuery {
		int _TOKEN = 0x01;
		String[] PROJECTION = { Plays._ID, Plays.PLAY_ID, Plays.DATE, PlayItems.NAME, PlayItems.OBJECT_ID,
			Plays.LOCATION, Plays.QUANTITY, Plays.LENGTH, Plays.SYNC_STATUS, Plays.PLAYER_COUNT, Games.THUMBNAIL_URL };
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