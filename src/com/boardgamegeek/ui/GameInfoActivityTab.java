package com.boardgamegeek.ui;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.StringUtils;

public class GameInfoActivityTab extends Activity implements AsyncQueryListener {
	private static final String TAG = "GameInfoActivityTab";

	private static final int TOKEN_GAME = 1;
	private static final int TOKEN_RANK = 2;
	private static final String SUBTYPE = "subtype";

	private NotifyingAsyncQueryHandler mHandler;
	private Uri mGameUri;
	private Uri mRankUri;
	private GameObserver mGameObserver;
	private RankObserver mRankObserver;

	private TextView mRatingView;
	private TextView mNumberRatingView;
	private TextView mRankView;
	private RatingBar mRatingBar;
	private TextView mYearPublishedView;
	private TextView mPlayersView;
	private TableRow mPlayingTimeRow;
	private TextView mPlayingTimeView;
	private TableRow mSuggestedAgesRow;
	private TextView mSuggestedAgesView;
	private TextView mIdView;
	private TextView mDescriptionView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tab_game_info);

		setUiVariables();
		setUrisAndObservers();

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(TOKEN_GAME, mGameUri, GameQuery.PROJECTION);
		mHandler.startQuery(TOKEN_RANK, mRankUri, RankQuery.PROJECTION);
	}

	@Override
	protected void onStart() {
		super.onStart();
		final ContentResolver cr = getContentResolver();
		cr.registerContentObserver(mGameUri, true, mGameObserver);
		cr.registerContentObserver(mGameUri, true, mRankObserver);
	}

	@Override
	protected void onStop() {
		super.onStop();
		final ContentResolver cr = getContentResolver();
		cr.unregisterContentObserver(mGameObserver);
		cr.unregisterContentObserver(mRankObserver);
	}

	private void setUrisAndObservers() {
		mGameUri = getIntent().getData();
		final int gameId = Games.getGameId(mGameUri);
		mRankUri = Games.buildRanksUri(gameId);
		mGameObserver = new GameObserver(null);
		mRankObserver = new RankObserver(null);
	}

	private void setUiVariables() {
		mRatingView = (TextView) findViewById(R.id.rating);
		mNumberRatingView = (TextView) findViewById(R.id.number_rating);
		mRankView = (TextView) findViewById(R.id.rank);
		mRatingBar = (RatingBar) findViewById(R.id.rating_bar);
		mYearPublishedView = (TextView) findViewById(R.id.yearPublished);
		mPlayersView = (TextView) findViewById(R.id.numOfPlayers);
		mPlayingTimeRow = (TableRow) findViewById(R.id.playing_time_row);
		mPlayingTimeView = (TextView) findViewById(R.id.playing_time);
		mSuggestedAgesRow = (TableRow) findViewById(R.id.suggested_ages_row);
		mSuggestedAgesView = (TextView) findViewById(R.id.suggested_ages);
		mIdView = (TextView) findViewById(R.id.gameId);
		mDescriptionView = (TextView) findViewById(R.id.description);
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (token == TOKEN_GAME) {
				if (!cursor.moveToFirst()) {
					return;
				}

				mRatingView.setText(getRating(cursor));
				mRatingBar.setRating((float) cursor.getDouble(GameQuery.STATS_AVERAGE));
				mNumberRatingView.setText(cursor.getInt(GameQuery.STATS_USERS_RATED) + " Ratings");
				mYearPublishedView.setText(getYearPublished(cursor));
				mPlayersView.setText(getPlayerDescription(cursor));

				int time = cursor.getInt(GameQuery.PLAYING_TIME);
				if (time == 0) {
					mPlayingTimeRow.setVisibility(View.GONE);
				} else {
					mPlayingTimeView.setText(getPlayingTime(time));
					mPlayingTimeRow.setVisibility(View.VISIBLE);
				}

				int age = cursor.getInt(GameQuery.MINIMUM_AGE);
				if (age == 0) {
					mSuggestedAgesRow.setVisibility(View.GONE);
				} else {
					mSuggestedAgesView.setText(getAge(age));
					mSuggestedAgesRow.setVisibility(View.VISIBLE);
				}

				mIdView.setText(cursor.getString(GameQuery.GAME_ID));
				mDescriptionView.setText(StringUtils.formatDescription(cursor.getString(GameQuery.DESCRIPTION)));
			} else if (token == TOKEN_RANK) {
				setRank(cursor);
			} else {
				Toast.makeText(this, "Unexpected onQueryComplete token: " + token, Toast.LENGTH_LONG).show();
			}
		} finally {
			cursor.close();
		}
	}

	private String getRating(Cursor cursor) {
		return new DecimalFormat("#0.00").format(cursor.getDouble(GameQuery.STATS_AVERAGE)) + " / 10";
	}

	private void setRank(Cursor cursor) {
		while (cursor.moveToNext()) {
			if (SUBTYPE.equals(cursor.getString(RankQuery.GAME_RANK_TYPE))) {
				final int rank = cursor.getInt(RankQuery.GAME_RANK_VALUE);
				if (rank == 0) {
					mRankView.setText(R.string.text_unknown);
				} else {
					mRankView.setText("" + rank);
				}
				break;
			}
		}
	}

	private String getYearPublished(Cursor cursor) {
		int year = cursor.getInt(GameQuery.YEAR_PUBLISHED);
		if (year == 0) {
			return getResources().getString(R.string.text_unknown);
		}
		return "" + year;
	}

	private String getPlayerDescription(Cursor cursor) {
		final int minPlayers = cursor.getInt(GameQuery.MIN_PLAYERS);
		final int maxPlayers = cursor.getInt(GameQuery.MAX_PLAYERS);

		if (minPlayers == 0 && maxPlayers == 0) {
			return getResources().getString(R.string.text_unknown);
		} else if (minPlayers >= maxPlayers) {
			return "" + minPlayers;
		} else {
			return "" + minPlayers + " - " + maxPlayers;
		}
	}

	private String getPlayingTime(int time) {
		if (time > 0) {
			return time + " " + getResources().getString(R.string.time_suffix);
		}
		return getResources().getString(R.string.text_unknown);
	}

	public String getAge(int age) {
		if (age > 0) {
			return age + " " + getResources().getString(R.string.age_suffix);
		}
		return getResources().getString(R.string.text_unknown);
	}

	private class GameObserver extends ContentObserver {

		public GameObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			Log.d(TAG, "Caught changed URI = " + mGameUri);
			mHandler.startQuery(TOKEN_GAME, mGameUri, GameQuery.PROJECTION);
		}
	}

	private class RankObserver extends ContentObserver {

		public RankObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			Log.d(TAG, "Caught changed URI = " + mRankUri);
			mHandler.startQuery(TOKEN_RANK, mRankUri, RankQuery.PROJECTION);
		}
	}

	private interface GameQuery {
		String[] PROJECTION = { Games.GAME_ID, Games.STATS_AVERAGE, Games.YEAR_PUBLISHED, Games.MIN_PLAYERS,
				Games.MAX_PLAYERS, Games.PLAYING_TIME, Games.MINIMUM_AGE, Games.DESCRIPTION, Games.STATS_USERS_RATED, };

		int GAME_ID = 0;
		int STATS_AVERAGE = 1;
		int YEAR_PUBLISHED = 2;
		int MIN_PLAYERS = 3;
		int MAX_PLAYERS = 4;
		int PLAYING_TIME = 5;
		int MINIMUM_AGE = 6;
		int DESCRIPTION = 7;
		int STATS_USERS_RATED = 8;
	}

	private interface RankQuery {
		String[] PROJECTION = { GameRanks.GAME_RANK_TYPE, GameRanks.GAME_RANK_VALUE, };

		int GAME_RANK_TYPE = 0;
		int GAME_RANK_VALUE = 1;
	}
}
