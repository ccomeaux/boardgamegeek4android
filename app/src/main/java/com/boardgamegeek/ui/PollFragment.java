package com.boardgamegeek.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.DisplayMetrics;
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
import com.boardgamegeek.ui.widget.IntegerValueFormatter;
import com.boardgamegeek.ui.widget.PlayerNumberRow;
import com.boardgamegeek.ui.widget.PollKeyRow;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.UIUtils;
import com.github.mikephil.charting.animation.Easing.EasingOption;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendPosition;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

public class PollFragment extends DialogFragment implements LoaderCallbacks<Cursor>, OnChartValueSelectedListener {
	public static final String LANGUAGE_DEPENDENCE = "language_dependence";
	public static final String SUGGESTED_PLAYERAGE = "suggested_playerage";
	public static final String SUGGESTED_NUMPLAYERS = "suggested_numplayers";
	// The following should not be externalized, they're used to match the incoming XML
	private static final String BEST = "Best";
	private static final String RECOMMENDED = "Recommended";
	private static final String NOT_RECOMMENDED = "Not Recommended";

	private static final Format FORMAT = new DecimalFormat("#0");

	private String mType;
	private int mPollCount;
	private int mKeyCount;
	private boolean mBarChart;
	private Uri mUri;
	private int[] mColors;
	private Snackbar mSnackbar;

	@InjectView((R.id.progress)) View mProgress;
	@InjectView(R.id.poll_scroll) ScrollView mScrollView;
	@InjectView(R.id.poll_vote_total) TextView mVoteTotalView;
	@InjectView(R.id.pie_chart) PieChart mPieChart;
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

		mPieChart.setDrawSliceText(false);
		mPieChart.setRotationEnabled(false);
		Legend legend = mPieChart.getLegend();
		legend.setPosition(LegendPosition.BELOW_CHART_LEFT);
		legend.setWordWrapEnabled(true);
		mPieChart.setDescription(null);
		mPieChart.setOnChartValueSelectedListener(this);

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// size the graph to be 80% of the screen width
		DisplayMetrics display = this.getResources().getDisplayMetrics();
		ViewGroup.LayoutParams lp = mPieChart.getLayoutParams();
		lp.width = (int) (display.widthPixels * .8);
		mPieChart.setLayoutParams(lp);
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
				mColors = ColorUtils.FIVE_STAGE_COLORS;
				break;
			case SUGGESTED_PLAYERAGE:
				getDialog().setTitle(R.string.suggested_playerage);
				mColors = null;
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
			mVoteTotalView.setVisibility(!mBarChart ? View.GONE : View.VISIBLE);

			mPieChart.setVisibility((mPollCount == 0 || mBarChart) ? View.GONE : View.VISIBLE);
			mPollList.setVisibility((mPollCount == 0 || !mBarChart) ? View.GONE : View.VISIBLE);
			mKeyContainer.setVisibility((mPollCount == 0 || !mBarChart) ? View.GONE : View.VISIBLE);
			if (mPollCount > 0) {
				if (mBarChart) {
					createBarChart(cursor);
				} else {
					createPieChart(cursor, mPollCount);
				}
			}

			mProgress.setVisibility(View.GONE);
			mScrollView.setVisibility(View.VISIBLE);
		} else {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@Override
	public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
		if (e == null || mPieChart == null) {
			if (mSnackbar != null) {
				mSnackbar.dismiss();
			}
			return;
		}

		String s = getString(R.string.pie_chart_click_description, FORMAT.format(e.getVal()), mPieChart.getXValue(e.getXIndex()));
		mSnackbar = Snackbar.make(getView(), s, Snackbar.LENGTH_INDEFINITE);
		mSnackbar.show();
	}

	@Override
	public void onNothingSelected() {
		if (mSnackbar != null) {
			mSnackbar.dismiss();
		}
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

	private void createPieChart(Cursor cursor, int voteCount) {
		List<String> labels = new ArrayList<>();
		List<Entry> entries = new ArrayList<>();
		int index = 0;
		do {
			String value = cursor.getString(Query.POLL_RESULTS_RESULT_VALUE);
			int votes = cursor.getInt(Query.POLL_RESULTS_RESULT_VOTES);

			labels.add(value);
			entries.add(new Entry(votes, index));
			index++;
		} while (cursor.moveToNext());

		PieDataSet dataSet = new PieDataSet(entries, "");
		dataSet.setValueFormatter(new IntegerValueFormatter(true));
		if (mColors != null) {
			dataSet.setColors(mColors);
		} else {
			dataSet.setColors(ColorUtils.createColors(index));
		}

		PieData data = new PieData(labels, dataSet);
		mPieChart.setData(data);
		mPieChart.setCenterText(getResources().getString(R.string.votes_suffix, voteCount));

		mPieChart.animateY(1000, EasingOption.EaseOutCubic);
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
