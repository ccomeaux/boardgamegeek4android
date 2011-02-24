package com.boardgamegeek.ui;

import java.text.NumberFormat;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;

public class GameStatsActivityTab extends Activity implements AsyncQueryListener {
	private static final String TAG = "GameStatsActivityTab";

	private static final int TOKEN_GAME = 1;
	private static final int TOKEN_RANK = 2;

	private Uri mBoardgameUri;
	private Uri mRankUri;
	private NotifyingAsyncQueryHandler mHandler;
	private NumberFormat mFormat = NumberFormat.getInstance();
	private int mRankIndex = 0;
	private float mRankTextSize;

	private TextView mRatingsCount;
	private ProgressBar mAverageBar;
	private TextView mAverage;
	private ProgressBar mBayesAverageBar;
	private TextView mBayesAverage;
	private ProgressBar mMedianBar;
	private TextView mMedian;
	private ProgressBar mStdDevBar;
	private TextView mStdDev;
	private RelativeLayout mMedianRow;

	private TextView mWeightCount;
	private TextView mWeightText;
	private ProgressBar mWeightBar;

	private TextView mUserCount;
	private ProgressBar mNumRatingBar;
	private TextView mNumRating;
	private ProgressBar mNumOwningBar;
	private TextView mNumOwning;
	private ProgressBar mNumTradingBar;
	private TextView mNumTrading;
	private ProgressBar mNumWantingBar;
	private TextView mNumWanting;
	private ProgressBar mNumWeightsBar;
	private TextView mNumWeights;
	private ProgressBar mNumWishingBar;
	private TextView mNumWishing;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tab_game_stats);

		setUiVariables();

		mBoardgameUri = getIntent().getData();
		mRankUri = Games.buildRanksUri(Games.getGameId(mBoardgameUri));
		getContentResolver().registerContentObserver(mBoardgameUri, true, new GameObserver(null));

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(TOKEN_GAME, mBoardgameUri, GameQuery.PROJECTION);
		mHandler.startQuery(TOKEN_RANK, null, mRankUri, RankQuery.PROJECTION, null, null, GameRanks.DEFAULT_SORT);
	}

	private void setUiVariables() {
		mRankTextSize = getResources().getDimension(R.dimen.text_size_small);
		mRatingsCount = (TextView) findViewById(R.id.statsRatingCount);
		mAverageBar = (ProgressBar) findViewById(R.id.averageBar);
		mAverage = (TextView) findViewById(R.id.averageText);
		mBayesAverageBar = (ProgressBar) findViewById(R.id.bayesBar);
		mBayesAverage = (TextView) findViewById(R.id.bayesText);
		mMedianBar = (ProgressBar) findViewById(R.id.medianBar);
		mMedian = (TextView) findViewById(R.id.medianText);
		mMedianRow = (RelativeLayout) findViewById(R.id.medianRow);
		mStdDevBar = (ProgressBar) findViewById(R.id.stdDevBar);
		mStdDev = (TextView) findViewById(R.id.stdDevText);

		mWeightCount = (TextView) findViewById(R.id.statsWeightCount);
		mWeightText = (TextView) findViewById(R.id.weightText);
		mWeightBar = (ProgressBar) findViewById(R.id.weightBar);

		mUserCount = (TextView) findViewById(R.id.usersCount);
		mNumOwning = (TextView) findViewById(R.id.owningText);
		mNumRating = (TextView) findViewById(R.id.ratingText);
		mNumTrading = (TextView) findViewById(R.id.tradingText);
		mNumWanting = (TextView) findViewById(R.id.wantingText);
		mNumWeights = (TextView) findViewById(R.id.weightingText);
		mNumWishing = (TextView) findViewById(R.id.wishingText);
		mNumOwningBar = (ProgressBar) findViewById(R.id.owningBar);
		mNumRatingBar = (ProgressBar) findViewById(R.id.ratingBar);
		mNumTradingBar = (ProgressBar) findViewById(R.id.tradingBar);
		mNumWantingBar = (ProgressBar) findViewById(R.id.wantingBar);
		mNumWishingBar = (ProgressBar) findViewById(R.id.wishingBar);
		mNumWeightsBar = (ProgressBar) findViewById(R.id.weightingBar);
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (token == TOKEN_GAME) {
				if (!cursor.moveToFirst()) {
					return;
				}

				// Ratings
				setText(mRatingsCount, R.string.rating_count, cursor.getInt(GameQuery.STATS_USERS_RATED));

				final double average = cursor.getDouble(GameQuery.STATS_AVERAGE);
				setProgressBar(mAverageBar, average, 10.0);
				setText(mAverage, R.string.average_meter_text, average);

				final double bayesAverage = cursor.getDouble(GameQuery.STATS_BAYES_AVERAGE);
				setProgressBar(mBayesAverageBar, bayesAverage, 10.0);
				setText(mBayesAverage, R.string.bayes_meter_text, bayesAverage);

				final double median = cursor.getDouble(GameQuery.STATS_MEDIAN);
				if (median <= 0) {
					mMedianRow.setVisibility(View.GONE);
				} else {
					mMedianRow.setVisibility(View.VISIBLE);
					setProgressBar(mMedianBar, median, 10.0);
					setText(mMedian, R.string.median_meter_text, median);
				}

				final double stdDev = cursor.getDouble(GameQuery.STATS_STANDARD_DEVIATION);
				setProgressBar(mStdDevBar, stdDev, 5.0);
				setText(mStdDev, R.string.stdDev_meter_text, stdDev);

				// Weight
				setText(mWeightCount, R.string.weight_count, cursor.getInt(GameQuery.STATS_NUMBER_WEIGHTS));
				final double weight = cursor.getDouble(GameQuery.STATS_AVERAGE_WEIGHT);
				setProgressBar(mWeightBar, weight - 1, 4.0);
				if (weight >= 4.5) {
					setText(mWeightText, R.string.weight_5_text, weight);
				} else if (weight >= 3.5) {
					setText(mWeightText, R.string.weight_4_text, weight);
				} else if (weight >= 2.5) {
					setText(mWeightText, R.string.weight_3_text, weight);
				} else if (weight >= 1.5) {
					setText(mWeightText, R.string.weight_2_text, weight);
				} else {
					setText(mWeightText, R.string.weight_1_text, weight);
				}

				// users
				int numRating = cursor.getInt(GameQuery.STATS_USERS_RATED);
				int numOwned = cursor.getInt(GameQuery.STATS_NUMBER_OWNED);
				int numTrading = cursor.getInt(GameQuery.STATS_NUMBER_TRADING);
				int numWanting = cursor.getInt(GameQuery.STATS_NUMBER_WANTING);
				int numWeights = cursor.getInt(GameQuery.STATS_NUMBER_WEIGHTS);
				int numWishing = cursor.getInt(GameQuery.STATS_NUMBER_WISHING);

				int max = Math.max(numRating, numOwned);
				max = Math.max(max, numTrading);
				max = Math.max(max, numWanting);
				max = Math.max(max, numWeights);
				max = Math.max(max, numWishing);

				setText(mUserCount, R.string.user_total, max);
				setProgressBar(mNumOwningBar, numOwned, max);
				setText(mNumOwning, R.string.owning_meter_text, numOwned);
				setProgressBar(mNumRatingBar, numRating, max);
				setText(mNumRating, R.string.rating_meter_text, numRating);
				setProgressBar(mNumTradingBar, numTrading, max);
				setText(mNumTrading, R.string.trading_meter_text, numTrading);
				setProgressBar(mNumWantingBar, numWanting, max);
				setText(mNumWanting, R.string.wanting_meter_text, numWanting);
				setProgressBar(mNumWishingBar, numWishing, max);
				setText(mNumWishing, R.string.wishing_meter_text, numWishing);
				setProgressBar(mNumWeightsBar, numWeights, max);
				setText(mNumWeights, R.string.weighting_meter_text, numWeights);
			} else if (token == TOKEN_RANK) {
				while (cursor.moveToNext()) {
					String name = cursor.getString(RankQuery.GAME_RANK_FRIENDLY_NAME);
					int rank = cursor.getInt(RankQuery.GAME_RANK_VALUE);
					String type = cursor.getString(RankQuery.GAME_RANK_TYPE);
					addRankRow(name, rank, "subtype".equals(type));
				}
			}
		} finally {
			cursor.close();
		}
	}

	private void setText(TextView textView, int stringResourceId, int i) {
		textView.setText(String.format(getResources().getString(stringResourceId), mFormat.format(i)));
	}

	private void setText(TextView textView, int stringResourceId, double d) {
		textView.setText(String.format(getResources().getString(stringResourceId), mFormat.format(d)));
	}

	private void setProgressBar(ProgressBar progressBar, double progress, double max) {
		setProgressBar(progressBar, (int) (progress * 1000), (int) (max * 1000));
	}

	private void setProgressBar(ProgressBar progressBar, int progress, int max) {
		progressBar.setMax(max);
		progressBar.setProgress(progress);
	}

	private void addRankRow(String label, int rank, boolean bold) {
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		TextView tv = new TextView(this, null, R.style.StatsHeading);
		tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, mRankTextSize);
		setText(tv, label, bold);
		layout.addView(tv);

		tv = new TextView(this);
		tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		tv.setTextAppearance(this, android.R.style.TextAppearance_Small);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, mRankTextSize);
		tv.setGravity(Gravity.RIGHT);
		String rankText = (rank == 0) ? getResources().getString(R.string.text_unknown) : "" + rank;
		setText(tv, rankText, bold);
		layout.addView(tv);

		LinearLayout ll = (LinearLayout) findViewById(R.id.rankLayout);
		ll.addView(layout, ++mRankIndex);
	}

	private void setText(TextView tv, String text, boolean bold) {
		if (bold) {
			SpannableString ss = new SpannableString(text);
			ss.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			tv.setText(ss);
		} else {
			tv.setText(text);
		}
	}

	class GameObserver extends ContentObserver {

		public GameObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			Log.d(TAG, "Caught changed URI = " + mBoardgameUri);
			mHandler.startQuery(mBoardgameUri, GameQuery.PROJECTION);
		}
	}

	private interface GameQuery {
		String[] PROJECTION = { Games.STATS_USERS_RATED, Games.STATS_AVERAGE, Games.STATS_BAYES_AVERAGE,
				Games.STATS_MEDIAN, Games.STATS_STANDARD_DEVIATION, Games.STATS_NUMBER_WEIGHTS,
				Games.STATS_AVERAGE_WEIGHT, Games.STATS_NUMBER_COMMENTS, Games.STATS_NUMBER_OWNED,
				Games.STATS_NUMBER_TRADING, Games.STATS_NUMBER_WANTING, Games.STATS_NUMBER_WISHING };

		int STATS_USERS_RATED = 0;
		int STATS_AVERAGE = 1;
		int STATS_BAYES_AVERAGE = 2;
		int STATS_MEDIAN = 3;
		int STATS_STANDARD_DEVIATION = 4;
		int STATS_NUMBER_WEIGHTS = 5;
		int STATS_AVERAGE_WEIGHT = 6;
		// int STATS_NUMBER_COMMENTS = 7;
		int STATS_NUMBER_OWNED = 8;
		int STATS_NUMBER_TRADING = 9;
		int STATS_NUMBER_WANTING = 10;
		int STATS_NUMBER_WISHING = 11;
	}

	private interface RankQuery {
		String[] PROJECTION = { GameRanks.GAME_RANK_FRIENDLY_NAME, GameRanks.GAME_RANK_VALUE, GameRanks.GAME_RANK_TYPE };

		int GAME_RANK_FRIENDLY_NAME = 0;
		int GAME_RANK_VALUE = 1;
		int GAME_RANK_TYPE = 2;
	}
}
