package com.boardgamegeek.view;

import java.text.NumberFormat;

import com.boardgamegeek.R;
import com.boardgamegeek.model.BoardGame;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class BoardGameStatsTab extends Activity {

	private NumberFormat format = NumberFormat.getInstance();
	private int mRankIndex = 0;

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
		BoardGame boardGame = BoardGameView.boardGame;
		if (boardGame == null) {
			return;
		}

		// ranks
		if (mRankIndex == 0) {
			addRankRow(R.string.rank_abstract, boardGame.getRankAbstract());
			addRankRow(R.string.rank_kids, boardGame.getRankKids());
			addRankRow(R.string.rank_collectible, boardGame.getRankCcg());
			addRankRow(R.string.rank_family, boardGame.getRankFamily());
			addRankRow(R.string.rank_party, boardGame.getRankParty());
			addRankRow(R.string.rank_strategy, boardGame.getRankStrategy());
			addRankRow(R.string.rank_theme, boardGame.getRankTheme());
			addRankRow(R.string.rank_war, boardGame.getRankWar());
		}

		// ratings
		setText(R.id.statsRank, (boardGame.getRank() == 0) ? getResources().getString(R.string.not_available)
			: "" + boardGame.getRank());
		setText(R.id.statsRatingCount, R.string.rating_count, boardGame.getRatingCount());
		setProgressBar(R.id.averageBar, boardGame.getAverage(), 10.0);
		setText(R.id.averageText, R.string.average_meter_text, boardGame.getAverage());
		setProgressBar(R.id.bayesBar, boardGame.getBayesAverage(), 10.0);
		setText(R.id.bayesText, R.string.bayes_meter_text, boardGame.getBayesAverage());
		if (boardGame.getMedian() == 0) {
			((RelativeLayout) findViewById(R.id.medianRow)).setVisibility(View.GONE);
		} else {
			setProgressBar(R.id.medianBar, boardGame.getMedian(), 10.0);
			setText(R.id.medianText, R.string.median_meter_text, boardGame.getMedian());
		}
		setProgressBar(R.id.stdDevBar, boardGame.getStandardDeviation(), 5.0);
		setText(R.id.stdDevText, R.string.stdDev_meter_text, boardGame.getStandardDeviation());

		// weight
		setText(R.id.statsWeightCount, R.string.weight_count, boardGame.getWeightCount());
		setProgressBar(R.id.weightBar, boardGame.getAverageWeight() - 1, 4.0);
		if (boardGame.getAverageWeight() >= 4.5) {
			setText(R.id.weightText, R.string.weight_5_text, boardGame.getAverageWeight());
		} else if (boardGame.getAverageWeight() >= 3.5) {
			setText(R.id.weightText, R.string.weight_4_text, boardGame.getAverageWeight());
		} else if (boardGame.getAverageWeight() >= 2.5) {
			setText(R.id.weightText, R.string.weight_3_text, boardGame.getAverageWeight());
		} else if (boardGame.getAverageWeight() >= 1.5) {
			setText(R.id.weightText, R.string.weight_2_text, boardGame.getAverageWeight());
		} else {
			setText(R.id.weightText, R.string.weight_1_text, boardGame.getAverageWeight());
		}

		// users
		int max = Math.max(boardGame.getRatingCount(), boardGame.getOwnedCount());
		max = Math.max(max, boardGame.getTradingCount());
		max = Math.max(max, boardGame.getWantingCount());
		max = Math.max(max, boardGame.getWishingCount());
		max = Math.max(max, boardGame.getWeightCount());
		setText(R.id.usersCount, R.string.user_total, max);
		setProgressBar(R.id.owningBar, boardGame.getOwnedCount(), max);
		setText(R.id.owningText, R.string.owning_meter_text, boardGame.getOwnedCount());
		setProgressBar(R.id.ratingBar, boardGame.getRatingCount(), max);
		setText(R.id.ratingText, R.string.rating_meter_text, boardGame.getRatingCount());
		setProgressBar(R.id.tradingBar, boardGame.getTradingCount(), max);
		setText(R.id.tradingText, R.string.trading_meter_text, boardGame.getTradingCount());
		setProgressBar(R.id.wantingBar, boardGame.getWantingCount(), max);
		setText(R.id.wantingText, R.string.wanting_meter_text, boardGame.getWantingCount());
		setProgressBar(R.id.wishingBar, boardGame.getWishingCount(), max);
		setText(R.id.wishingText, R.string.wishing_meter_text, boardGame.getWishingCount());
		setProgressBar(R.id.weightingBar, boardGame.getWeightCount(), max);
		setText(R.id.weightingText, R.string.weighting_meter_text, boardGame.getWeightCount());
	}

	private void addRankRow(int labelResource, int rank) {
		if (rank > 0) {
			LinearLayout layout = new LinearLayout(this);
			layout.setOrientation(LinearLayout.HORIZONTAL);
			layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

			TextView tv = new TextView(this);
			tv.setText(labelResource);
			tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
			tv.setTextAppearance(this, android.R.style.TextAppearance_Small);
			layout.addView(tv);

			tv = new TextView(this);
			tv.setText("" + rank);
			tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
			tv.setTextAppearance(this, android.R.style.TextAppearance_Small);
			tv.setGravity(Gravity.RIGHT);
			layout.addView(tv);

			LinearLayout ll = (LinearLayout) findViewById(R.id.rankLayout);
			ll.addView(layout, ++mRankIndex);
		}
	}

	// HELPER METHODS

	private void setText(int textViewId, String text) {
		TextView textView = (TextView) findViewById(textViewId);
		textView.setText(text);
	}

	private void setText(int textViewId, int stringResourceId, int i) {
		setText(textViewId, String.format(getResources().getString(stringResourceId), format.format(i)));
	}

	private void setText(int textViewId, int stringResourceId, double d) {
		setText(textViewId, String.format(getResources().getString(stringResourceId), format.format(d)));
	}

	private void setProgressBar(int progressBarId, double progress, double max) {
		setProgressBar(progressBarId, (int) (progress * 1000), (int) (max * 1000));
	}

	private void setProgressBar(int progressBarId, int progress, int max) {
		ProgressBar progressBar = (ProgressBar) findViewById(progressBarId);
		progressBar.setMax(max);
		progressBar.setProgress(progress);
	}
}
