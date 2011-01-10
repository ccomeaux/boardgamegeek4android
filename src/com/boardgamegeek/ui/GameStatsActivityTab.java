package com.boardgamegeek.ui;

import java.text.NumberFormat;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;

public class GameStatsActivityTab extends Activity implements
		AsyncQueryListener {

	private Uri mBoardgameUri;
	private NotifyingAsyncQueryHandler mHandler;
	private NumberFormat mFormat = NumberFormat.getInstance();

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
		setContentView(R.layout.boardgamestats);

		setUiVariables();

		final Intent intent = getIntent();
		mBoardgameUri = intent.getData();

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(mBoardgameUri, BoardgameQuery.PROJECTION);
	}

	private void setUiVariables() {
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
			if (!cursor.moveToFirst()) {
				return;
			}

			// Ratings
			setText(mRatingsCount, R.string.rating_count,
					cursor.getInt(BoardgameQuery.STATS_USERS_RATED));

			final double average = cursor
					.getDouble(BoardgameQuery.STATS_AVERAGE);
			setProgressBar(mAverageBar, average, 10.0);
			setText(mAverage, R.string.average_meter_text, average);

			final double bayesAverage = cursor
					.getDouble(BoardgameQuery.STATS_BAYES_AVERAGE);
			setProgressBar(mBayesAverageBar, bayesAverage, 10.0);
			setText(mBayesAverage, R.string.bayes_meter_text, bayesAverage);

			final double median = cursor.getDouble(BoardgameQuery.STATS_MEDIAN);
			if (median <= 0) {
				mMedianRow.setVisibility(View.GONE);
			} else {
				mMedianRow.setVisibility(View.VISIBLE);
				setProgressBar(mMedianBar, median, 10.0);
				setText(mMedian, R.string.median_meter_text, median);
			}

			final double stdDev = cursor
					.getDouble(BoardgameQuery.STATS_STANDARD_DEVIATION);
			setProgressBar(mStdDevBar, stdDev, 5.0);
			setText(mStdDev, R.string.stdDev_meter_text, stdDev);

			// Weight
			setText(mWeightCount, R.string.weight_count,
					cursor.getInt(BoardgameQuery.STATS_NUMBER_WEIGHTS));
			final double weight = cursor
					.getDouble(BoardgameQuery.STATS_AVERAGE_WEIGHT);
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
			int numRating = cursor.getInt(BoardgameQuery.STATS_USERS_RATED);
			int numOwned = cursor.getInt(BoardgameQuery.STATS_NUMBER_OWNED);
			int numTrading = cursor.getInt(BoardgameQuery.STATS_NUMBER_TRADING);
			int numWanting = cursor.getInt(BoardgameQuery.STATS_NUMBER_WANTING);
			int numWeights = cursor.getInt(BoardgameQuery.STATS_NUMBER_WEIGHTS);
			int numWishing = cursor.getInt(BoardgameQuery.STATS_NUMBER_WISHING);

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
		} finally {
			cursor.close();
		}
	}

	private void setText(TextView textView, int stringResourceId, int i) {
		textView.setText(String.format(
				getResources().getString(stringResourceId), mFormat.format(i)));
	}

	private void setText(TextView textView, int stringResourceId, double d) {
		textView.setText(String.format(
				getResources().getString(stringResourceId), mFormat.format(d)));
	}

	private void setProgressBar(ProgressBar progressBar, double progress,
			double max) {
		setProgressBar(progressBar, (int) (progress * 1000), (int) (max * 1000));
	}

	private void setProgressBar(ProgressBar progressBar, int progress, int max) {
		progressBar.setMax(max);
		progressBar.setProgress(progress);
	}

	private interface BoardgameQuery {
		String[] PROJECTION = { Games.STATS_USERS_RATED, Games.STATS_AVERAGE,
				Games.STATS_BAYES_AVERAGE, Games.STATS_MEDIAN,
				Games.STATS_STANDARD_DEVIATION, Games.STATS_NUMBER_WEIGHTS,
				Games.STATS_AVERAGE_WEIGHT, Games.STATS_NUMBER_COMMENTS,
				Games.STATS_NUMBER_OWNED, Games.STATS_NUMBER_TRADING,
				Games.STATS_NUMBER_WANTING, Games.STATS_NUMBER_WISHING, };

		int STATS_USERS_RATED = 0;
		int STATS_AVERAGE = 1;
		int STATS_BAYES_AVERAGE = 2;
		int STATS_MEDIAN = 3;
		int STATS_STANDARD_DEVIATION = 4;
		int STATS_NUMBER_WEIGHTS = 5;
		int STATS_AVERAGE_WEIGHT = 6;
		int STATS_NUMBER_COMMENTS = 7;
		int STATS_NUMBER_OWNED = 8;
		int STATS_NUMBER_TRADING = 9;
		int STATS_NUMBER_WANTING = 10;
		int STATS_NUMBER_WISHING = 11;
	}
}
