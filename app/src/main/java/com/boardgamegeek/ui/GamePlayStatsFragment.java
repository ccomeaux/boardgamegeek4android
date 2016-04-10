package com.boardgamegeek.ui;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TableLayout;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.widget.IntegerYAxisValueFormatter;
import com.boardgamegeek.ui.widget.PlayStatView;
import com.boardgamegeek.ui.widget.PlayStatView.Builder;
import com.boardgamegeek.ui.widget.PlayerStatView;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;
import com.github.mikephil.charting.animation.Easing.EasingOption;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import timber.log.Timber;

public class GamePlayStatsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final DecimalFormat SCORE_FORMAT = new DecimalFormat("0.##");
	private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private int mGameId;

	private int mPlayingTime;
	private double mRating;
	private Stats mStats;

	@Bind(R.id.progress) View mProgressView;
	@Bind(R.id.empty) View mEmptyView;
	@Bind(R.id.data) View mDataView;
	@Bind(R.id.table_play_count) TableLayout mPlayCountTable;
	@Bind(R.id.chart_play_count) HorizontalBarChart mPlayCountChart;
	@Bind(R.id.card_wins) View mWins;
	@Bind(R.id.table_wins) TableLayout mWinTable;
	@Bind(R.id.card_score) View mScores;
	@Bind(R.id.table_score) TableLayout mScoreTable;
	@Bind(R.id.card_opponents) View mOpponents;
	@Bind(R.id.table_opponents) TableLayout mOpponentsTable;
	@Bind(R.id.table_dates) TableLayout mDatesTable;
	@Bind(R.id.table_play_time) TableLayout mPlayTimeTable;
	@Bind(R.id.card_locations) View mLocations;
	@Bind(R.id.table_locations) TableLayout mLocationsTable;
	@Bind(R.id.table_advanced) TableLayout mAdvancedTable;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Uri uri = UIUtils.fragmentArgumentsToIntent(getArguments()).getData();
		mGameId = Games.getGameId(uri);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game_play_stats, container, false);
		ButterKnife.bind(this, rootView);

		mPlayCountChart.setDrawGridBackground(false);
		mPlayCountChart.getAxisRight().setValueFormatter(new IntegerYAxisValueFormatter());
		mPlayCountChart.getAxisLeft().setEnabled(false);
		mPlayCountChart.getXAxis().setDrawGridLines(false);
		mPlayCountChart.setDescription(null);

		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().restartLoader(GameQuery._TOKEN, null, this);
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
			case PlayerQuery._TOKEN:
				loader = new CursorLoader(getActivity(),
					Plays.buildPlayersUri(),
					PlayerQuery.PROJECTION,
					PlayItems.OBJECT_ID + "=? AND " + Plays.SYNC_STATUS + "=?",
					new String[] { String.valueOf(mGameId), String.valueOf(Play.SYNC_STATUS_SYNCED) },
					null);
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
				getLoaderManager().restartLoader(PlayQuery._TOKEN, null, this);
				break;
			case PlayQuery._TOKEN:
				mStats = new Stats(cursor);
				getLoaderManager().restartLoader(PlayerQuery._TOKEN, null, this);
				break;
			case PlayerQuery._TOKEN:
				mStats.addPlayerData(cursor);
				mStats.calculate();
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
		mWinTable.removeAllViews();
		mScoreTable.removeAllViews();
		mDatesTable.removeAllViews();
		mPlayTimeTable.removeAllViews();
		mAdvancedTable.removeAllViews();

		if (!TextUtils.isEmpty(stats.getDollarDate())) {
			addStatRow(mPlayCountTable, new Builder().value(getString(R.string.play_stat_dollar)));
		} else if (!TextUtils.isEmpty(stats.getHalfDollarDate())) {
			addStatRow(mPlayCountTable, new Builder().value(getString(R.string.play_stat_half_dollar)));
		} else if (!TextUtils.isEmpty(stats.getQuarterDate())) {
			addStatRow(mPlayCountTable, new Builder().value(getString(R.string.play_stat_quarter)));
		} else if (!TextUtils.isEmpty(stats.getDimeDate())) {
			addStatRow(mPlayCountTable, new Builder().value(getString(R.string.play_stat_dime)));
		} else if (!TextUtils.isEmpty(stats.getNickelDate())) {
			addStatRow(mPlayCountTable, new Builder().value(getString(R.string.play_stat_nickel)));
		}
		addStatRow(mPlayCountTable, new Builder().labelId(R.string.play_stat_play_count).value(stats.getPlayCount()));
		if (stats.getPlayCountIncomplete() > 0) {
			addStatRow(mPlayCountTable, new Builder().labelId(R.string.play_stat_play_count_incomplete).value(stats.getPlayCountIncomplete()));

		}
		addStatRow(mPlayCountTable, new Builder().labelId(R.string.play_stat_months_played).value(stats.getMonthsPlayed()));
		if (stats.getPlayRate() > 0) {
			addStatRow(mPlayCountTable, new Builder().labelId(R.string.play_stat_play_rate).value(stats.getPlayRate()));
		}

		if (stats.hasWins()) {
			addStatRow(mWinTable, new Builder().labelId(R.string.play_stat_win_percentage).valueAsPercentage(stats.getWinPercentage()));
			addStatRow(mWinTable, new Builder().labelId(R.string.play_stat_win_skill).value(stats.getWinSkill()).infoId(R.string.play_stat_win_skill_info));
			mWins.setVisibility(View.VISIBLE);
		} else {
			mWins.setVisibility(View.GONE);
		}

		ArrayList<String> playersLabels = new ArrayList<>();
		ArrayList<BarEntry> playCountValues = new ArrayList<>();
		ArrayList<BarEntry> winValues = new ArrayList<>();
		int index = 0;
		for (int i = stats.getMinPlayerCount(); i <= stats.getMaxPlayerCount(); i++) {
			playersLabels.add(String.valueOf(i));
			playCountValues.add(new BarEntry(stats.getPlayCount(i), index));
			winValues.add(new BarEntry(stats.getWins(i), index));
			index++;
		}
		ArrayList<IBarDataSet> dataSets = new ArrayList<>();

		BarDataSet playCountDataSet = new BarDataSet(playCountValues, getString(R.string.title_plays));
		playCountDataSet.setDrawValues(false);
		playCountDataSet.setHighlightEnabled(false);
		playCountDataSet.setColor(getResources().getColor(R.color.primary));
		dataSets.add(playCountDataSet);

		BarDataSet winsDataSet = new BarDataSet(winValues, getString(R.string.title_wins));
		winsDataSet.setDrawValues(false);
		winsDataSet.setHighlightEnabled(false);
		winsDataSet.setColor(getResources().getColor(R.color.primary_dark));
		dataSets.add(winsDataSet);

		BarData data = new BarData(playersLabels, dataSets);
		mPlayCountChart.setData(data);
		mPlayCountChart.animateY(1000, EasingOption.EaseInOutBack);

		if (stats.hasScores()) {
			addStatRow(mScoreTable, new Builder().labelId(R.string.average).value(stats.getAverageScore()));
			addStatRow(mScoreTable, new Builder().labelId(R.string.average_win).value(stats.getAverageWinningScore()));
			addStatRow(mScoreTable, new Builder().labelId(R.string.high).value(stats.getHighScore(), SCORE_FORMAT).infoText(stats.getHighScorers()));
			addStatRow(mScoreTable, new Builder().labelId(R.string.low).value(stats.getLowScore(), SCORE_FORMAT).infoText(stats.getLowScorers()));
			addStatRow(mScoreTable, new Builder().value(getString(R.string.title_personal)));
			addStatRow(mScoreTable, new Builder().labelId(R.string.average).value(stats.getPersonalAverageScore()));
			addStatRow(mScoreTable, new Builder().labelId(R.string.high).value(stats.getPersonalHighScore(), SCORE_FORMAT));
			addStatRow(mScoreTable, new Builder().labelId(R.string.low).value(stats.getPersonalLowScore(), SCORE_FORMAT));
			mScores.setVisibility(View.VISIBLE);
		} else {
			mScores.setVisibility(View.GONE);
		}

		addStatRowMaybe(mDatesTable, new Builder().labelId(R.string.play_stat_first_play).valueAsDate(stats.getFirstPlayDate(), getActivity()));
		addStatRowMaybe(mDatesTable, new Builder().labelId(R.string.play_stat_nickel).valueAsDate(stats.getNickelDate(), getActivity()));
		addStatRowMaybe(mDatesTable, new Builder().labelId(R.string.play_stat_dime).valueAsDate(stats.getDimeDate(), getActivity()));
		addStatRowMaybe(mDatesTable, new Builder().labelId(R.string.play_stat_quarter).valueAsDate(stats.getQuarterDate(), getActivity()));
		addStatRowMaybe(mDatesTable, new Builder().labelId(R.string.play_stat_half_dollar).valueAsDate(stats.getHalfDollarDate(), getActivity()));
		addStatRowMaybe(mDatesTable, new Builder().labelId(R.string.play_stat_dollar).valueAsDate(stats.getDollarDate(), getActivity()));
		addStatRowMaybe(mDatesTable, new Builder().labelId(R.string.play_stat_last_play).valueAsDate(stats.getLastPlayDate(), getActivity()));

		addStatRow(mPlayTimeTable, new Builder().labelId(R.string.play_stat_hours_played).value((int) stats.getHoursPlayed()));
		int average = stats.getAveragePlayTime();
		if (average > 0) {
			addStatRow(mPlayTimeTable, new Builder().labelId(R.string.play_stat_average_play_time).valueInMinutes(average));
			if (mPlayingTime > 0) {
				if (average > mPlayingTime) {
					addStatRow(mPlayTimeTable, new Builder().labelId(R.string.play_stat_average_play_time_slower).valueInMinutes(average - mPlayingTime));
				} else if (mPlayingTime > average) {
					addStatRow(mPlayTimeTable, new Builder().labelId(R.string.play_stat_average_play_time_faster).valueInMinutes(mPlayingTime - average));
				}
				// don't display anything if the average is exactly as expected
			}
		}
		int averagePerPlayer = stats.getAveragePlayTimePerPlayer();
		if (averagePerPlayer > 0) {
			addStatRow(mPlayTimeTable, new Builder().labelId(R.string.play_stat_average_play_time_per_player).valueInMinutes(averagePerPlayer));
		}

		for (Entry<String, Integer> location : stats.getPlaysPerLocation()) {
			mLocations.setVisibility(View.VISIBLE);
			addStatRow(mLocationsTable, new Builder().labelText(location.getKey()).value(location.getValue()));
		}

		int row = 0;
		for (Entry<String, PlayerStats> playerStats : stats.getPlayerStats()) {
			mOpponents.setVisibility(View.VISIBLE);
			PlayerStats ps = playerStats.getValue();

			PlayerStatView psv = new PlayerStatView(getActivity());
			psv.setName(playerStats.getKey());
			psv.setPlayCount(ps.playCount);
			psv.setWins(ps.wins);
			psv.setAverageScore(ps.getAverageScore());
			if (row % 2 == 1) {
				psv.setBackgroundResource(R.color.primary_transparent);
			}
			mOpponentsTable.addView(psv);
			row++;
		}

		if (mRating > 0) {
			addStatRow(mAdvancedTable, new Builder().labelId(R.string.play_stat_fhm).value(stats.calculateFhm()).infoId(R.string.play_stat_fhm_info));
			addStatRow(mAdvancedTable, new Builder().labelId(R.string.play_stat_hhm).value(stats.calculateHhm()).infoId(R.string.play_stat_hhm_info));
			addStatRow(mAdvancedTable, new Builder().labelId(R.string.play_stat_ruhm).value(stats.calculateRuhm()).infoId(R.string.play_stat_ruhm_info));
		}
		addStatRow(mAdvancedTable, new Builder().labelId(R.string.play_stat_utilization).valueAsPercentage(stats.calculateUtilization()).infoId(R.string.play_stat_utilization_info));
		int hIndexOffset = stats.getHIndexOffset();
		if (hIndexOffset == -1) {
			addStatRow(mAdvancedTable, new Builder().labelId(R.string.play_stat_h_index_offset_in));
		} else {
			addStatRow(mAdvancedTable, new Builder().labelId(R.string.play_stat_h_index_offset_out).value(hIndexOffset));
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

	private void addStatRowMaybe(ViewGroup container, Builder builder) {
		if (builder.hasValue()) {
			PlayStatView view = builder.build(getActivity());
			container.addView(view);
		}
	}

	private void addStatRow(ViewGroup container, Builder builder) {
		PlayStatView view = builder.build(getActivity());
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

	private class PlayerStats {
		private String key;
		private int playCount;
		public int wins;
		private double totalScore;
		private int totalScoreCount;

		public PlayerStats() {
			playCount = 0;
			wins = 0;
			totalScore = 0.0;
			totalScoreCount = 0;
		}

		public void add(PlayModel play, PlayerModel player) {
			playCount += play.quantity;
			if (player.win) {
				wins += play.quantity;
			}
			if (StringUtils.isNumeric(player.score)) {
				totalScore += StringUtils.parseDouble(player.score) * play.quantity;
				totalScoreCount += play.quantity;
			}
		}

		public double getAverageScore() {
			return totalScore / totalScoreCount;
		}
	}

	private class Stats {
		private double mLambda;
		private String mCurrentYear;

		private final Map<Integer, PlayModel> mPlays = new LinkedHashMap<>();
		private final Map<String, PlayerStats> mPlayerStats = new HashMap<>();

		private String mFirstPlayDate;
		private String mLastPlayDate;
		private String mNickelDate;
		private String mDimeDate;
		private String mQuarterDate;
		private String mHalfDollarDate;
		private String mDollarDate;
		private int mPlayCount;
		private int mPlayCountIncomplete;
		private int mPlayCountWithLength;
		private int mPlayCountThisYear;
		private int mPlayerCountSumWithLength;
		private Map<Integer, Integer> mPlayCountPerPlayerCount;
		private int mRealMinutesPlayed;
		private int mEstimatedMinutesPlayed;
		private int mWinnableGames;
		private double mTotalScore;
		private int mTotalScoreCount;
		private double mHighScore;
		private double mLowScore;
		private int mTotalWinningScoreCount;
		private double mTotalWinningScore;
		private double mPersonalTotalScore;
		private int mPersonalTotalScoreCount;
		private double mPersonalHighScore;
		private double mPersonalLowScore;
		private final Set<String> mHighScorers = new HashSet<>();
		private final Set<String> mLowScorers = new HashSet<>();
		private int mWonGames;
		private int mWonPlayerCount;
		private Map<Integer, Integer> mWinsPerPlayerCount;
		private Map<String, Integer> mPlaysPerLocation;
		private final Set<String> mMonths = new HashSet<>();

		public Stats(Cursor cursor) {
			init();
			do {
				PlayModel pm = new PlayModel(cursor);
				mPlays.put(pm.playId, pm);
			} while (cursor.moveToNext());
		}

		private void init() {
			mLambda = Math.log(0.1) / -10;
			mCurrentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

			mPlays.clear();

			mFirstPlayDate = null;
			mLastPlayDate = null;
			mNickelDate = null;
			mDimeDate = null;
			mQuarterDate = null;
			mHalfDollarDate = null;
			mDollarDate = null;
			mPlayCount = 0;
			mPlayCountIncomplete = 0;
			mPlayCountWithLength = 0;
			mPlayCountThisYear = 0;
			mPlayerCountSumWithLength = 0;
			mPlayCountPerPlayerCount = new ArrayMap<>();
			mRealMinutesPlayed = 0;
			mEstimatedMinutesPlayed = 0;
			mWinnableGames = 0;
			mTotalScore = 0;
			mTotalScoreCount = 0;
			mHighScore = Integer.MIN_VALUE;
			mLowScore = Integer.MAX_VALUE;
			mTotalWinningScoreCount = 0;
			mTotalWinningScore = 0;
			mPersonalTotalScore = 0;
			mPersonalTotalScoreCount = 0;
			mPersonalHighScore = Integer.MIN_VALUE;
			mPersonalLowScore = Integer.MAX_VALUE;
			mHighScorers.clear();
			mLowScorers.clear();
			mWonGames = 0;
			mWonPlayerCount = 0;
			mWinsPerPlayerCount = new ArrayMap<>();
			mPlaysPerLocation = new HashMap<>();
			mMonths.clear();
		}

		public void calculate() {
			boolean includeIncomplete = PreferencesUtils.logPlayStatsIncomplete(getActivity());
			for (PlayModel pm : mPlays.values()) {
				if (!includeIncomplete && pm.incomplete) {
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
				if (mPlayCount < 50 && (mPlayCount + pm.quantity) >= 50) {
					mHalfDollarDate = pm.date;
				}
				if (mPlayCount < 100 && (mPlayCount + pm.quantity) >= 100) {
					mDollarDate = pm.date;
				}
				mPlayCount += pm.quantity;
				if (pm.getYear().equals(mCurrentYear)) {
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

				if (pm.isWinnable()) {
					mWinnableGames += pm.quantity;
					if (pm.didWin(AccountUtils.getUsername(getActivity()))) {
						mWonGames += pm.quantity;
						mWonPlayerCount += pm.quantity * pm.playerCount;
						int previousQuantity = 0;
						if (mWinsPerPlayerCount.containsKey(pm.playerCount)) {
							previousQuantity = mWinsPerPlayerCount.get(pm.playerCount);
						}
						mWinsPerPlayerCount.put(pm.playerCount, previousQuantity + pm.quantity);
					}
				}

				if (!TextUtils.isEmpty(pm.location)) {
					int previousPlays = 0;
					if (mPlaysPerLocation.containsKey(pm.location)) {
						previousPlays = mPlaysPerLocation.get(pm.location);
					}
					mPlaysPerLocation.put(pm.location, previousPlays + pm.quantity);
				}

				for (PlayerModel player : pm.getPlayers()) {
					if (!TextUtils.isEmpty(player.getUniqueName()) &&
						!AccountUtils.getUsername(getActivity()).equals(player.username)) {
						PlayerStats ps = mPlayerStats.get(player.getUniqueName());
						if (ps == null) {
							ps = new PlayerStats();
						}
						ps.add(pm, player);
						mPlayerStats.put(player.getUniqueName(), ps);
					}

					if (StringUtils.isNumeric(player.score)) {
						double i = StringUtils.parseDouble(player.score);
						mTotalScoreCount++;
						mTotalScore += i;
						if (i > mHighScore) {
							mHighScore = i;
							mHighScorers.clear();
							mHighScorers.add(player.getUniqueName());
						} else if (i == mHighScore) {
							mHighScorers.add(player.getUniqueName());
						}
						if (i < mLowScore) {
							mLowScore = i;
							mLowScorers.clear();
							mLowScorers.add(player.getUniqueName());
						} else if (i == mLowScore) {
							mLowScorers.add(player.getUniqueName());
						}
						if (player.win) {
							mTotalWinningScoreCount++;
							mTotalWinningScore += i;
						}
						if (AccountUtils.getUsername(getActivity()).equals(player.username)) {
							mPersonalTotalScoreCount++;
							mPersonalTotalScore += i;
							if (i > mPersonalHighScore) {
								mPersonalHighScore = i;
							}
							if (i < mPersonalLowScore) {
								mPersonalLowScore = i;
							}
						}
					}
				}

				mMonths.add(pm.getYearAndMonth());
			}
		}

		public void addPlayerData(Cursor cursor) {
			do {
				PlayerModel pm = new PlayerModel(cursor);
				if (!mPlays.containsKey(pm.playId)) {
					Timber.e("Play " + pm.playId + " not found in the play map!");
					return;
				}
				mPlays.get(pm.playId).addPlayer(pm);
			} while (cursor.moveToNext());
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

		private String getHalfDollarDate() {
			return mHalfDollarDate;
		}

		private String getDollarDate() {
			return mDollarDate;
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

		public int getMinPlayerCount() {
			int min = Integer.MAX_VALUE;
			for (Integer playerCount : mPlayCountPerPlayerCount.keySet()) {
				if (playerCount < min) {
					min = playerCount;
				}
			}
			return min;
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

		public int getWins(int playerCount) {
			if (mWinsPerPlayerCount.containsKey(playerCount)) {
				return mWinsPerPlayerCount.get(playerCount);
			}
			return 0;
		}

		public boolean hasWins() {
			return mWinnableGames > 0;
		}

		public double getWinPercentage() {
			return (double) mWonGames / (double) mWinnableGames;
		}

		public int getWinSkill() {
			return (int) (((double) mWonPlayerCount / (double) mWinnableGames) * 100);
		}

		public boolean hasScores() {
			return mTotalScoreCount > 0;
		}

		public double getAverageScore() {
			return mTotalScore / mTotalScoreCount;
		}

		public double getHighScore() {
			return mHighScore;
		}

		public String getHighScorers() {
			return StringUtils.formatList(new ArrayList(mHighScorers));
		}

		public double getLowScore() {
			return mLowScore;
		}

		public String getLowScorers() {
			return StringUtils.formatList(new ArrayList(mLowScorers));
		}

		public double getAverageWinningScore() {
			return mTotalWinningScore / mTotalWinningScoreCount;
		}

		public double getPersonalAverageScore() {
			return mPersonalTotalScore / mPersonalTotalScoreCount;
		}

		public double getPersonalHighScore() {
			return mPersonalHighScore;
		}

		public double getPersonalLowScore() {
			return mPersonalLowScore;
		}

		public List<Entry<String, PlayerStats>> getPlayerStats() {
			Set<Entry<String, PlayerStats>> set = mPlayerStats.entrySet();
			List<Entry<String, PlayerStats>> list = new ArrayList(set);
			Collections.sort(list, new Comparator<Entry<String, PlayerStats>>() {
				@Override
				public int compare(Entry<String, PlayerStats> lhs, Entry<String, PlayerStats> rhs) {
					if (lhs.getValue().playCount > rhs.getValue().playCount) {
						return -1;
					} else if (lhs.getValue().playCount < rhs.getValue().playCount) {
						return 1;
					} else {
						return lhs.getKey().compareTo(rhs.getKey());
					}
				}
			});
			return list;
		}

		public List<Entry<String, Integer>> getPlaysPerLocation() {
			Set<Entry<String, Integer>> set = mPlaysPerLocation.entrySet();
			List<Entry<String, Integer>> list = new ArrayList(set);
			Collections.sort(list, new Comparator<Entry<String, Integer>>() {
				@Override
				public int compare(Entry<String, Integer> lhs, Entry<String, Integer> rhs) {
					if (lhs.getValue() > rhs.getValue()) {
						return -1;
					} else if (lhs.getValue() < rhs.getValue()) {
						return 1;
					} else {
						return lhs.getKey().compareTo(rhs.getKey());
					}
				}
			});
			return list;
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

		public int getHIndexOffset() {
			int hIndex = PreferencesUtils.getHIndex(getActivity());
			if (mPlayCount >= hIndex) {
				return -1;
			} else {
				return hIndex - mPlayCount + 1;
			}
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
		final int playId;
		final String date;
		final int length;
		final int quantity;
		final boolean incomplete;
		final int playerCount;
		final boolean noWinStats;
		final String location;
		final List<PlayerModel> mPlayers = new ArrayList<>();

		PlayModel(Cursor cursor) {
			playId = cursor.getInt(PlayQuery.PLAY_ID);
			date = cursor.getString(PlayQuery.DATE);
			length = cursor.getInt(PlayQuery.LENGTH);
			quantity = cursor.getInt(PlayQuery.QUANTITY);
			incomplete = CursorUtils.getBoolean(cursor, PlayQuery.INCOMPLETE);
			playerCount = cursor.getInt(PlayQuery.PLAYER_COUNT);
			noWinStats = CursorUtils.getBoolean(cursor, PlayQuery.NO_WIN_STATS);
			location = cursor.getString(PlayQuery.LOCATION);
			mPlayers.clear();
		}

		public List<PlayerModel> getPlayers() {
			return mPlayers;
		}

		public String getYear() {
			return date.substring(0, 4);
		}

		public String getYearAndMonth() {
			return date.substring(0, 7);
		}

		public void addPlayer(PlayerModel player) {
			mPlayers.add(player);
		}

		public boolean isWinnable() {
			if (noWinStats) {
				return false;
			}
			for (PlayerModel player : mPlayers) {
				if (player.win) {
					return true;
				}
			}
			return false;
		}

		public boolean didWin(@NonNull String username) {
			if (noWinStats) {
				return false;
			}
			for (PlayerModel player : mPlayers) {
				if (username.equals(player.username)) {
					return player.win;
				}
			}
			return false;
		}
	}

	private class PlayerModel {
		final int playId;
		final String username;
		final String name;
		final boolean win;
		final String score;

		PlayerModel(Cursor cursor) {
			playId = cursor.getInt(PlayerQuery.PLAY_ID);
			username = cursor.getString(PlayerQuery.USER_NAME);
			name = cursor.getString(PlayerQuery.NAME);
			win = CursorUtils.getBoolean(cursor, PlayerQuery.WIN);
			score = cursor.getString(PlayerQuery.SCORE);
		}

		public String getUniqueName() {
			if (TextUtils.isEmpty(username)) {
				return name;
			}
			return name + " (" + username + ")";
		}
	}

	private interface PlayQuery {
		int _TOKEN = 0x01;
		String[] PROJECTION = { Plays._ID, Plays.PLAY_ID, Plays.DATE, PlayItems.NAME, PlayItems.OBJECT_ID,
			Plays.LOCATION, Plays.QUANTITY, Plays.LENGTH, Plays.SYNC_STATUS, Plays.PLAYER_COUNT, Games.THUMBNAIL_URL,
			Plays.INCOMPLETE, Plays.NO_WIN_STATS };
		int PLAY_ID = 1;
		int DATE = 2;
		int LOCATION = 5;
		int QUANTITY = 6;
		int LENGTH = 7;
		int PLAYER_COUNT = 9;
		int INCOMPLETE = 11;
		int NO_WIN_STATS = 12;
	}

	private interface PlayerQuery {
		int _TOKEN = 0x03;
		String[] PROJECTION = { PlayPlayers._ID, PlayPlayers.PLAY_ID, PlayPlayers.USER_NAME, PlayPlayers.WIN, PlayPlayers.SCORE,
			PlayPlayers.NAME };
		int PLAY_ID = 1;
		int USER_NAME = 2;
		int WIN = 3;
		int SCORE = 4;
		int NAME = 5;
	}

	private interface GameQuery {
		int _TOKEN = 0x02;
		String[] PROJECTION = { Games._ID, Collection.RATING, Games.PLAYING_TIME };
		int RATING = 1;
		int PLAYING_TIME = 2;
	}
}