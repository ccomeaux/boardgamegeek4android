package com.boardgamegeek.ui;

import android.app.TabActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TableRow;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class BoardgameActivity extends TabActivity implements AsyncQueryListener {
	// private final static String TAG = "BoardgameActivity";

	private Uri mBoardgameUri;
	private NotifyingAsyncQueryHandler mHandler;

	private int mId;
	private String mName;

	private TextView mNameView;
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
		setContentView(R.layout.activity_boardgame);
		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);

		setUiVariables();
		setupMainTab();

		final Intent intent = getIntent();
		mBoardgameUri = intent.getData();

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(mBoardgameUri, BoardgameQuery.PROJECTION);
	}

	private void setUiVariables() {
		mNameView = (TextView) findViewById(R.id.game_name);
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
				return;

			mId = cursor.getInt(BoardgameQuery.GAME_ID);
			mName = cursor.getString(BoardgameQuery.GAME_NAME);

			populateUi(cursor);
						
			// final String url = cursor.getString(BuddiesQuery.AVATAR_URL);
			// new BuddyAvatarTask().execute(url);

		} finally {
			cursor.close();
		}
	}

	private void populateUi(Cursor cursor) {
		mNameView.setText(mName);
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
		
		mIdView.setText("" + mId);
		mDescriptionView.setText(StringUtils.unescapeHtml(cursor.getString(BoardgameQuery.DESCRIPTION)));
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

	@Override
	public void setTitle(CharSequence title) {
		UIUtils.setTitle(this, title);
	}

	public void onHomeClick(View v) {
		UIUtils.resetToHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	private void setupMainTab() {
		final TabHost host = getTabHost();

		// Summary content comes from existing layout
		host.addTab(host.newTabSpec("main").setIndicator(getResources().getString(R.string.main_tab_title),
			getResources().getDrawable(R.drawable.ic_tab_main)).setContent(R.id.mainTab));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		final MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.boardgame, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// only allow logging a play once the game is populated
		MenuItem mi = menu.findItem(R.id.log_play);
		mi.setEnabled(!TextUtils.isEmpty(mName));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.log_play:
				logPlay(false);
				return true;
			case R.id.log_play_quick:
				logPlay(true);
				return true;
		}
		return false;
	}

	private void logPlay(boolean quick) {
		Intent intent = new Intent(this, LogPlayActivity.class);
		intent.setAction(quick ? Intent.ACTION_VIEW : Intent.ACTION_EDIT);
		intent.putExtra("GAME_ID", mId);
		intent.putExtra("GAME_NAME", mName);
		startActivity(intent);
	}

	private interface BoardgameQuery {
		String[] PROJECTION = {
			Games._ID,
			Games.GAME_NAME,
			Games.GAME_ID,
			Games.YEAR_PUBLISHED,
			Games.MIN_PLAYERS,
			Games.MAX_PLAYERS,
			Games.PLAYING_TIME,
			Games.MINIMUM_AGE,
			Games.DESCRIPTION,
		};

		// int ID = 0;
		int GAME_NAME = 1;
		int GAME_ID = 2;
		int YEAR_PUBLISHED = 3;
		int MIN_PLAYERS = 4;
		int MAX_PLAYERS = 5;
		int PLAYING_TIME = 6;
		int MINIMUM_AGE = 7;
		int DESCRIPTION = 8;
	}
}
