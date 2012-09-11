package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class GameInfoActivityTab extends Activity implements AsyncQueryListener {
	private static final String TAG = makeLogTag(GameInfoActivityTab.class);

	private static final int TOKEN_GAME = 1;
	private static final int TOKEN_RANK = 2;
	private static final String SUBTYPE = "subtype";

	private NotifyingAsyncQueryHandler mHandler;
	private int mGameId;
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
	private TextView mPlayingTimeView;
	private TextView mSuggestedAgesView;
	private TextView mIdView;
	private TextView mUpdatedView;
	private TextView mDescriptionView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tab_game_info);

		setUiVariables();
		setUrisAndObservers();
	}

	private void startGameQuery() {
		if (mHandler == null) {
			mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		}
		mHandler.startQuery(TOKEN_GAME, mGameUri, GameQuery.PROJECTION);
	}

	private void startRankQuery() {
		if (mHandler == null) {
			mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		}
		mHandler.startQuery(TOKEN_RANK, mRankUri, RankQuery.PROJECTION);
	}

	@Override
	protected void onStart() {
		super.onStart();
		ContentResolver cr = getContentResolver();
		cr.registerContentObserver(mGameUri, false, mGameObserver);
		cr.registerContentObserver(mRankUri, false, mRankObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		startGameQuery();
		startRankQuery();
	}

	@Override
	protected void onStop() {
		ContentResolver cr = getContentResolver();
		cr.unregisterContentObserver(mGameObserver);
		cr.unregisterContentObserver(mRankObserver);
		super.onStop();
	}

	private void setUrisAndObservers() {
		mGameUri = getIntent().getData();
		mGameId = Games.getGameId(mGameUri);
		mRankUri = Games.buildRanksUri(mGameId);
		mGameObserver = new GameObserver(new Handler());
		mRankObserver = new RankObserver(new Handler());
	}

	private void setUiVariables() {
		mRatingView = (TextView) findViewById(R.id.rating);
		mNumberRatingView = (TextView) findViewById(R.id.number_rating);
		mRankView = (TextView) findViewById(R.id.rank);
		mRatingBar = (RatingBar) findViewById(R.id.rating_stars);
		mYearPublishedView = (TextView) findViewById(R.id.year_published);
		mPlayersView = (TextView) findViewById(R.id.num_of_players);
		mPlayingTimeView = (TextView) findViewById(R.id.playing_time);
		mSuggestedAgesView = (TextView) findViewById(R.id.suggested_ages);
		mIdView = (TextView) findViewById(R.id.game_id);
		mUpdatedView = (TextView) findViewById(R.id.updated);
		mDescriptionView = (TextView) findViewById(R.id.description);
	}

	public void onClick(View v) {
		Intent intent = new Intent(this, PollActivity.class);
		intent.putExtra(PollActivity.KEY_GAME_ID, mGameId);
		switch (v.getId()) {
			case R.id.languges_button:
				intent.putExtra(PollActivity.KEY_TYPE, PollActivity.LANGUAGE_DEPENDENCE);
				break;
			case R.id.suggested_ages_button:
				intent.putExtra(PollActivity.KEY_TYPE, PollActivity.SUGGESTED_PLAYERAGE);
				break;
			case R.id.num_of_players_button:
				intent.putExtra(PollActivity.KEY_TYPE, PollActivity.SUGGESTED_NUMPLAYERS);
				break;
			default:
				LOGW(TAG, "Unexpected button: " + v.getId());
				return;
		}
		startActivity(intent);
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
				mPlayingTimeView.setText(getPlayingTime(cursor.getInt(GameQuery.PLAYING_TIME)));
				mPlayersView.setText(getPlayerDescription(cursor));
				mSuggestedAgesView.setText(getAge(cursor.getInt(GameQuery.MINIMUM_AGE)));
				UIUtils.setTextMaybeHtml(mDescriptionView, cursor.getString(GameQuery.DESCRIPTION));

				mIdView.setText(String.format(getResources().getString(R.string.id_list_text),
					cursor.getString(GameQuery.GAME_ID)));

				long updated = cursor.getLong(GameQuery.UPDATED);
				if (updated == 0) {
					mUpdatedView.setVisibility(View.GONE);
				} else {
					mUpdatedView.setVisibility(View.VISIBLE);
					CharSequence u = DateUtils.getRelativeTimeSpanString(updated, System.currentTimeMillis(),
						DateUtils.MINUTE_IN_MILLIS);
					mUpdatedView.setText(getResources().getString(R.string.updated) + " " + u);
				}
			} else if (token == TOKEN_RANK) {
				setRank(cursor);
			} else {
				Toast.makeText(this, "Unexpected onQueryComplete token: " + token, Toast.LENGTH_LONG).show();
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
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
					mRankView.setText(R.string.text_not_available);
				} else {
					mRankView.setText(String.valueOf(rank));
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
		return String.valueOf(year);
	}

	private String getPlayerDescription(Cursor cursor) {
		final int minPlayers = cursor.getInt(GameQuery.MIN_PLAYERS);
		final int maxPlayers = cursor.getInt(GameQuery.MAX_PLAYERS);

		if (minPlayers == 0 && maxPlayers == 0) {
			return getResources().getString(R.string.text_unknown);
		} else if (minPlayers >= maxPlayers) {
			return String.valueOf(minPlayers);
		} else {
			return String.valueOf(minPlayers) + " - " + String.valueOf(maxPlayers);
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
			super.onChange(selfChange);
			LOGD(TAG, "Caught changed URI = " + mGameUri);
			startGameQuery();
		}
	}

	private class RankObserver extends ContentObserver {
		public RankObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			LOGD(TAG, "Caught changed URI = " + mRankUri);
			startRankQuery();
		}
	}

	private interface GameQuery {
		String[] PROJECTION = { Games.GAME_ID, Games.STATS_AVERAGE, Games.YEAR_PUBLISHED, Games.MIN_PLAYERS,
			Games.MAX_PLAYERS, Games.PLAYING_TIME, Games.MINIMUM_AGE, Games.DESCRIPTION, Games.STATS_USERS_RATED,
			Games.UPDATED };

		int GAME_ID = 0;
		int STATS_AVERAGE = 1;
		int YEAR_PUBLISHED = 2;
		int MIN_PLAYERS = 3;
		int MAX_PLAYERS = 4;
		int PLAYING_TIME = 5;
		int MINIMUM_AGE = 6;
		int DESCRIPTION = 7;
		int STATS_USERS_RATED = 8;
		int UPDATED = 9;
	}

	private interface RankQuery {
		String[] PROJECTION = { GameRanks.GAME_RANK_TYPE, GameRanks.GAME_RANK_VALUE, };

		int GAME_RANK_TYPE = 0;
		int GAME_RANK_VALUE = 1;
	}
}
