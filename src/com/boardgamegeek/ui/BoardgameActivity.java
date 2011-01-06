package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;

public class BoardgameActivity extends Activity implements AsyncQueryListener {
	// private final static String TAG = "BoardgameActivity";

	private Uri mBoardgameUri;
	private NotifyingAsyncQueryHandler mHandler;

	private int id;
	private String name;

	private TextView mName;
	private TextView mId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_boardgame);
		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);

		mName = (TextView) findViewById(R.id.game_name);
		mId = (TextView) findViewById(R.id.game_id);

		final Intent intent = getIntent();
		mBoardgameUri = intent.getData();

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(mBoardgameUri, BoardgameQuery.PROJECTION);
	}

	@Override
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (!cursor.moveToFirst())
				return;

			id = cursor.getInt(BoardgameQuery.GAME_ID);
			name = cursor.getString(BoardgameQuery.GAME_NAME);

			mName.setText(name);
			mId.setText("" + id);

			// final String url = cursor.getString(BuddiesQuery.AVATAR_URL);
			// new BuddyAvatarTask().execute(url);

		} finally {
			cursor.close();
		}
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
		mi.setEnabled(!TextUtils.isEmpty(name));
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
		intent.putExtra("GAME_ID", id);
		intent.putExtra("GAME_NAME", name);
		startActivity(intent);
	}

	private interface BoardgameQuery {
		String[] PROJECTION = {
			Games._ID,
			Games.GAME_NAME,
			Games.GAME_ID,
		};

		// int ID = 0;
		int GAME_NAME = 1;
		int GAME_ID = 2;
	}
}
