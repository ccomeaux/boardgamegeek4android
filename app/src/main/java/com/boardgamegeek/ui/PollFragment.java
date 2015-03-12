package com.boardgamegeek.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
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

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.widget.PieChartView;
import com.boardgamegeek.ui.widget.PlayerNumberRow;
import com.boardgamegeek.ui.widget.PollKeyRow;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

public class PollFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	public static final String LANGUAGE_DEPENDENCE = "language_dependence";
	public static final String SUGGESTED_PLAYERAGE = "suggested_playerage";
	public static final String SUGGESTED_NUMPLAYERS = "suggested_numplayers";
	// The following should not be externalized, they're used to match the incoming XML
	private static final String BEST = "Best";
	private static final String RECOMMENDED = "Recommended";
	private static final String NOT_RECOMMENDED = "Not Recommended";

	private String mType;
	private int mPollCount;
	private int mKeyCount;
	private boolean mBarChart;
	private Uri mUri;

	@InjectView((R.id.progress)) View mProgress;
	@InjectView(R.id.poll_scroll) ScrollView mScrollView;
	@InjectView(R.id.poll_vote_total) TextView mVoteTotalView;
	@InjectView(R.id.pie_chart) PieChartView mPieChart;
	@InjectView(R.id.poll_list) LinearLayout mPollList;
	@InjectView(R.id.poll_key) LinearLayout mKeyList;
	@InjectView(R.id.poll_key2) LinearLayout mKeyList2;
	@InjectView(R.id.poll_key_container) View mKeyContainer;
	@InjectView(R.id.poll_key_divider) View mKeyDivider;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		int gameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		mType = intent.getStringExtra(ActivityUtils.KEY_TYPE);
		mUri = Games.buildPollResultsResultUri(gameId, mType);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_poll, container, false);
		ButterKnife.inject(this, rootView);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (mType == null) {
			Timber.w("Missing type");
		}
		switch (mType) {
			case LANGUAGE_DEPENDENCE:
				getDialog().setTitle(R.string.language_dependence);
				break;
			case SUGGESTED_PLAYERAGE:
				getDialog().setTitle(R.string.suggested_playerage);
				break;
			case SUGGESTED_NUMPLAYERS:
				mBarChart = true;
				getDialog().setTitle(R.string.suggested_numplayers);
				addKeyRow(getResources().getColor(R.color.best), BEST);
				addKeyRow(getResources().getColor(R.color.recommended), RECOMMENDED);
				addKeyRow(getResources().getColor(R.color.not_recommended), NOT_RECOMMENDED);
				break;
		}

		getLoaderManager().restartLoader(Query._TOKEN, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == Query._TOKEN) {
			loader = new CursorLoader(getActivity(), mUri, Query.PROJECTION, null, null, Query.SORT);
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
			mVoteTotalView.setText(getResources().getString(R.string.votes_suffix, mPollCount));
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
		mPollList.removeAllViews();
		PlayerNumberRow row = null;
		String playerNumber;
		String lastPlayerNumber = "-1";
		do {
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

			if (row != null) {
				String value = cursor.getString(Query.POLL_RESULTS_RESULT_VALUE);
				if (value == null) {
					Timber.w("Missing key");
				}
				int votes = cursor.getInt(Query.POLL_RESULTS_RESULT_VOTES);
				switch (value) {
					case BEST:
						row.setBest(votes);
						break;
					case RECOMMENDED:
						row.setRecommended(votes);
						break;
					case NOT_RECOMMENDED:
						row.setNotRecommended(votes);
						break;
					default:
						Timber.w("Bad key: " + value);
						break;
				}
			}
		} while (cursor.moveToNext());
	}

	private void createPieChart(Cursor cursor) {
		ArrayList<Pair<CharSequence, Integer>> slices = new ArrayList<>();
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
