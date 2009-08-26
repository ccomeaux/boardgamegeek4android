package com.boardgamegeek;

import java.text.DecimalFormat;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

public class BoardGameStatsTab extends Activity {

	private DecimalFormat statFormat = new DecimalFormat("#0.000");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.boardgamestats);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateUI();
	}

	private void updateUI() {
		BoardGame boardGame = ViewBoardGame.boardGame;
		if (boardGame == null) {
			return;
		}

		// ratings
		setText(R.id.statsRank, R.string.rank,
				(boardGame.getRank() == 0) ? getResources().getString(
						R.string.not_available) : "" + boardGame.getRank());
		setText(R.id.statsRatingCount, R.string.rating_count, boardGame
				.getRatingCount());
		setProgressBar(R.id.averageBar, boardGame.getAverage(), 10.0);
		setText(R.id.averageText, R.string.average_meter_text, boardGame
				.getAverage());
		setProgressBar(R.id.bayesBar, boardGame.getBayesAverage(), 10.0);
		setText(R.id.bayesText, R.string.bayes_meter_text, boardGame
				.getBayesAverage());
		setProgressBar(R.id.medianBar, boardGame.getMedian(), 10.0);
		setText(R.id.medianText, R.string.median_meter_text, boardGame
				.getMedian());
		setProgressBar(R.id.stdDevBar, boardGame.getStandardDeviation(), 5.0);
		setText(R.id.stdDevText, R.string.stdDev_meter_text, boardGame
				.getStandardDeviation());

		// weight
		setText(R.id.statsWeightCount, R.string.weight_count, boardGame
				.getWeightCount());
		setProgressBar(R.id.weightBar, boardGame.getAverageWeight(), 5.0);
		setText(R.id.weightText, R.string.average_meter_text, boardGame
				.getAverageWeight());

		// users
		int max = Math.max(boardGame.getRatingCount(), boardGame
				.getOwnedCount());
		max = Math.max(max, boardGame.getTradingCount());
		max = Math.max(max, boardGame.getWantingCount());
		max = Math.max(max, boardGame.getWishingCount());
		max = Math.max(max, boardGame.getWeightCount());
		setText(R.id.usersCount, R.string.user_total, max);
		setProgressBar(R.id.owningBar, boardGame.getOwnedCount(), max);
		setText(R.id.owningText, R.string.owning_meter_text, boardGame
				.getOwnedCount());
		setProgressBar(R.id.ratingBar, boardGame.getRatingCount(), max);
		setText(R.id.ratingText, R.string.rating_meter_text, boardGame
				.getRatingCount());
		setProgressBar(R.id.tradingBar, boardGame.getTradingCount(), max);
		setText(R.id.tradingText, R.string.trading_meter_text, boardGame
				.getTradingCount());
		setProgressBar(R.id.wantingBar, boardGame.getWantingCount(), max);
		setText(R.id.wantingText, R.string.wanting_meter_text, boardGame
				.getWantingCount());
		setProgressBar(R.id.wishingBar, boardGame.getWishingCount(), max);
		setText(R.id.wishingText, R.string.wishing_meter_text, boardGame
				.getWishingCount());
		setProgressBar(R.id.weightingBar, boardGame.getWeightCount(), max);
		setText(R.id.weightingText, R.string.weighting_meter_text, boardGame
				.getWeightCount());
	}

	// HELPER METHODS

	private void setText(int textViewId, String text) {
		TextView textView = (TextView) findViewById(textViewId);
		textView.setText(text);
	}

	private void setText(int textViewId, int stringResourceId, String s) {
		setText(textViewId, String.format(getResources().getString(
				stringResourceId), s));
	}

	private void setText(int textViewId, int stringResourceId, int i) {
		setText(textViewId, String.format(getResources().getString(
				stringResourceId), i));
	}

	private void setText(int textViewId, int stringResourceId, double d) {
		setText(textViewId, String.format(getResources().getString(
				stringResourceId), statFormat.format(d)));
	}

	private void setProgressBar(int progressBarId, double progress, double max) {
		setProgressBar(progressBarId, (int) (progress * 1000),
				(int) (max * 1000));
	}

	private void setProgressBar(int progressBarId, int progress, int max) {
		ProgressBar progressBar = (ProgressBar) findViewById(progressBarId);
		progressBar.setMax(max);
		progressBar.setProgress(progress);
	}
}
