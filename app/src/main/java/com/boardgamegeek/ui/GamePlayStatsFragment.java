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
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TableLayout;
import butterknife.ButterKnife;
import butterknife.InjectView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.widget.PlayStatView;
import com.boardgamegeek.util.UIUtils;

public class GamePlayStatsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.00");;
	private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("0.0");
	private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private int mGameId;

	private int mPlayingTime;
	private double mRating;

	@InjectView(R.id.progress) View mProgressView;
	@InjectView(R.id.empty) View mEmptyView;
	@InjectView(R.id.data) View mDataView;
	@InjectView(R.id.table) TableLayout mTable;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Uri uri = UIUtils.fragmentArgumentsToIntent(getArguments()).getData();
		mGameId = Games.getGameId(uri);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_play_stats, container, false);
		ButterKnife.inject(this, rootView);
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
			showEmpty();
			return;
		}

		int token = loader.getId();
		switch (token) {
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
				if (ratingCount == 0) {
					mRating = 0.0;
				} else {
					mRating = ratingSum / ratingCount;
				}
				getLoaderManager().restartLoader(PlayQuery._TOKEN, getArguments(), this);
				break;
			case PlayQuery._TOKEN:
				mTable.removeAllViews();

				Stats stats = new Stats(cursor);

				if (!TextUtils.isEmpty(stats.getQuarterDate())) {
					addStatRow("", getString(R.string.play_stat_quarter));
				} else if (!TextUtils.isEmpty(stats.getDimeDate())) {
					addStatRow("", getString(R.string.play_stat_dime));
				} else if (!TextUtils.isEmpty(stats.getNickelDate())) {
					addStatRow("", getString(R.string.play_stat_nickel));
				}
				addStatRow(R.string.play_stat_play_count, stats.getPlayCount());
				addStatRow(R.string.play_stat_hours_played, (int) stats.getHoursPlayed());
				addStatRow(R.string.play_stat_months_played, stats.getMonthsPlayed());
				addDateRow(stats.getNickelDate(), R.string.play_stat_nickel);
				addDateRow(stats.getDimeDate(), R.string.play_stat_dime);
				addDateRow(stats.getQuarterDate(), R.string.play_stat_quarter);

				addDivider();

				addStatRow(R.string.play_stat_fhm, stats.calculateFhm(), R.string.play_stat_fhm_info);
				addStatRow(R.string.play_stat_hhm, stats.calculateHhm(), R.string.play_stat_hhm_info);
				addStatRow(R.string.play_stat_ruhm, stats.calculateRuhm(), R.string.play_stat_ruhm_info);
				addStatRowPercentage(R.string.play_stat_utilization, stats.calculateUtilization(),
					R.string.play_stat_utilization_info);

				showData();
				break;
			default:
				cursor.close();
				break;
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

	private void addDateRow(String date, int resId) {
		if (!TextUtils.isEmpty(date)) {
			try {
				long l = FORMAT.parse(date).getTime();
				String d = DateUtils.formatDateTime(getActivity(), l, DateUtils.FORMAT_SHOW_DATE
					| DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_MONTH);
				addStatRow(getString(resId), d);
			} catch (ParseException e) {
				// just don't add the row
			}
		}
	}

	private void addStatRow(int labelId, int value) {
		addStatRow(labelId, String.valueOf(value));
	}

	private void addStatRow(int labelId, int value, int infoId) {
		addStatRow(labelId, String.valueOf(value), infoId);
	}

	private void addStatRow(int labelId, double value, int infoId) {
		addStatRow(labelId, DOUBLE_FORMAT.format(value), infoId);
	}

	private void addStatRowPercentage(int labelId, double value, int infoId) {
		addStatRow(labelId, PERCENTAGE_FORMAT.format(value * 100) + "%", infoId);
	}

	private void addStatRow(int labelId, String value) {
		PlayStatView view = new PlayStatView(getActivity());
		view.setLabel(labelId);
		view.setValue(value);
		mTable.addView(view);
	}

	private void addStatRow(int labelId, String value, int infoId) {
		PlayStatView view = new PlayStatView(getActivity());
		view.setLabel(labelId);
		view.setValue(value);
		view.setInfoText(infoId);
		mTable.addView(view);
	}

	private void addStatRow(String label, String value) {
		PlayStatView view = new PlayStatView(getActivity());
		view.setLabel(label);
		view.setValue(value);
		mTable.addView(view);
	}

	private void addDivider() {
		View view = new View(getActivity());
		view.setLayoutParams(new TableLayout.LayoutParams(0, 1));
		view.setBackgroundResource(R.color.primary_dark);
		mTable.addView(view);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	private class Stats {
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

		private String getNickelDate() {
			return mNickelDate;
		}

		private String getDimeDate() {
			return mDimeDate;
		}

		private String getQuarterDate() {
			return mQuarterDate;
		}

		public double getHoursPlayed() {
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
			return (int) ((mRating - 5) * getHoursPlayed());
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
					return 1;
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