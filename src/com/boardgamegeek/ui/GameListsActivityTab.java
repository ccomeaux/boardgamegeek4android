package com.boardgamegeek.ui;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.boardgamegeek.R;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;

public class GameListsActivityTab extends Activity implements AsyncQueryListener {
	private static final String TAG = "GameListsActivityTab";

	private Uri mBoardgameUri;
	private NotifyingAsyncQueryHandler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tab_game_lists);

		setUiVariables();

		mBoardgameUri = getIntent().getData();
		getContentResolver().registerContentObserver(mBoardgameUri, true, new GameObserver(null));

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(mBoardgameUri, Query.PROJECTION);
	}

	private void setUiVariables() {
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (!cursor.moveToFirst()) {
				return;
			}
		} finally {
			cursor.close();
		}
	}

	class GameObserver extends ContentObserver {

		public GameObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			Log.d(TAG, "Caught changed URI = " + mBoardgameUri);
			mHandler.startQuery(mBoardgameUri, Query.PROJECTION);
		}
	}

	private interface Query {
		String[] PROJECTION = {};
	}
}
