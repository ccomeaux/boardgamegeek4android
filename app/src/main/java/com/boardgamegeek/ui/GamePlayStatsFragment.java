package com.boardgamegeek.ui;

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

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.widget.PlayStatView;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.UIUtils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class GamePlayStatsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.00");
	private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("0.0");
	private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private int mGameId;

	private int mPlayingTime;
	private double mRating;
	private Stats mStats;

	@InjectView(R.id.progress) View mProgressView;
	@InjectView(R.id.empty) View mEmptyView;
	@InjectView(R.id.data) View mDataView;
	@InjectView(R.id.table_play_count) TableLayout mPlayCountTable;
	@InjectView(R.id.table_dates) TableLayout mDatesTable;
	@InjectView(R.id.table_play_time) TableLayout mPlayTimeTable;
	@InjectView(R.id.table_advanced) TableLayout mAdvancedTable;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Uri uri = UIUtils.fragmentArgumentsToIntent(getArguments()).getData();
		mGameId = Games.getGameId(uri);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game_play_stats, container, false);
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
			case GameQuery._TOKEN:
				loader = new CursorLoader(getActivity(),
					Collection.CONTENT_URI,
					GameQuery.PROJECTION,
					"collection." + Collection.GAME_ID + "=?",
					new String[] { String.valueOf(mGameId) },
					null);
				break;
			case PlayQuery._TOKEN:
				loader = new CursorLoader(getActivity(),
					Plays.CONTENT_URI,
					PlayQuery.PROJECTION,
					PlayItems.OBJECT_ID + "=? AND " + Plays.SYNC_STATUS + "=?",
					new String[] { String.valueOf(mGameId), String.valueOf(Play.SYNC_STATUS_SYNCED) },
					Plays.DATE + " ASC");
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
				mStats = new Stats(cursor);
				bindUi(mStats);
				showData();
				break;
			default:
				cursor.close();
				break;
		}
	}

	private void bindUi(Stats stats) {
		mPlayCountTable.removeAllViews();
		mDatesTable.removeAllViews();
		mPlayTimeTable.removeAllViews();
		mAdvancedTable.removeAllViews();

		if (!TextUtils.isEmpty(stats.getQuarterDate())) {
			addStatRow(mPlayCountTable, "", getString(R.string.play_stat_quarter));
		} else if (!TextUtils.isEmpty(stats.getDimeDate())) {
			addStatRow(mPlayCountTable, "", getString(R.string.play_stat_dime));
		} else if (!TextUtils.isEmpty(stats.getNickelDate())) {
			addStatRow(mPlayCountTable, "", getString(R.string.play_stat_nickel));
		}
		addStatRow(mPlayCountTable, R.string.play_stat_play_count, stats.getPlayCount());
		addStatRowMaybe(mPlayCountTable, R.string.play_stat_play_count_incomplete, stats.getPlayCountIncomplete());
		addDivider(mPlayCountTable);
		for (int i = 1; i <= stats.getMaxPlayerCount(); i++) {
			addStatRowMaybe(mPlayCountTable, getResources().getQuantityString(R.plurals.player_description, i, i), stats.getPlayCount(i));
		}
		addDivider(mPlayCountTable);
		addStatRow(mPlayCountTable, R.string.play_stat_months_played, stats.getMonthsPlayed());
		addStatRowMaybe(mPlayCountTable, R.string.play_stat_play_rate, stats.getPlayRate());

		addDateRow(mDatesTable, stats.getFirstPlayDate(), R.string.play_stat_first_play);
		addDateRow(mDatesTable, stats.getNickelDate(), R.string.play_stat_nickel);
		addDateRow(mDatesTable, stats.getDimeDate(), R.string.play_stat_dime);
		addDateRow(mDatesTable, stats.getQuarterDate(), R.string.play_stat_quarter);
		addDateRow(mDatesTable, stats.getLastPlayDate(), R.string.play_stat_last_play);

		addStatRow(mPlayTimeTable, R.string.play_stat_hours_played, (int) stats.getHoursPlayed());
		int average = stats.getAveragePlayTime();
		if (average > 0) {
			addStatRow(mPlayTimeTable, R.string.play_stat_average_play_time, DateTimeUtils.formatMinutes(average));
			if (mPlayingTime > 0) {
				if (average > mPlayingTime) {
					addStatRow(mPlayTimeTable, R.string.play_stat_average_play_time_slower, DateTimeUtils.formatMinutes(average - mPlayingTime));
				} else if (mPlayingTime > average) {
					addStatRow(mPlayTimeTable, R.string.play_stat_average_play_time_faster, DateTimeUtils.formatMinutes(mPlayingTime - average));
				}
				// don't display anything if the average is exactly as expected
			}
		}
		int averagePerPlayer = stats.getAveragePlayTimePerPlayer();
		if (averagePerPlayer > 0) {
			addStatRow(mPlayTimeTable, R.string.play_stat_average_play_time_per_player, DateTimeUtils.formatMinutes(averagePerPlayer));
		}

		addStatRow(mAdvancedTable, R.string.play_stat_fhm, stats.calculateFhm(), R.string.play_stat_fhm_info);
		addStatRow(mAdvancedTable, R.string.play_stat_hhm, stats.calculateHhm(), R.string.play_stat_hhm_info);
		addStatRow(mAdvancedTable, R.string.play_stat_ruhm, stats.calculateRuhm(), R.string.play_stat_ruhm_info);
		addStatRowPercentage(mAdvancedTable, R.string.play_stat_utilization, stats.calculateUtilization(), R.string.play_stat_utilization_info);
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

	private void addDateRow(ViewGroup container, String date, int resId) {
		if (!TextUtils.isEmpty(date)) {
			try {
				long l = FORMAT.parse(date).getTime();
				String d = DateUtils.formatDateTime(getActivity(), l,
					DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_MONTH);
				addStatRow(container, getString(resId), d);
			} catch (ParseException e) {
				// just don't add the row
			}
		}
	}

	private void addStatRowMaybe(ViewGroup container, int labelId, int value) {
		if (value > 0) {
			addStatRow(container, labelId, value);
		}
	}

	private void addStatRowMaybe(ViewGroup container, String label, int value) {
		if (value > 0) {
			addStatRow(container, label, String.valueOf(value));
		}
	}

	private void addStatRowMaybe(ViewGroup container, int labelId, int value, int infoId) {
		if (value > 0) {
			addStatRow(container, labelId, value, infoId);
		}
	}

	private void addStatRowMaybe(ViewGroup container, int labelId, double value) {
		if (value > 0.0) {
			addStatRow(container, labelId, value);
		}
	}

	private void addStatRow(ViewGroup container, int labelId, int value) {
		addStatRow(container, labelId, String.valueOf(value));
	}

	private void addStatRow(ViewGroup container, int labelId, int value, int infoId) {
		addStatRow(container, labelId, String.valueOf(value), infoId);
	}

	private void addStatRow(ViewGroup container, int labelId, double value) {
		addStatRow(container, labelId, DOUBLE_FORMAT.format(value));
	}

	private void addStatRow(ViewGroup container, int labelId, double value, int infoId) {
		addStatRow(container, labelId, DOUBLE_FORMAT.format(value), infoId);
	}

	private void addStatRowPercentage(ViewGroup container, int labelId, double value, int infoId) {
		addStatRow(container, labelId, PERCENTAGE_FORMAT.format(value * 100) + "%", infoId);
	}

	private void addStatRow(ViewGroup container, int labelId, String value) {
		PlayStatView view = new PlayStatView(getActivity());
		view.setLabel(labelId);
		view.setValue(value);
		container.addView(view);
	}

	private void addStatRow(ViewGroup container, int labelId, String value, int infoId) {
		PlayStatView view = new PlayStatView(getActivity());
		view.setLabel(labelId);
		view.setValue(value);
		view.setInfoText(infoId);
		container.addView(view);
	}

	private void addStatRow(ViewGroup container, String label, String value) {
		PlayStatView view = new PlayStatView(getActivity());
		view.setLabel(label);
		view.setValue(value);
		container.addView(view);
	}

	private void addDivider(ViewGroup container) {
		View view = new View(getActivity());
		view.setLayoutParams(new TableLayout.LayoutParams(0, 1));
		view.setBackgroundResource(R.color.primary_dark);
		container.addView(view);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	private class Stats {
		private double mLambda;
		private String mCurrentYear;

		private final Map<Integer, PlayModel> mPlays = new HashMap<>();

		private String mFirstPlayDate;
		private String mLastPlayDate;
		private String mNickelDate;
		private String mDimeDate;
		private String mQuarterDate;
		private int mPlayCount;
		private int mPlayCountIncomplete;
		private int mPlayCountWithLength;
		private int mPlayCountThisYear;
		private int mPlayerCountSumWithLength;
		private Map<Integer, Integer> mPlayCountPerPlayerCount;
		private int mRealMinutesPlayed;
		private int mEstimatedMinutesPlayed;
		private Set<String> mMonths = new HashSet<>();

		public Stats(Cursor cursor) {
			mLambda = Math.log(0.1) / -10;
			mCurrentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

			mPlays.clear();

			mFirstPlayDate = null;
			mLastPlayDate = null;
			mNickelDate = null;
			mDimeDate = null;
			mQuarterDate = null;
			mPlayCount = 0;
			mPlayCount = 0;
			mPlayCountWithLength = 0;
			mPlayCountThisYear = 0;
			mPlayerCountSumWithLength = 0;
			mPlayCountPerPlayerCount = new HashMap<>();
			mRealMinutesPlayed = 0;
			mEstimatedMinutesPlayed = 0;
			mMonths.clear();

			do {
				PlayModel pm = new PlayModel(cursor);
				mPlays.put(pm.playId, pm);
			} while (cursor.moveToNext());

			for (PlayModel pm : mPlays.values()) {
				if (pm.incomplete) {
					mPlayCountIncomplete += pm.quantity;
					continue;
				}

				if (mFirstPlayDate == null) {
					mFirstPlayDate = pm.date;
				}
				mLastPlayDate = pm.date;

				if (mPlayCount < 5 && (mPlayCount + pm.quantity) >= 5) {
					mNickelDate = pm.date;
				}
				if (mPlayCount < 10 && (mPlayCount + pm.quantity) >= 10) {
					mDimeDate = pm.date;
				}
				if (mPlayCount < 25 && (mPlayCount + pm.quantity) >= 25) {
					mQuarterDate = pm.date;
				}
				mPlayCount += pm.quantity;
				if (pm.date.substring(0, 4).equals(mCurrentYear)) {
					mPlayCountThisYear += pm.quantity;
				}

				if (pm.length == 0) {
					mEstimatedMinutesPlayed += mPlayingTime * pm.quantity;
				} else {
					mRealMinutesPlayed += pm.length;
					mPlayCountWithLength += pm.quantity;
					mPlayerCountSumWithLength += pm.playerCount * pm.quantity;
				}

				if (pm.playerCount > 0) {
					int previousQuantity = 0;
					if (mPlayCountPerPlayerCount.containsKey(pm.playerCount)) {
						previousQuantity = mPlayCountPerPlayerCount.get(pm.playerCount);
					}
					mPlayCountPerPlayerCount.put(pm.playerCount, previousQuantity + pm.quantity);
				}

				mMonths.add(pm.date.substring(0, 7));
			}
		}

		public int getPlayCount() {
			return mPlayCount;
		}

		public int getPlayCountIncomplete() {
			return mPlayCountIncomplete;
		}

		public String getFirstPlayDate() {
			return mFirstPlayDate;
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

		public String getLastPlayDate() {
			if (mPlayCount > 0) {
				return mLastPlayDate;
			}
			return null;
		}

		public double getHoursPlayed() {
			return (mRealMinutesPlayed + mEstimatedMinutesPlayed) / 60;
		}

		/* plays per month, only counting the active period) */
		public double getPlayRate() {
			long flash = calculateFlash();
			if (flash > 0) {
				double rate = ((double) (mPlayCount * 365) / flash) / 12;
				return Math.min(rate, mPlayCount);
			}
			return 0;
		}

		public int getAveragePlayTime() {
			if (mPlayCountWithLength > 0) {
				return mRealMinutesPlayed / mPlayCountWithLength;
			}
			return 0;
		}

		public int getAveragePlayTimePerPlayer() {
			if (mPlayerCountSumWithLength > 0) {
				return mRealMinutesPlayed / mPlayerCountSumWithLength;
			}
			return 0;
		}

		public int getMonthsPlayed() {
			return mMonths.size();
		}

		public int getMaxPlayerCount() {
			int max = 0;
			for (Integer playerCount : mPlayCountPerPlayerCount.keySet()) {
				if (playerCount > max) {
					max = playerCount;
				}
			}
			return max;
		}

		public int getPlayCount(int playerCount) {
			if (mPlayCountPerPlayerCount.containsKey(playerCount)) {
				return mPlayCountPerPlayerCount.get(playerCount);
			}
			return 0;
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

	private class PlayModel {
		int playId;
		String date;
		int length;
		int quantity;
		boolean incomplete;
		int playerCount;

		PlayModel(Cursor cursor) {
			playId = cursor.getInt(PlayQuery.PLAY_ID);
			date = cursor.getString(PlayQuery.DATE);
			length = cursor.getInt(PlayQuery.LENGTH);
			quantity = cursor.getInt(PlayQuery.QUANTITY);
			incomplete = CursorUtils.getBoolean(cursor, PlayQuery.INCOMPLETE);
			playerCount = cursor.getInt(PlayQuery.PLAYER_COUNT);
		}
	}

	private interface PlayQuery {
		int _TOKEN = 0x01;
		String[] PROJECTION = { Plays._ID, Plays.PLAY_ID, Plays.DATE, PlayItems.NAME, PlayItems.OBJECT_ID,
			Plays.LOCATION, Plays.QUANTITY, Plays.LENGTH, Plays.SYNC_STATUS, Plays.PLAYER_COUNT, Games.THUMBNAIL_URL, Plays.INCOMPLETE };
		int PLAY_ID = 1;
		int DATE = 2;
		int QUANTITY = 6;
		int LENGTH = 7;
		int PLAYER_COUNT = 9;
		int INCOMPLETE = 11;
	}

	private interface GameQuery {
		int _TOKEN = 0x02;
		String[] PROJECTION = { Games._ID, Collection.RATING, Games.PLAYING_TIME };
		int RATING = 1;
		int PLAYING_TIME = 2;
	}
}