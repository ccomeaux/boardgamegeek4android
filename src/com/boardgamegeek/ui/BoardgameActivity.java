package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.provider.BggContract.Games;

public class BoardgameActivity extends Activity implements AsyncQueryListener {
	// private final static String TAG = "BoardgameActivity";

	private Uri mBoardgameUri;
	private NotifyingAsyncQueryHandler mHandler;

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
			if (!cursor.moveToFirst()) return;

			mName.setText(cursor.getString(BoardgameQuery.GAME_NAME));
			mId.setText(cursor.getString(BoardgameQuery.GAME_ID));

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

	private interface BoardgameQuery {
		String[] PROJECTION = {
			Games._ID,
			Games.GAME_NAME,
			Games.GAME_ID,
		};

		//int ID = 0;
		int GAME_NAME = 1;
		int GAME_ID = 2;
	}
}
