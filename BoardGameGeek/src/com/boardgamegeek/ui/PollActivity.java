package com.boardgamegeek.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.widget.PieChartView;
import com.boardgamegeek.ui.widget.PlayerNumberRow;
import com.boardgamegeek.ui.widget.PollKeyRow;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class PollActivity extends Activity implements AsyncQueryListener {

	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_TYPE = "TYPE";
	public static final String LANGUAGE_DEPENDENCE = "language_dependence";
	public static final String SUGGESTED_PLAYERAGE = "suggested_playerage";
	public static final String SUGGESTED_NUMPLAYERS = "suggested_numplayers";

	private static final String TAG = "PollActivity";
	// The following should not be externalized, they're used to match the
	// incoming XML
	private static final String BEST = "Best";
	private static final String RECOMMENDED = "Recommended";
	private static final String NOT_RECOMMENDED = "Not Recommended";

	private static final int TOKEN_POLL = 1;
	private static final int TOKEN_POLL_RESULTS = 2;
	private static final int TOKEN_POLL_RESULTS_RESULT = 3;

	private Cursor mCursor;
	private NotifyingAsyncQueryHandler mHandler;
	private int mGameId;
	private String mType;
	private Uri mPollUri;
	private Uri mPollResultsUri;

	private ScrollView mScrollView;
	private TextView mVoteTotalView;
	private PieChartView mPieChart;
	private LinearLayout mLinearLayoutList;
	private LinearLayout mLinearLayoutKey;
	private LinearLayout mLinearLayoutKey2;
	private View mKeyDivider;

	private int mPollCount;
	private int mKeyCount;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);

		reset();

		setContentView(R.layout.activity_poll);

		setUiVariables();
		setUris();
		initUi();
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
		mScrollView = (ScrollView) findViewById(R.id.poll_scroll);
		mVoteTotalView = (TextView) findViewById(R.id.poll_vote_total);
		mPieChart = (PieChartView) findViewById(R.id.pie_chart);
		mLinearLayoutList = (LinearLayout) findViewById(R.id.poll_list);
		mLinearLayoutKey = (LinearLayout) findViewById(R.id.poll_key);
		mLinearLayoutKey2 = (LinearLayout) findViewById(R.id.poll_key2);
		mKeyDivider = findViewById(R.id.poll_key_divider);
	}

	private void setUris() {
		mGameId = getIntent().getIntExtra(KEY_GAME_ID, -1);
		mType = getIntent().getStringExtra(KEY_TYPE);
		mPollUri = Games.buildPollsUri(mGameId, mType);
		mPollResultsUri = Games.buildPollResultsUri(mGameId, mType);
	}

	private void initUi() {
		if (LANGUAGE_DEPENDENCE.equals(mType)) {
			setTitle(R.string.language_dependence);
		} else if (SUGGESTED_PLAYERAGE.equals(mType)) {
			setTitle(R.string.suggested_playerage);
		} else if (SUGGESTED_NUMPLAYERS.equals(mType)) {
			setTitle(R.string.suggested_numplayers);
			addKeyRow(Color.GREEN, BEST);
			addKeyRow(Color.YELLOW, RECOMMENDED);
			addKeyRow(Color.RED, NOT_RECOMMENDED);
		}
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (token == TOKEN_POLL) {
				mCursor = cursor;
				if (cursor.moveToFirst()) {
					mPollCount = cursor.getInt(PollQuery.POLL_TOTAL_VOTES);
				}
				mVoteTotalView.setText(String.format(getResources().getString(R.string.votes_suffix), mPollCount));
				mHandler.startQuery(TOKEN_POLL_RESULTS, null, mPollResultsUri, GamePollResultsQuery.PROJECTION, null,
						null, GamePollResults.DEFAULT_SORT);
			} else if (token == TOKEN_POLL_RESULTS) {
				while (cursor.moveToNext()) {
					final String key = cursor.getString(GamePollResultsQuery.POLL_RESULTS_KEY.ordinal());
					mHandler.startQuery(TOKEN_POLL_RESULTS_RESULT, (key == null) ? "" : key,
							Games.buildPollResultsResultUri(mGameId, mType, key),
							GamePollResultsResultQuery.PROJECTION, null, null, GamePollResultsResult.DEFAULT_SORT);
				}
			} else if (token == TOKEN_POLL_RESULTS_RESULT) {
				PlayerNumberRow pnr = new PlayerNumberRow(this);
				String key = cookie.toString();
				if ("X".equals(key)) {
					mKeyCount = 0;
					ArrayList<SuggestedAgesElement> suggestedAgesList = new ArrayList<SuggestedAgesElement>();
					while (cursor.moveToNext()) {
						String value = cursor.getString(GamePollResultsResultQuery.POLL_RESULTS_VALUE.ordinal());
						int votes = cursor.getInt(GamePollResultsResultQuery.POLL_RESULTS_VOTES.ordinal());
						if (votes > 0) {
							suggestedAgesList.add(new SuggestedAgesElement(value, votes));
						}
					}
					int[] colors = CreateColors(suggestedAgesList.size());
					int colorIndex = 0;
					for (SuggestedAgesElement suggestedAgesElement : suggestedAgesList) {
						mPieChart.addSlice(suggestedAgesElement.votes, colors[colorIndex]);
						addKeyRow(colors[colorIndex], suggestedAgesElement.value, String.valueOf(suggestedAgesElement.votes));
						colorIndex++;
					}
					
					mPieChart.setVisibility(View.VISIBLE);
				} else {
					pnr.setText(key);
					pnr.setTotal(mPollCount);
					int[] voteCount = new int[3];
					while (cursor.moveToNext()) {
						String value = cursor.getString(GamePollResultsResultQuery.POLL_RESULTS_VALUE.ordinal());
						int votes = cursor.getInt(GamePollResultsResultQuery.POLL_RESULTS_VOTES.ordinal());
						if (BEST.equals(value)) {
							pnr.setBest(votes);
							voteCount[0] = votes;
						} else if (RECOMMENDED.equals(value)) {
							pnr.setRecommended(votes);
							voteCount[1] = votes;
						} else if (NOT_RECOMMENDED.equals(value)) {
							pnr.setNotRecommended(votes);
							voteCount[2] = votes;
						} else {
							Log.w(TAG, "Bad key: " + value);
						}
					}
					pnr.setTag(voteCount);

					pnr.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							for (int i = 0; i < mLinearLayoutList.getChildCount(); i++) {
								((PlayerNumberRow) mLinearLayoutList.getChildAt(i)).clearHighlight();
							}
							((PlayerNumberRow) v).setHighlight();

							int[] voteCount = (int[]) v.getTag();
							for (int i = 0; i < mLinearLayoutKey.getChildCount(); i++) {
								((PollKeyRow) mLinearLayoutKey.getChildAt(i)).setInfo(String.valueOf(voteCount[i]));
							}
						}
					});
					mLinearLayoutList.addView(pnr);
				}

				mScrollView.setVisibility(View.VISIBLE);
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

	private int[] CreateColors(int count) {
		int[] colors = new int[count];
		if (count > 0) {
			float[] hsv = new float[3];
			hsv[1] = 0.75f;
			hsv[2] = 1f;
			float factor = (float) (360.0 / count);
			int colorIndex = 0;
			for (int i = 0; i < count; i++) {
				hsv[0] = i * factor;
				colors[colorIndex] = Color.HSVToColor(hsv);
				colorIndex += 2;
				if (colorIndex >= colors.length) {
					colorIndex = 1;
				}
			}
		}
		return colors;
	}

	private void addKeyRow(int color, CharSequence text, CharSequence info) {
		PollKeyRow pkr = new PollKeyRow(this);
		pkr.setColor(color);
		pkr.setText(text);
		if (!TextUtils.isEmpty(info)) {
			pkr.setInfo(info);
		}
		mKeyCount++;
		if (mKeyCount > 6) {
			mLinearLayoutKey2.addView(pkr);
			mLinearLayoutKey2.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));
			mKeyDivider.setVisibility(View.VISIBLE);
		} else {
			mLinearLayoutKey.addView(pkr);
		}
	}

	private void addKeyRow(int color, CharSequence text) {
		addKeyRow(color, text, null);
	}
	
	private class SuggestedAgesElement {
		CharSequence value;
		int votes;
		
		SuggestedAgesElement(CharSequence value, int votes) {
			this.value = value;
			this.votes = votes;
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
