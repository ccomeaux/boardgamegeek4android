package com.boardgamegeek.ui;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.widget.PlayerNumberRow;
import com.boardgamegeek.ui.widget.PollKeyRow;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class PollActivity extends Activity implements AsyncQueryListener {

	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_TYPE = "TYPE";

	private static final String TAG = "PollActivity";
	private static final String SUGGESTED_NUMPLAYERS = "suggested_numplayers";
	private static final String BEST = "Best";
	private static final String RECOMMENDED = "Recommended";
	private static final String NOT_RECOMMENDED = "Not Recommended";

	private static final int TOKEN_POLL = 1;
	private static final int TOKEN_POLL_RESULTS = 2;
	private static final int TOKEN_POLL_RESULTS_RESULT = 3;

	private Cursor mCursor;
	private NotifyingAsyncQueryHandler mHandler;
	private int mGameId;
	private Uri mPollUri;
	private Uri mPollResultsUri;

	private LinearLayout mLinearLayout;

	private int mKeyCount = 3;
	private int mPollCount;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);

		reset();

		setContentView(R.layout.activity_poll);

		setUiVariables();
		setUris();
		mHandler.startQuery(TOKEN_POLL, null, mPollUri, PollQuery.PROJECTION, null, null, GamePolls.DEFAULT_SORT);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getContentResolver().registerContentObserver(mPollUri, true, mPollObserver);
		if (mCursor != null) {
			mCursor.requery();
		}
	}

	@Override
	protected void onPause() {
		getContentResolver().unregisterContentObserver(mPollObserver);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		reset();
		super.onDestroy();
	}

	private void reset() {
		if (mCursor != null) {
			mCursor.close();
			stopManagingCursor(mCursor);
			mCursor = null;
		}
		mHandler.cancelOperation(TOKEN_POLL);
		mHandler.cancelOperation(TOKEN_POLL_RESULTS);
		mHandler.cancelOperation(TOKEN_POLL_RESULTS_RESULT);
	}

	private void setUiVariables() {
		mLinearLayout = (LinearLayout) findViewById(R.id.poll_list);
	}

	private void setUris() {
		mGameId = getIntent().getIntExtra(KEY_GAME_ID, -1);
		String type = getIntent().getStringExtra(KEY_TYPE);
		if (SUGGESTED_NUMPLAYERS.equals(type)) {
			mPollUri = Games.buildPollsUri(mGameId, SUGGESTED_NUMPLAYERS);
			mPollResultsUri = Games.buildPollResultsUri(mGameId, SUGGESTED_NUMPLAYERS);

			addKeyRow(BEST, Color.GREEN);
			addKeyRow(RECOMMENDED, Color.YELLOW);
			addKeyRow(NOT_RECOMMENDED, Color.RED);
		}
	}

	private void addKeyRow(String text, int color) {
		PollKeyRow pkr = new PollKeyRow(this);
		pkr.setText(text);
		pkr.setColor(color);
		mLinearLayout.addView(pkr);
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (token == TOKEN_POLL) {
				mCursor = cursor;
				if (cursor.moveToFirst()) {
					mPollCount = cursor.getInt(PollQuery.POLL_TOTAL_VOTES);
				}
				mHandler.startQuery(TOKEN_POLL_RESULTS, null, mPollResultsUri, GamePollResultsQuery.PROJECTION, null,
						null, GamePollResults.DEFAULT_SORT);
			} else if (token == TOKEN_POLL_RESULTS) {
				while (cursor.moveToNext()) {
					final String key = cursor.getString(GamePollResultsQuery.POLL_RESULTS_KEY.ordinal());
					mHandler.startQuery(TOKEN_POLL_RESULTS_RESULT, key,
							Games.buildPollResultsResultUri(mGameId, SUGGESTED_NUMPLAYERS, key),
							GamePollResultsResultQuery.PROJECTION, null, null, GamePollResultsResult.DEFAULT_SORT);
				}
			} else if (token == TOKEN_POLL_RESULTS_RESULT) {
				PlayerNumberRow pnr = new PlayerNumberRow(this);
				pnr.setText(cookie.toString());
				pnr.setTotal(mPollCount);
				while (cursor.moveToNext()) {
					String key = cursor.getString(GamePollResultsResultQuery.POLL_RESULTS_VALUE.ordinal());
					int votes = cursor.getInt(GamePollResultsResultQuery.POLL_RESULTS_VOTES.ordinal());
					if (BEST.equals(key)) {
						pnr.setBest(votes);
					} else if (RECOMMENDED.equals(key)) {
						pnr.setRecommended(votes);
					} else if (NOT_RECOMMENDED.equals(key)) {
						pnr.setNotRecommended(votes);
					} else {
						Log.w(TAG, "Bad key: " + key);
					}
				}
				mLinearLayout.addView(pnr, mLinearLayout.getChildCount() - mKeyCount);

				mLinearLayout.setVisibility(View.VISIBLE);
				findViewById(R.id.progress).setVisibility(View.GONE);
			} else {
				Toast.makeText(this, "Unexpected onQueryComplete token: " + token, Toast.LENGTH_LONG).show();
			}
		} finally {
			if (cursor != null && cursor != mCursor) {
				cursor.close();
			}
		}
	}

	private ContentObserver mPollObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			Log.d(TAG, "Caught changed URI = " + mPollUri);
			if (mCursor != null) {
				mCursor.requery();
			}
		}
	};

	private interface PollQuery {
		String[] PROJECTION = { GamePolls.POLL_TOTAL_VOTES, };

		int POLL_TOTAL_VOTES = 0;
	}

	private enum GamePollResultsQuery {
		_ID(GamePollResults._ID), POLL_ID(GamePollResults.POLL_ID), POLL_RESULTS_KEY(GamePollResults.POLL_RESULTS_KEY), POLL_RESULTS_PLAYERS(
				GamePollResults.POLL_RESULTS_PLAYERS);

		public static String[] PROJECTION = UIUtils.projectionFromEnums(GamePollResultsQuery.values());

		private String mColumnName;

		GamePollResultsQuery(String columnName) {
			mColumnName = columnName;
		}

		@Override
		public String toString() {
			return mColumnName;
		}
	}

	private enum GamePollResultsResultQuery {
		POLL_RESULTS_ID(GamePollResultsResult.POLL_RESULTS_ID), POLL_RESULTS_LEVEL(
				GamePollResultsResult.POLL_RESULTS_RESULT_LEVEL), POLL_RESULTS_VALUE(
				GamePollResultsResult.POLL_RESULTS_RESULT_VALUE), POLL_RESULTS_VOTES(
				GamePollResultsResult.POLL_RESULTS_RESULT_VOTES);

		public static String[] PROJECTION = UIUtils.projectionFromEnums(GamePollResultsResultQuery.values());

		private String mColumnName;

		GamePollResultsResultQuery(String columnName) {
			mColumnName = columnName;
		}

		@Override
		public String toString() {
			return mColumnName;
		}
	}
}
