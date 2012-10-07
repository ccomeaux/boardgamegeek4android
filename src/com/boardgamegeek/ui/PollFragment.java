package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.widget.PieChartView;
import com.boardgamegeek.ui.widget.PlayerNumberRow;
import com.boardgamegeek.ui.widget.PollKeyRow;
import com.boardgamegeek.util.UIUtils;

public class PollFragment extends SherlockDialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = makeLogTag(PollFragment.class);

	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_TYPE = "TYPE";
	public static final String LANGUAGE_DEPENDENCE = "language_dependence";
	public static final String SUGGESTED_PLAYERAGE = "suggested_playerage";
	public static final String SUGGESTED_NUMPLAYERS = "suggested_numplayers";
	// The following should not be externalized, they're used to match the incoming XML
	private static final String BEST = "Best";
	private static final String RECOMMENDED = "Recommended";
	private static final String NOT_RECOMMENDED = "Not Recommended";

	private int mGameId;
	private String mType;
	private int mPollCount;
	private int mKeyCount;
	private boolean mBarChart;

	private View mProgress;
	private ScrollView mScrollView;
	private TextView mVoteTotalView;
	private PieChartView mPieChart;
	private LinearLayout mPollList;
	private LinearLayout mKeyList;
	private LinearLayout mKeyList2;
	private View mKeyContainer;
	private View mKeyDivider;

	private Uri mUri;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGameId = intent.getIntExtra(KEY_GAME_ID, -1);
		mType = intent.getStringExtra(KEY_TYPE);
		mUri = Games.buildPollResultsResultUri(mGameId, mType);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_poll, null);

		mProgress = rootView.findViewById(R.id.progress);
		mScrollView = (ScrollView) rootView.findViewById(R.id.poll_scroll);
		mVoteTotalView = (TextView) rootView.findViewById(R.id.poll_vote_total);
		mPieChart = (PieChartView) rootView.findViewById(R.id.pie_chart);
		mPollList = (LinearLayout) rootView.findViewById(R.id.poll_list);
		mKeyList = (LinearLayout) rootView.findViewById(R.id.poll_key);
		mKeyList2 = (LinearLayout) rootView.findViewById(R.id.poll_key2);
		mKeyContainer = rootView.findViewById(R.id.poll_key_container);
		mKeyDivider = rootView.findViewById(R.id.poll_key_divider);

		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (LANGUAGE_DEPENDENCE.equals(mType)) {
			getDialog().setTitle(R.string.language_dependence);
		} else if (SUGGESTED_PLAYERAGE.equals(mType)) {
			getDialog().setTitle(R.string.suggested_playerage);
		} else if (SUGGESTED_NUMPLAYERS.equals(mType)) {
			mBarChart = true;
			getDialog().setTitle(R.string.suggested_numplayers);
			addKeyRow(0xff4cc417, BEST);
			addKeyRow(Color.YELLOW, RECOMMENDED);
			addKeyRow(Color.RED, NOT_RECOMMENDED);
		}

		getLoaderManager().restartLoader(Query._TOKEN, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == Query._TOKEN) {
			return new CursorLoader(getActivity(), mUri, Query.PROJECTION, null, null, Query.SORT);
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (loader.getId() == Query._TOKEN) {
			if (cursor != null && cursor.moveToFirst()) {
				mPollCount = cursor.getInt(Query.POLL_TOTAL_VOTES);
			} else {
				mPollCount = 0;
			}
			mVoteTotalView.setText(String.format(getResources().getString(R.string.votes_suffix), mPollCount));
			mProgress.setVisibility(View.GONE);
			mScrollView.setVisibility(View.VISIBLE);
			mPieChart.setVisibility((mPollCount == 0 || mBarChart) ? View.GONE : View.VISIBLE);
			mPollList.setVisibility((mPollCount == 0 || !mBarChart) ? View.GONE : View.VISIBLE);
			mKeyContainer.setVisibility((mPollCount == 0) ? View.GONE : View.VISIBLE);
			if (mPollCount > 0) {
				if (mBarChart) {
					createBarChart(cursor);
				} else {
					createPieChart(cursor);
				}
			}
		} else {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	private void createBarChart(Cursor cursor) {
		PlayerNumberRow row = null;
		String playerNumber = null;
		String lastPlayerNumber = "-1";
		while (cursor.moveToNext()) {
			playerNumber = cursor.getString(Query.POLL_RESULTS_PLAYERS);
			if (!lastPlayerNumber.equals(playerNumber)) {
				lastPlayerNumber = playerNumber;
				row = new PlayerNumberRow(getActivity());
				row.setText(playerNumber);
				row.setTotal(mPollCount);
				row.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						for (int i = 0; i < mPollList.getChildCount(); i++) {
							((PlayerNumberRow) mPollList.getChildAt(i)).clearHighlight();
						}
						PlayerNumberRow row = (PlayerNumberRow) v;
						row.setHighlight();

						int[] voteCount = row.getVotes();
						for (int i = 0; i < mKeyList.getChildCount(); i++) {
							((PollKeyRow) mKeyList.getChildAt(i)).setInfo(String.valueOf(voteCount[i]));
						}
					}
				});
				mPollList.addView(row);
			}

			String value = cursor.getString(Query.POLL_RESULTS_RESULT_VALUE);
			int votes = cursor.getInt(Query.POLL_RESULTS_RESULT_VOTES);
			if (BEST.equals(value)) {
				row.setBest(votes);
			} else if (RECOMMENDED.equals(value)) {
				row.setRecommended(votes);
			} else if (NOT_RECOMMENDED.equals(value)) {
				row.setNotRecommended(votes);
			} else {
				LOGW(TAG, "Bad key: " + value);
			}
		}
	}

	private void createPieChart(Cursor cursor) {
		ArrayList<Pair<CharSequence, Integer>> slices = new ArrayList<Pair<CharSequence, Integer>>();
		do {
			String value = cursor.getString(Query.POLL_RESULTS_RESULT_VALUE);
			int votes = cursor.getInt(Query.POLL_RESULTS_RESULT_VOTES);
			if (votes > 0) {
				slices.add(new Pair<CharSequence, Integer>(value, votes));
			}
		} while (cursor.moveToNext());

		mKeyCount = 0;
		int[] colors = CreateColors(slices.size());
		for (int i = 0; i < slices.size(); i++) {
			Pair<CharSequence, Integer> slice = slices.get(i);
			mPieChart.addSlice(slice.second, colors[i]);
			addKeyRow(colors[i], slice.first, String.valueOf(slice.second));
		}
	}

	private void addKeyRow(int color, CharSequence text) {
		addKeyRow(color, text, null);
	}

	private void addKeyRow(int color, CharSequence text, CharSequence info) {
		PollKeyRow pkr = new PollKeyRow(getActivity());
		pkr.setColor(color);
		pkr.setText(text);
		if (!TextUtils.isEmpty(info)) {
			pkr.setInfo(info);
		}
		mKeyCount++;
		if (mKeyCount > 6) {
			mKeyList2.addView(pkr);
			mKeyList2.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));
			mKeyDivider.setVisibility(View.VISIBLE);
		} else {
			mKeyList.addView(pkr);
		}
	}

	/**
	 * Calculate an array of high-contrast, alternating colors
	 */
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

	private interface Query {
		int _TOKEN = 0x0;
		String[] PROJECTION = { GamePollResultsResult.POLL_RESULTS_RESULT_VALUE,
			GamePollResultsResult.POLL_RESULTS_RESULT_VOTES, GamePollResults.POLL_RESULTS_PLAYERS,
			GamePolls.POLL_TOTAL_VOTES };
		int POLL_RESULTS_RESULT_VALUE = 0;
		int POLL_RESULTS_RESULT_VOTES = 1;
		int POLL_RESULTS_PLAYERS = 2;
		int POLL_TOTAL_VOTES = 3;

		String SORT = GamePollResultsResult.POLL_RESULTS_SORT_INDEX + " ASC, "
			+ GamePollResultsResult.POLL_RESULTS_RESULT_SORT_INDEX;
	}
}
