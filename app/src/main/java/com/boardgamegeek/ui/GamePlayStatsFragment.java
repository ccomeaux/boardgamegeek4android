package com.boardgamegeek.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.ArrayMap;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.widget.IntegerYAxisValueFormatter;
import com.boardgamegeek.ui.widget.PlayStatView;
import com.boardgamegeek.ui.widget.PlayStatView.Builder;
import com.boardgamegeek.ui.widget.PlayerStatView;
import com.boardgamegeek.ui.widget.ScoreGraphView;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.PaletteUtils;
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

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import timber.log.Timber;

public class GamePlayStatsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final DecimalFormat SCORE_FORMAT = new DecimalFormat("0.##");
	private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private int gameId;

	private int playingTime;
	private double personalRating;
	private Stats stats;
	private final SparseBooleanArray selectedItems = new SparseBooleanArray();

	private Unbinder unbinder;
	@BindView(R.id.progress) ContentLoadingProgressBar progressView;
	@BindView(R.id.empty) View emptyView;
	@BindView(R.id.data) View dataView;
	@BindView(R.id.table_play_count) TableLayout playCountTable;
	@BindView(R.id.chart_play_count) HorizontalBarChart playCountChart;
	@BindView(R.id.card_score) View scoresCard;
	@BindView(R.id.low_score) TextView lowScoreView;
	@BindView(R.id.average_score) TextView averageScoreView;
	@BindView(R.id.average_win_score) TextView averageWinScoreView;
	@BindView(R.id.score_graph) ScoreGraphView scoreGraphView;
	@BindView(R.id.high_score) TextView highScoreView;
	@BindView(R.id.card_players) View playersCard;
	@BindView(R.id.list_players) LinearLayout playersList;
	@BindView(R.id.table_dates) TableLayout datesTable;
	@BindView(R.id.table_play_time) TableLayout playTimeTable;
	@BindView(R.id.card_locations) View locationsCard;
	@BindView(R.id.table_locations) TableLayout locationsTable;
	@BindView(R.id.table_advanced) TableLayout advancedTable;
	@BindViews({
		R.id.header_play_count,
		R.id.header_scores,
		R.id.header_players,
		R.id.header_dates,
		R.id.header_play_time,
		R.id.header_locations,
		R.id.header_advanced
	}) List<TextView> colorizedHeaders;
	@BindViews({
		R.id.score_help
	}) List<ImageView> colorizedIcons;

	private Transition playerTransition;
	private int headerColor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		Uri uri = intent.getData();
		gameId = Games.getGameId(uri);
		headerColor = intent.getIntExtra(ActivityUtils.KEY_HEADER_COLOR, R.color.accent);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game_play_stats, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		ButterKnife.apply(colorizedHeaders, PaletteUtils.rgbTextViewSetter, headerColor);
		ButterKnife.apply(colorizedIcons, PaletteUtils.rgbIconSetter, headerColor);

		playCountChart.setDrawGridBackground(false);
		playCountChart.getAxisRight().setValueFormatter(new IntegerYAxisValueFormatter());
		playCountChart.getAxisLeft().setEnabled(false);
		playCountChart.getXAxis().setDrawGridLines(false);
		playCountChart.setDescription(null);

		if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
			playerTransition = new AutoTransition();
			playerTransition.setDuration(150);
			AnimationUtils.setInterpolator(getContext(), playerTransition);
		}

		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().restartLoader(GameQuery._TOKEN, null, this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
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
					new String[] { String.valueOf(gameId) },
					null);
				loader.setUpdateThrottle(5000);
				break;
			case PlayQuery._TOKEN:
				loader = new CursorLoader(getActivity(),
					Plays.CONTENT_URI,
					PlayQuery.PROJECTION,
					Plays.OBJECT_ID + "=? AND " + Plays.SYNC_STATUS + "=?",
					new String[] { String.valueOf(gameId), String.valueOf(Play.SYNC_STATUS_SYNCED) },
					Plays.DATE + " ASC");
				loader.setUpdateThrottle(5000);
				break;
			case PlayerQuery._TOKEN:
				loader = new CursorLoader(getActivity(),
					Plays.buildPlayersUri(),
					PlayerQuery.PROJECTION,
					Plays.OBJECT_ID + "=? AND " + Plays.SYNC_STATUS + "=?",
					new String[] { String.valueOf(gameId), String.valueOf(Play.SYNC_STATUS_SYNCED) },
					null);
				loader.setUpdateThrottle(5000);
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
				playingTime = cursor.getInt(GameQuery.PLAYING_TIME);
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
					personalRating = 0.0;
				} else {
					personalRating = ratingSum / ratingCount;
				}
				getLoaderManager().restartLoader(PlayQuery._TOKEN, null, this);
				break;
			case PlayQuery._TOKEN:
				stats = new Stats(cursor, personalRating);
				getLoaderManager().restartLoader(PlayerQuery._TOKEN, null, this);
				break;
			case PlayerQuery._TOKEN:
				stats.addPlayerData(cursor);
				stats.calculate();
				bindUi(stats);
				showData();
				break;
			default:
				cursor.close();
				break;
		}
	}

	private void bindUi(Stats stats) {
		playCountTable.removeAllViews();
		datesTable.removeAllViews();
		playTimeTable.removeAllViews();
		advancedTable.removeAllViews();

		if (!TextUtils.isEmpty(stats.getDollarDate())) {
			addStatRow(playCountTable, new Builder().value(getString(R.string.play_stat_dollar)));
		} else if (!TextUtils.isEmpty(stats.getHalfDollarDate())) {
			addStatRow(playCountTable, new Builder().value(getString(R.string.play_stat_half_dollar)));
		} else if (!TextUtils.isEmpty(stats.getQuarterDate())) {
			addStatRow(playCountTable, new Builder().value(getString(R.string.play_stat_quarter)));
		} else if (!TextUtils.isEmpty(stats.getDimeDate())) {
			addStatRow(playCountTable, new Builder().value(getString(R.string.play_stat_dime)));
		} else if (!TextUtils.isEmpty(stats.getNickelDate())) {
			addStatRow(playCountTable, new Builder().value(getString(R.string.play_stat_nickel)));
		}
		addStatRow(playCountTable, new Builder().labelId(R.string.play_stat_play_count).value(stats.getPlayCount()));
		if (stats.getPlayCountIncomplete() > 0) {
			addStatRow(playCountTable, new Builder().labelId(R.string.play_stat_play_count_incomplete).value(stats.getPlayCountIncomplete()));

		}
		addStatRow(playCountTable, new Builder().labelId(R.string.play_stat_months_played).value(stats.getMonthsPlayed()));
		if (stats.getPlayRate() > 0) {
			addStatRow(playCountTable, new Builder().labelId(R.string.play_stat_play_rate).value(stats.getPlayRate()));
		}

		ArrayList<String> playersLabels = new ArrayList<>();
		ArrayList<BarEntry> playCountValues = new ArrayList<>();
		ArrayList<BarEntry> winValues = new ArrayList<>();
		int index = 0;
		for (int i = stats.getMinPlayerCount(); i <= stats.getMaxPlayerCount(); i++) {
			playersLabels.add(String.valueOf(i));
			playCountValues.add(new BarEntry(new float[] { stats.getWinnablePlayCount(i), stats.getPlayCount(i) - stats.getWinnablePlayCount(i) }, index));
			winValues.add(new BarEntry(stats.getWinCount(i), index));
			index++;
		}
		ArrayList<IBarDataSet> dataSets = new ArrayList<>();

		BarDataSet playCountDataSet = new BarDataSet(playCountValues, getString(R.string.title_plays));
		playCountDataSet.setDrawValues(false);
		playCountDataSet.setHighlightEnabled(false);
		playCountDataSet.setColors(new int[] { ContextCompat.getColor(getContext(), R.color.dark_blue), ContextCompat.getColor(getContext(), R.color.light_blue) });
		playCountDataSet.setStackLabels(new String[] { getString(R.string.winnable), getString(R.string.all) });
		dataSets.add(playCountDataSet);

		BarDataSet winsDataSet = new BarDataSet(winValues, getString(R.string.title_wins));
		winsDataSet.setDrawValues(false);
		winsDataSet.setHighlightEnabled(false);
		winsDataSet.setColor(ContextCompat.getColor(getContext(), R.color.orange));
		dataSets.add(winsDataSet);

		BarData data = new BarData(playersLabels, dataSets);
		playCountChart.setData(data);
		playCountChart.animateY(1000, EasingOption.EaseInOutBack);

		if (stats.hasScores()) {
			lowScoreView.setText(SCORE_FORMAT.format(stats.getLowScore()));
			averageScoreView.setText(SCORE_FORMAT.format(stats.getAverageScore()));
			averageWinScoreView.setText(SCORE_FORMAT.format(stats.getAverageWinningScore()));
			highScoreView.setText(SCORE_FORMAT.format(stats.getHighScore()));
			if (stats.getHighScore() > stats.getLowScore()) {
				scoreGraphView.setLowScore(stats.getLowScore());
				scoreGraphView.setAverageScore(stats.getAverageScore());
				scoreGraphView.setAverageWinScore(stats.getAverageWinningScore());
				scoreGraphView.setHighScore(stats.getHighScore());
				scoreGraphView.setVisibility(View.VISIBLE);
			}
			scoresCard.setVisibility(View.VISIBLE);
		} else {
			scoresCard.setVisibility(View.GONE);
		}

		addStatRowMaybe(datesTable, new Builder().labelId(R.string.play_stat_first_play).valueAsDate(stats.getFirstPlayDate(), getActivity()));
		addStatRowMaybe(datesTable, new Builder().labelId(R.string.play_stat_nickel).valueAsDate(stats.getNickelDate(), getActivity()));
		addStatRowMaybe(datesTable, new Builder().labelId(R.string.play_stat_dime).valueAsDate(stats.getDimeDate(), getActivity()));
		addStatRowMaybe(datesTable, new Builder().labelId(R.string.play_stat_quarter).valueAsDate(stats.getQuarterDate(), getActivity()));
		addStatRowMaybe(datesTable, new Builder().labelId(R.string.play_stat_half_dollar).valueAsDate(stats.getHalfDollarDate(), getActivity()));
		addStatRowMaybe(datesTable, new Builder().labelId(R.string.play_stat_dollar).valueAsDate(stats.getDollarDate(), getActivity()));
		addStatRowMaybe(datesTable, new Builder().labelId(R.string.play_stat_last_play).valueAsDate(stats.getLastPlayDate(), getActivity()));

		addStatRow(playTimeTable, new Builder().labelId(R.string.play_stat_hours_played).value((int) stats.getHoursPlayed()));
		int average = stats.getAveragePlayTime();
		if (average > 0) {
			addStatRow(playTimeTable, new Builder().labelId(R.string.play_stat_average_play_time).valueInMinutes(average));
			if (playingTime > 0) {
				if (average > playingTime) {
					addStatRow(playTimeTable, new Builder().labelId(R.string.play_stat_average_play_time_slower).valueInMinutes(average - playingTime));
				} else if (playingTime > average) {
					addStatRow(playTimeTable, new Builder().labelId(R.string.play_stat_average_play_time_faster).valueInMinutes(playingTime - average));
				}
				// don't display anything if the average is exactly as expected
			}
		}
		int averagePerPlayer = stats.getAveragePlayTimePerPlayer();
		if (averagePerPlayer > 0) {
			addStatRow(playTimeTable, new Builder().labelId(R.string.play_stat_average_play_time_per_player).valueInMinutes(averagePerPlayer));
		}

		locationsTable.removeAllViews();
		for (Entry<String, Integer> location : stats.getPlaysPerLocation()) {
			locationsCard.setVisibility(View.VISIBLE);
			addStatRow(locationsTable, new Builder().labelText(location.getKey()).value(location.getValue()));
		}

		playersList.removeAllViews();
		int position = 0;
		for (Entry<String, PlayerStats> playerStats : stats.getPlayerStats()) {
			position++;
			playersCard.setVisibility(View.VISIBLE);
			PlayerStats ps = playerStats.getValue();

			final PlayerStatView view = new PlayerStatView(getActivity());
			view.setName(playerStats.getKey());
			view.setWinInfo(ps.wins, ps.winnableGames);
			view.setWinSkill(ps.getWinSkill());

			view.setOverallLowScore(stats.getLowScore());
			view.setOverallAverageScore(stats.getAverageScore());
			view.setOverallAverageWinScore(stats.getAverageWinningScore());
			view.setOverallHighScore(stats.getHighScore());
			view.setLowScore(ps.getLowScore());
			view.setAverageScore(ps.getAverageScore());
			view.setAverageWinScore(ps.getAverageWinScore());
			view.setHighScore(ps.getHighScore());

			view.showScores(selectedItems.get(position, false));
			if (stats.hasScores()) {
				final int finalPosition = position;
				view.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
							TransitionManager.beginDelayedTransition(playersList, playerTransition);
						}

						if (selectedItems.get(finalPosition, false)) {
							selectedItems.delete(finalPosition);
							view.showScores(false);
						} else {
							selectedItems.put(finalPosition, true);
							view.showScores(true);
						}
					}
				});
			}
			playersList.addView(view);
		}

		if (personalRating > 0) {
			addStatRow(advancedTable, new Builder().labelId(R.string.play_stat_fhm).value(stats.calculateFhm()).infoId(R.string.play_stat_fhm_info));
			addStatRow(advancedTable, new Builder().labelId(R.string.play_stat_hhm).value(stats.calculateHhm()).infoId(R.string.play_stat_hhm_info));
			addStatRow(advancedTable, new Builder().labelId(R.string.play_stat_ruhm).value(stats.calculateRuhm()).infoId(R.string.play_stat_ruhm_info));
		}
		addStatRow(advancedTable, new Builder().labelId(R.string.play_stat_utilization).valueAsPercentage(stats.calculateUtilization()).infoId(R.string.play_stat_utilization_info));
		int hIndexOffset = stats.getHIndexOffset();
		if (hIndexOffset == -1) {
			addStatRow(advancedTable, new Builder().labelId(R.string.play_stat_h_index_offset_in));
		} else {
			addStatRow(advancedTable, new Builder().labelId(R.string.play_stat_h_index_offset_out).value(hIndexOffset));
		}
	}

	private void showEmpty() {
		progressView.hide();
		AnimationUtils.fadeOut(dataView);
		AnimationUtils.fadeIn(emptyView);
	}

	private void showData() {
		progressView.hide();
		AnimationUtils.fadeOut(emptyView);
		AnimationUtils.fadeIn(dataView);
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

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@OnClick(R.id.score_help)
	public void onScoreHelpClick() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle(R.string.title_scores).setView(R.layout.dialog_help_score);
		builder.show();
	}

	@OnClick(R.id.low_score)
	public void onLowScoreClick() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle(R.string.title_low_scorers).setMessage(stats.getLowScorers());
		builder.show();
	}

	@OnClick(R.id.high_score)
	public void onHighScoreClick() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle(R.string.title_high_scorers).setMessage(stats.getHighScorers());
		builder.show();
	}

	private class PlayerStats {
		private String username;
		private int playCount;
		private int wins;
		private int winsWithScore;
		private int winnableGames;
		private int winsTimesPlayers;
		private final SparseIntArray winsByPlayerCount = new SparseIntArray();
		private final SparseIntArray playsByPlayerCount = new SparseIntArray();
		private final SparseIntArray winnablePlaysByPlayerCount = new SparseIntArray();
		private double totalScore;
		private double winningScore;
		private int totalScoreCount;
		private double highScore;
		private double lowScore;

		public PlayerStats() {
			username = "";
			playCount = 0;
			wins = 0;
			winsWithScore = 0;
			winnableGames = 0;
			winsTimesPlayers = 0;
			winsByPlayerCount.clear();
			totalScore = 0.0;
			winningScore = 0.0;
			totalScoreCount = 0;
			highScore = Integer.MIN_VALUE;
			lowScore = Integer.MAX_VALUE;
		}

		public void add(PlayModel play, PlayerModel player) {
			username = player.username;
			playCount += play.quantity;
			addByPlayerCount(playsByPlayerCount, play.playerCount, play.quantity);

			if (play.isWinnable()) {
				winnableGames += play.quantity;
				addByPlayerCount(winnablePlaysByPlayerCount, play.playerCount, play.quantity);
				if (player.win) {
					wins += play.quantity;
					winsTimesPlayers += play.quantity * play.playerCount;
					if (StringUtils.isNumeric(player.score)) winsWithScore += play.quantity;
					addByPlayerCount(winsByPlayerCount, play.playerCount, play.quantity);
				}
			}
			if (StringUtils.isNumeric(player.score)) {
				final double score = StringUtils.parseDouble(player.score);
				totalScore += score * play.quantity;
				totalScoreCount += play.quantity;
				if (score < lowScore) lowScore = score;
				if (score > highScore) highScore = score;
				if (play.isWinnable() && player.win) {
					winningScore += score * play.quantity;
				}
			}
		}

		private void addByPlayerCount(SparseIntArray playerCountMap, int playerCount, int quantity) {
			playerCountMap.put(playerCount, playerCountMap.get(playerCount) + quantity);
		}

		public String getUsername() {
			return username;
		}

		public int getWinCountByPlayerCount(int playerCount) {
			return winsByPlayerCount.get(playerCount);
		}

		public int getWinnablePlayCountByPlayerCount(int playerCount) {
			return winnablePlaysByPlayerCount.get(playerCount);
		}

		public int getPlayCountByPlayerCount(int playerCount) {
			return playsByPlayerCount.get(playerCount);
		}

		public int getWinSkill() {
			return (int) (((double) winsTimesPlayers / (double) winnableGames) * 100);
		}

		public double getAverageScore() {
			if (totalScoreCount == 0) return Integer.MIN_VALUE;
			return totalScore / totalScoreCount;
		}

		public double getAverageWinScore() {
			if (totalScoreCount == 0) return Integer.MIN_VALUE;
			if (winsWithScore == 0) return Integer.MIN_VALUE;
			return winningScore / winsWithScore;
		}

		public double getHighScore() {
			return highScore;
		}

		public double getLowScore() {
			return lowScore;
		}
	}

	private class Stats {
		private final double lambda = Math.log(0.1) / -10;
		private final String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

		private final Map<Integer, PlayModel> plays = new LinkedHashMap<>();
		private final Map<String, PlayerStats> playerStats = new HashMap<>();

		private final double personalRating;

		private String firstPlayDate;
		private String lastPlayDate;
		private String nickelDate;
		private String dimeDate;
		private String quarterDate;
		private String halfDollarDate;
		private String dollarDate;
		private int playCount;
		private int playCountIncomplete;
		private int playCountWithLength;
		private int playCountThisYear;
		private int playerCountSumWithLength;
		private Map<Integer, Integer> playCountPerPlayerCount;
		private int realMinutesPlayed;
		private int estimatedMinutesPlayed;
		private int numberOfWinnableGames;
		private double scoreSum;
		private int scoreCount;
		private double highScore;
		private double lowScore;
		private int winningScoreCount;
		private double winningScoreSum;
		private Map<String, Integer> playCountByLocation;
		private final Set<String> monthsPlayed = new HashSet<>();

		public Stats(Cursor cursor, double personalRating) {
			init();
			this.personalRating = personalRating;
			do {
				PlayModel model = new PlayModel(cursor);
				plays.put(model.playId, model);
			} while (cursor.moveToNext());
		}

		private void init() {
			plays.clear();

			// dates
			firstPlayDate = null;
			lastPlayDate = null;
			nickelDate = null;
			dimeDate = null;
			quarterDate = null;
			halfDollarDate = null;
			dollarDate = null;
			monthsPlayed.clear();

			playCount = 0;
			playCountIncomplete = 0;
			playCountWithLength = 0;
			playCountThisYear = 0;
			playerCountSumWithLength = 0;
			playCountPerPlayerCount = new ArrayMap<>();
			playCountByLocation = new HashMap<>();
			numberOfWinnableGames = 0;

			realMinutesPlayed = 0;
			estimatedMinutesPlayed = 0;

			scoreSum = 0;
			scoreCount = 0;
			highScore = Integer.MIN_VALUE;
			lowScore = Integer.MAX_VALUE;
			winningScoreCount = 0;
			winningScoreSum = 0;
		}

		public void calculate() {
			boolean includeIncomplete = PreferencesUtils.logPlayStatsIncomplete(getActivity());
			for (PlayModel play : plays.values()) {
				if (!includeIncomplete && play.incomplete) {
					playCountIncomplete += play.quantity;
					continue;
				}

				if (firstPlayDate == null) {
					firstPlayDate = play.date;
				}
				lastPlayDate = play.date;

				if (playCount < 5 && (playCount + play.quantity) >= 5) {
					nickelDate = play.date;
				}
				if (playCount < 10 && (playCount + play.quantity) >= 10) {
					dimeDate = play.date;
				}
				if (playCount < 25 && (playCount + play.quantity) >= 25) {
					quarterDate = play.date;
				}
				if (playCount < 50 && (playCount + play.quantity) >= 50) {
					halfDollarDate = play.date;
				}
				if (playCount < 100 && (playCount + play.quantity) >= 100) {
					dollarDate = play.date;
				}
				playCount += play.quantity;
				if (play.getYear().equals(currentYear)) {
					playCountThisYear += play.quantity;
				}

				if (play.length == 0) {
					estimatedMinutesPlayed += playingTime * play.quantity;
				} else {
					realMinutesPlayed += play.length;
					playCountWithLength += play.quantity;
					playerCountSumWithLength += play.playerCount * play.quantity;
				}

				if (play.playerCount > 0) {
					int previousQuantity = 0;
					if (playCountPerPlayerCount.containsKey(play.playerCount)) {
						previousQuantity = playCountPerPlayerCount.get(play.playerCount);
					}
					playCountPerPlayerCount.put(play.playerCount, previousQuantity + play.quantity);
				}

				if (play.isWinnable()) {
					numberOfWinnableGames += play.quantity;
				}

				if (!TextUtils.isEmpty(play.location)) {
					int previousPlays = 0;
					if (playCountByLocation.containsKey(play.location)) {
						previousPlays = playCountByLocation.get(play.location);
					}
					playCountByLocation.put(play.location, previousPlays + play.quantity);
				}

				for (PlayerModel player : play.getPlayers()) {
					if (!TextUtils.isEmpty(player.getUniqueName())) {
						PlayerStats playerStats = this.playerStats.get(player.getUniqueName());
						if (playerStats == null) {
							playerStats = new PlayerStats();
						}
						playerStats.add(play, player);
						this.playerStats.put(player.getUniqueName(), playerStats);
					}

					if (StringUtils.isNumeric(player.score)) {
						double score = StringUtils.parseDouble(player.score);

						scoreCount += play.quantity;
						scoreSum += score * play.quantity;

						if (player.win) {
							winningScoreCount += play.quantity;
							winningScoreSum += score * play.quantity;
						}

						if (score > highScore) highScore = score;
						if (score < lowScore) lowScore = score;
					}
				}

				monthsPlayed.add(play.getYearAndMonth());
			}
		}

		public void addPlayerData(Cursor cursor) {
			do {
				PlayerModel playerModel = new PlayerModel(cursor);
				if (!plays.containsKey(playerModel.playId)) {
					Timber.e("Play %s not found in the play map!", playerModel.playId);
					return;
				}
				plays.get(playerModel.playId).addPlayer(playerModel);
			} while (cursor.moveToNext());
		}

		public int getPlayCount() {
			return playCount;
		}

		public int getPlayCountIncomplete() {
			return playCountIncomplete;
		}

		public String getFirstPlayDate() {
			return firstPlayDate;
		}

		private String getNickelDate() {
			return nickelDate;
		}

		private String getDimeDate() {
			return dimeDate;
		}

		private String getQuarterDate() {
			return quarterDate;
		}

		private String getHalfDollarDate() {
			return halfDollarDate;
		}

		private String getDollarDate() {
			return dollarDate;
		}

		public String getLastPlayDate() {
			if (playCount > 0) {
				return lastPlayDate;
			}
			return null;
		}

		public double getHoursPlayed() {
			return (realMinutesPlayed + estimatedMinutesPlayed) / 60;
		}

		/* plays per month, only counting the active period) */
		public double getPlayRate() {
			long flash = calculateFlash();
			if (flash > 0) {
				double rate = ((double) (playCount * 365) / flash) / 12;
				return Math.min(rate, playCount);
			}
			return 0;
		}

		public int getAveragePlayTime() {
			if (playCountWithLength > 0) {
				return realMinutesPlayed / playCountWithLength;
			}
			return 0;
		}

		public int getAveragePlayTimePerPlayer() {
			if (playerCountSumWithLength > 0) {
				return realMinutesPlayed / playerCountSumWithLength;
			}
			return 0;
		}

		public int getMonthsPlayed() {
			return monthsPlayed.size();
		}

		public int getMinPlayerCount() {
			int min = Integer.MAX_VALUE;
			for (Integer playerCount : playCountPerPlayerCount.keySet()) {
				if (playerCount < min) {
					min = playerCount;
				}
			}
			return min;
		}

		public int getMaxPlayerCount() {
			int max = 0;
			for (Integer playerCount : playCountPerPlayerCount.keySet()) {
				if (playerCount > max) {
					max = playerCount;
				}
			}
			return max;
		}

		public int getWinCount(int playerCount) {
			PlayerStats ps = getPersonalStats();
			if (ps != null) {
				return ps.getWinCountByPlayerCount(playerCount);
			}
			return 0;
		}

		public int getWinnablePlayCount(int playerCount) {
			PlayerStats ps = getPersonalStats();
			if (ps != null) {
				return ps.getWinnablePlayCountByPlayerCount(playerCount);
			}
			return 0;
		}

		public int getPlayCount(int playerCount) {
			PlayerStats ps = getPersonalStats();
			if (ps != null) {
				return ps.getPlayCountByPlayerCount(playerCount);
			}
			return 0;
		}

		private PlayerStats getPersonalStats() {
			String username = AccountUtils.getUsername(getActivity());
			for (Entry<String, PlayerStats> ps : stats.getPlayerStats()) {
				if (username != null && username.equalsIgnoreCase(ps.getValue().getUsername())) {
					return ps.getValue();
				}
			}
			return null;
		}

		public boolean hasScores() {
			return scoreCount > 0;
		}

		public double getAverageScore() {
			return scoreSum / scoreCount;
		}

		public double getHighScore() {
			return highScore;
		}

		public String getHighScorers() {
			if (highScore == Integer.MIN_VALUE) return "";
			List<String> players = new ArrayList<>();
			for (Entry<String, PlayerStats> ps : playerStats.entrySet()) {
				if (ps.getValue().highScore == highScore) {
					players.add(ps.getKey());
				}
			}
			return StringUtils.formatList(players);
		}

		public double getLowScore() {
			return lowScore;
		}

		public String getLowScorers() {
			if (lowScore == Integer.MAX_VALUE) return "";
			List<String> players = new ArrayList<>();
			for (Entry<String, PlayerStats> ps : playerStats.entrySet()) {
				if (ps.getValue().lowScore == lowScore) {
					players.add(ps.getKey());
				}
			}
			return StringUtils.formatList(players);
		}

		public double getAverageWinningScore() {
			return winningScoreSum / winningScoreCount;
		}

		public List<Entry<String, PlayerStats>> getPlayerStats() {
			Set<Entry<String, PlayerStats>> set = playerStats.entrySet();
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
			Set<Entry<String, Integer>> set = playCountByLocation.entrySet();
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
			return 1 - Math.exp(-lambda * playCount);
		}

		public int calculateFhm() {
			return (int) ((personalRating * 5) + playCount + (4 * getMonthsPlayed()) + getHoursPlayed());
		}

		public int calculateHhm() {
			return (int) ((personalRating - 5) * getHoursPlayed());
		}

		public double calculateRuhm() {
			double raw = (((double) calculateFlash()) / calculateLag()) * getMonthsPlayed() * personalRating;
			if (raw == 0) {
				return 0;
			}
			return Math.log(raw);
		}

		public int getHIndexOffset() {
			int hIndex = PreferencesUtils.getHIndex(getActivity());
			if (playCount >= hIndex) {
				return -1;
			} else {
				return hIndex - playCount + 1;
			}
		}

		// public int getMonthsPerPlay() {
		// long days = calculateSpan();
		// int months = (int) (days / 365.25 * 12);
		// return months / playCount;
		// }

		public double calculateGrayHotness(int intervalPlayCount) {
			// http://matthew.gray.org/2005/10/games_16.html
			double S = 1 + (intervalPlayCount / playCount);
			// TODO: need to get HHM for the interval _only_
			return S * S * Math.sqrt(intervalPlayCount) * calculateHhm();
		}

		public int calculateWhitemoreScore() {
			// http://www.boardgamegeek.com/geeklist/37832/my-favorite-designers
			int score = (int) (personalRating * 2 - 13);
			if (score < 0) {
				return 0;
			}
			return score;
		}

		public double calculateZefquaaviusScore() {
			// http://boardgamegeek.com/user/zefquaavius
			double neutralRating = 5.5;
			double abs = (personalRating - neutralRating);
			double squared = abs * abs;
			if (personalRating < neutralRating) {
				squared *= -1;
			}
			return squared / 2.025;
		}

		public double calculateZefquaaviusHotness(int intervalPlayCount) {
			return calculateGrayHotness(intervalPlayCount) * calculateZefquaaviusScore();
		}

		private long calculateFlash() {
			return daysBetweenDates(firstPlayDate, lastPlayDate);
		}

		private long calculateLag() {
			return daysBetweenDates(lastPlayDate, null);
		}

		private long calculateSpan() {
			return daysBetweenDates(firstPlayDate, null);
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
		final int syncStatus;
		final List<PlayerModel> players = new ArrayList<>();

		PlayModel(Cursor cursor) {
			playId = cursor.getInt(PlayQuery.PLAY_ID);
			date = cursor.getString(PlayQuery.DATE);
			length = cursor.getInt(PlayQuery.LENGTH);
			quantity = cursor.getInt(PlayQuery.QUANTITY);
			incomplete = CursorUtils.getBoolean(cursor, PlayQuery.INCOMPLETE);
			playerCount = cursor.getInt(PlayQuery.PLAYER_COUNT);
			noWinStats = CursorUtils.getBoolean(cursor, PlayQuery.NO_WIN_STATS);
			location = cursor.getString(PlayQuery.LOCATION);
			syncStatus = cursor.getInt(PlayQuery.SYNC_STATUS);
			players.clear();
		}

		public List<PlayerModel> getPlayers() {
			return players;
		}

		public String getYear() {
			return date.substring(0, 4);
		}

		public String getYearAndMonth() {
			return date.substring(0, 7);
		}

		public void addPlayer(PlayerModel player) {
			players.add(player);
		}

		public boolean isWinnable() {
			if (noWinStats) {
				return false;
			}
			if (players == null || players.isEmpty()) {
				return false;
			}
			if (syncStatus == Play.SYNC_STATUS_PENDING_UPDATE) {
				return true;
			}
			if (playId > 0 && playId < Play.UNSYNCED_PLAY_ID && syncStatus != Play.SYNC_STATUS_PENDING_DELETE) {
				return true;
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
		String[] PROJECTION = { Plays._ID, Plays.PLAY_ID, Plays.DATE, Plays.ITEM_NAME, Plays.OBJECT_ID,
			Plays.LOCATION, Plays.QUANTITY, Plays.LENGTH, Plays.SYNC_STATUS, Plays.PLAYER_COUNT, Games.THUMBNAIL_URL,
			Plays.INCOMPLETE, Plays.NO_WIN_STATS, Plays.SYNC_STATUS };
		int PLAY_ID = 1;
		int DATE = 2;
		int LOCATION = 5;
		int QUANTITY = 6;
		int LENGTH = 7;
		int PLAYER_COUNT = 9;
		int INCOMPLETE = 11;
		int NO_WIN_STATS = 12;
		int SYNC_STATUS = 13;
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