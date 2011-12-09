package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.widget.PlayerRow;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class PlayActivity extends Activity implements AsyncQueryListener {
	private final static String TAG = "PlayActivity";

	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";

	private static final int TOKEN_PLAY = 1;
	private static final int TOKEN_PLAYER = 2;
	private static final int TOKEN_GAME = 3;

	private NotifyingAsyncQueryHandler mHandler;
	private Uri mPlayUri;
	private Uri mPlayerUri;

	private int mGameId;
	private String mGameName;
	private String mThumbnailUrl;
	private Play mPlay;

	private TextView mDate;
	private TextView mQuantity;
	private TextView mLength;
	private TextView mLocation;
	private CheckBox mIncomplete;
	private CheckBox mNoWinStats;
	private TextView mComments;
	private LinearLayout mPlayerList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play);
		setUiVariables();
		parseIntent();
		UIUtils.setGameHeader(this, mGameName, mThumbnailUrl);

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(TOKEN_PLAY, mPlayUri, Query.PROJECTION);
		mHandler.startQuery(TOKEN_PLAYER, mPlayerUri, PlayerQuery.PROJECTION);
		if (TextUtils.isEmpty(mThumbnailUrl)) {
			Uri uri = Games.buildGameUri(mGameId);
			mHandler.startQuery(TOKEN_GAME, uri, GameQuery.PROJECTION);
		}
	}

	private void parseIntent() {
		Intent intent = getIntent();
		mGameId = intent.getExtras().getInt(KEY_GAME_ID);
		mGameName = intent.getExtras().getString(KEY_GAME_NAME);
		mThumbnailUrl = intent.getExtras().getString(KEY_THUMBNAIL_URL);

		if (mGameId == -1) {
			Log.w(TAG, "Didn't get a game ID");
			finish();
		}

		mPlayUri = intent.getData();
		int playId = Plays.getPlayId(mPlayUri);
		mPlayerUri = Plays.buildPlayerUri(playId);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		final MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.play, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_edit:
				ActivityUtils.logPlay(this, mPlay.PlayId, mGameId, mGameName, mThumbnailUrl);
				return true;
		}
		return false;
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

	@Override
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (token == TOKEN_PLAY) {
				if (!cursor.moveToFirst()) {
					return;
				}

				mGameName = cursor.getString(Query.NAME);
				UIUtils.setGameName(this, mGameName);

				mPlay = new Play();
				mPlay.populate(cursor);
				bindUi();
			} else if (token == TOKEN_PLAYER) {
				while (cursor.moveToNext()) {
					Player player = new Player(cursor);
					mPlay.addPlayer(player);
				}
				bindUi();
			} else if (token == TOKEN_GAME) {
				if (!cursor.moveToFirst()) {
					return;
				}

				mThumbnailUrl = cursor.getString(GameQuery.THUMBNAIL_URL);
				new UIUtils(this).setThumbnail(mThumbnailUrl);
			}
		} finally {
			cursor.close();
		}
	}

	private void setUiVariables() {
		mDate = (TextView) findViewById(R.id.play_date);
		mQuantity = (TextView) findViewById(R.id.play_quantity);
		mLength = (TextView) findViewById(R.id.play_length);
		mLocation = (TextView) findViewById(R.id.play_location);
		mIncomplete = (CheckBox) findViewById(R.id.play_incomplete);
		mNoWinStats = (CheckBox) findViewById(R.id.play_no_win_stats);
		mComments = (TextView) findViewById(R.id.play_comments);
		mPlayerList = (LinearLayout) findViewById(R.id.play_player_list);
	}

	private void bindUi() {
		mDate.setText(mPlay.getFormattedDate());
		mQuantity.setText(String.valueOf(mPlay.Quantity));
		mLength.setText(String.valueOf(mPlay.Length));
		mLocation.setText(mPlay.Location);
		mIncomplete.setChecked(mPlay.Incomplete);
		mNoWinStats.setChecked(mPlay.NoWinStats);
		mComments.setText(mPlay.Comments);
		for (Player player : mPlay.getPlayers()) {
			PlayerRow pr = new PlayerRow(this);
			pr.hideButtons();
			pr.setPlayer(player);
			mPlayerList.addView(pr);
		}
	}

	private interface Query {
		String[] PROJECTION = { Plays.PLAY_ID, PlayItems.NAME, PlayItems.OBJECT_ID, Plays.DATE, Plays.LOCATION,
				Plays.LENGTH, Plays.QUANTITY, Plays.INCOMPLETE, Plays.NO_WIN_STATS, Plays.COMMENTS, };

		int NAME = 1;
	}

	private interface PlayerQuery {
		String[] PROJECTION = { PlayPlayers.USER_NAME, PlayPlayers.NAME, PlayPlayers.START_POSITION, PlayPlayers.COLOR,
				PlayPlayers.SCORE, PlayPlayers.RATING, PlayPlayers.NEW, PlayPlayers.WIN, };
	}

	private interface GameQuery {
		String[] PROJECTION = { Games.THUMBNAIL_URL };

		int THUMBNAIL_URL = 0;
	}
}
