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
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.widget.StatBar;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;

public class GameStatsActivityTab extends Activity implements AsyncQueryListener {
	private static final String TAG = "GameStatsActivityTab";

	private static final int TOKEN_GAME = 1;
	private static final int TOKEN_RANK = 2;

	private Uri mBoardgameUri;
	private Uri mRankUri;
	private NotifyingAsyncQueryHandler mHandler;
	private GameObserver mGameObserver;
	private RankObserver mRankObserver;

	private NumberFormat mFormat = NumberFormat.getInstance();

	private LinearLayout mRankLayout;
	private int mRankIndex = 0;
	private float mRankTextSize;
	private int mRankPadding;

	private TextView mRatingsCount;
	private StatBar mAverageStatBar;
	private StatBar mBayesAverageBar;
	private StatBar mMedianBar;
	private StatBar mStdDevBar;

	private TextView mWeightCount;
	private StatBar mWeightBar;

	private TextView mUserCount;
	private StatBar mNumOwningBar;
	private StatBar mNumRatingBar;
	private StatBar mNumTradingBar;
	private StatBar mNumWantingBar;
	private StatBar mNumWishingBar;
	private StatBar mNumWeightingBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tab_game_stats);

		setUiVariables();

		mBoardgameUri = getIntent().getData();
		mRankUri = Games.buildRanksUri(Games.getGameId(mBoardgameUri));
		mGameObserver = new GameObserver(new Handler());
		mRankObserver = new RankObserver(new Handler());
	}

	@Override
	protected void onStart() {
		super.onStart();
		getContentResolver().registerContentObserver(mBoardgameUri, false, mGameObserver);
		getContentResolver().registerContentObserver(mRankUri, false, mRankObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		startGameQuery();
		startRankQuery();
	}

	@Override
	protected void onStop() {
		getContentResolver().unregisterContentObserver(mGameObserver);
		getContentResolver().unregisterContentObserver(mRankObserver);
		super.onStop();
	}

	private void setUiVariables() {
		mRankLayout = (LinearLayout) findViewById(R.id.rank_layout);
		mRankTextSize = getResources().getDimension(R.dimen.text_size_small);
		mRankPadding = (int) (getResources().getDimension(R.dimen.padding_small) / getResources().getDisplayMetrics().density);

		mRatingsCount = (TextView) findViewById(R.id.statsRatingCount);
		mAverageStatBar = (StatBar) findViewById(R.id.average_bar);
		mBayesAverageBar = (StatBar) findViewById(R.id.bayes_bar);
		mMedianBar = (StatBar) findViewById(R.id.median_bar);
		mStdDevBar = (StatBar) findViewById(R.id.stddev_bar);

		mWeightCount = (TextView) findViewById(R.id.statsWeightCount);
		mWeightBar = (StatBar) findViewById(R.id.weight_bar);

		mUserCount = (TextView) findViewById(R.id.usersCount);
		mNumOwningBar = (StatBar) findViewById(R.id.owning_bar);
		mNumRatingBar = (StatBar) findViewById(R.id.rating_bar);
		mNumTradingBar = (StatBar) findViewById(R.id.trading_bar);
		mNumWantingBar = (StatBar) findViewById(R.id.wanting_bar);
		mNumWishingBar = (StatBar) findViewById(R.id.wishing_bar);
		mNumWeightingBar = (StatBar) findViewById(R.id.weighting_bar);
	}

	private void startGameQuery() {
		if (mHandler == null) {
			mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		}
		mHandler.startQuery(TOKEN_GAME, mBoardgameUri, GameQuery.PROJECTION);
	}

	private void startRankQuery() {
		removeRankRows();
		if (mHandler == null) {
			mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		}
		mHandler.startQuery(TOKEN_RANK, null, mRankUri, RankQuery.PROJECTION, null, null, GameRanks.DEFAULT_SORT);
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (token == TOKEN_GAME) {
				if (!cursor.moveToFirst()) {
					return;
				}

				// Ratings
				mRatingsCount.setText(String.format(getResources().getString(R.string.rating_count),
						mFormat.format(cursor.getInt(GameQuery.STATS_USERS_RATED))));
				mAverageStatBar.setBar(R.string.average_meter_text, cursor.getDouble(GameQuery.STATS_AVERAGE));
				mBayesAverageBar.setBar(R.string.bayes_meter_text, cursor.getDouble(GameQuery.STATS_BAYES_AVERAGE));
				final double median = cursor.getDouble(GameQuery.STATS_MEDIAN);
				if (median <= 0) {
					mMedianBar.setVisibility(View.GONE);
				} else {
					mMedianBar.setVisibility(View.VISIBLE);
					mMedianBar.setBar(R.string.median_meter_text, median);
				}
				mStdDevBar
						.setBar(R.string.stdDev_meter_text, cursor.getDouble(GameQuery.STATS_STANDARD_DEVIATION), 5.0);

				// Weight
				mWeightCount.setText(String.format(getResources().getString(R.string.weight_count),
						mFormat.format(cursor.getInt(GameQuery.STATS_NUMBER_WEIGHTS))));
				final double weight = cursor.getDouble(GameQuery.STATS_AVERAGE_WEIGHT);
				int textId = R.string.weight_1_text;
				if (weight >= 4.5) {
					textId = R.string.weight_5_text;
				} else if (weight >= 3.5) {
					textId = R.string.weight_4_text;
				} else if (weight >= 2.5) {
					textId = R.string.weight_3_text;
				} else if (weight >= 1.5) {
					textId = R.string.weight_2_text;
				}
				mWeightBar.setBar(textId, weight, 5.0, 1.0);

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

				mUserCount.setText(String.format(getResources().getString(R.string.user_total), mFormat.format(max)));
				mNumOwningBar.setBar(R.string.owning_meter_text, numOwned, max);
				mNumRatingBar.setBar(R.string.rating_meter_text, numRating, max);
				mNumTradingBar.setBar(R.string.trading_meter_text, numTrading, max);
				mNumWantingBar.setBar(R.string.wanting_meter_text, numWanting, max);
				mNumWishingBar.setBar(R.string.wishing_meter_text, numWishing, max);
				mNumWeightingBar.setBar(R.string.weighting_meter_text, numWeights, max);
			} else if (token == TOKEN_RANK) {
				while (cursor.moveToNext()) {
					String name = cursor.getString(RankQuery.GAME_RANK_FRIENDLY_NAME);
					int rank = cursor.getInt(RankQuery.GAME_RANK_VALUE);
					double rating = cursor.getDouble(RankQuery.GAME_RANK_BAYES_AVERAGE);
					String type = cursor.getString(RankQuery.GAME_RANK_TYPE);
					addRankRow(name, rank, "subtype".equals(type), rating);
				}
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private void addRankRow(String label, int rank, boolean bold, double rating) {
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
		String rankText = (rank == 0) ? getResources().getString(R.string.text_not_available) : String.valueOf(rank);
		setText(tv, rankText, bold);
		layout.addView(tv);

		StatBar sb = new StatBar(this);
		sb.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.FILL_PARENT));
		sb.setPadding(mRankPadding, mRankPadding, mRankPadding, mRankPadding);
		sb.setBar(R.string.average_meter_text, rating);

		mRankLayout.addView(layout, mRankIndex++);
		mRankLayout.addView(sb, mRankIndex++);
	}

	private void removeRankRows() {
		mRankLayout.removeAllViews();
		mRankIndex = 0;
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
			super.onChange(selfChange);
			Log.d(TAG, "Caught changed URI = " + mBoardgameUri);
			startGameQuery();
		}
	}

	class RankObserver extends ContentObserver {
		public RankObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			Log.d(TAG, "Caught changed URI = " + mBoardgameUri);
			startRankQuery();
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
		String[] PROJECTION = { GameRanks.GAME_RANK_FRIENDLY_NAME, GameRanks.GAME_RANK_VALUE, GameRanks.GAME_RANK_TYPE,
				GameRanks.GAME_RANK_BAYES_AVERAGE };

		int GAME_RANK_FRIENDLY_NAME = 0;
		int GAME_RANK_VALUE = 1;
		int GAME_RANK_TYPE = 2;
		int GAME_RANK_BAYES_AVERAGE = 3;
	}
}
