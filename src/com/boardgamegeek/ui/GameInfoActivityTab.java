package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TableRow;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.StringUtils;

public class GameInfoActivityTab extends Activity implements AsyncQueryListener {
	
	private Uri mBoardgameUri;
	private NotifyingAsyncQueryHandler mHandler;

	private TextView mRankView;
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
		
		final Intent intent = getIntent();
		mBoardgameUri = intent.getData();
	
		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(mBoardgameUri, BoardgameQuery.PROJECTION);
	}
	
	private void setUiVariables() {
		mRankView = (TextView) findViewById(R.id.rank);
		mYearPublishedView = (TextView) findViewById(R.id.yearPublished);
		mPlayersView = (TextView) findViewById(R.id.numOfPlayers);
		mPlayingTimeRow = (TableRow) findViewById(R.id.playingTimeRow);
		mPlayingTimeView = (TextView) findViewById(R.id.playingTime);
		mSuggestedAgesRow = (TableRow) findViewById(R.id.suggestedAgesRow);
		mSuggestedAgesView = (TextView) findViewById(R.id.suggestedAges);
		mIdView = (TextView) findViewById(R.id.gameId);
		mDescriptionView = (TextView) findViewById(R.id.description);
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (!cursor.moveToFirst())
			{
				return;
			}
			
			mRankView.setText("?");
			mYearPublishedView.setText(cursor.getString(BoardgameQuery.YEAR_PUBLISHED));
			mPlayersView.setText(getPlayerDescription(cursor));
			
			int time = cursor.getInt(BoardgameQuery.PLAYING_TIME);
			if (time == 0) {
				mPlayingTimeRow.setVisibility(View.GONE);
			} else {
				mPlayingTimeView.setText(getPlayingTime(time));
				mPlayingTimeRow.setVisibility(View.VISIBLE);
			}
			
			int age = cursor.getInt(BoardgameQuery.MINIMUM_AGE);
			if (age == 0) {
				mSuggestedAgesRow.setVisibility(View.GONE);
			} else {
				mSuggestedAgesView.setText(getAge(age));
				mSuggestedAgesRow.setVisibility(View.VISIBLE);
			}
			
			mIdView.setText(cursor.getString(BoardgameQuery.GAME_ID));
			mDescriptionView.setText(StringUtils.unescapeHtml(cursor.getString(BoardgameQuery.DESCRIPTION)));
		} finally {
			cursor.close();
		}
	}
	
	private String getPlayerDescription(Cursor cursor) {
		final int minPlayers = cursor.getInt(BoardgameQuery.MIN_PLAYERS);
		final int maxPlayers = cursor.getInt(BoardgameQuery.MAX_PLAYERS);

		if (minPlayers == 0 && maxPlayers == 0) {
			return "?";
		} else if (minPlayers >= maxPlayers) {
			return "" + minPlayers;
		} else {
			return "" + minPlayers + " - " + maxPlayers;
		}
	}

	private String getPlayingTime(int time) {
		if (time > 0) {
			return time + " minutes";
		}
		return "?";
	}

	public String getAge(int age) {
		if (age > 0) {
			return age + " and up";
		}
		return "?";
	}

	private interface BoardgameQuery {
		String[] PROJECTION = {
			Games.GAME_ID,
			Games.YEAR_PUBLISHED,
			Games.MIN_PLAYERS,
			Games.MAX_PLAYERS,
			Games.PLAYING_TIME,
			Games.MINIMUM_AGE,
			Games.DESCRIPTION,
		};

		int GAME_ID = 0;
		int YEAR_PUBLISHED = 1;
		int MIN_PLAYERS = 2;
		int MAX_PLAYERS = 3;
		int PLAYING_TIME = 4;
		int MINIMUM_AGE = 5;
		int DESCRIPTION = 6;
	}
}
