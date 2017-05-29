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
import android.support.v4.widget.ContentLoadingProgressBar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.widget.IntegerValueFormatter;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

public class PollFragment extends DialogFragment implements LoaderCallbacks<Cursor>, OnChartValueSelectedListener {
	public static final String LANGUAGE_DEPENDENCE = "language_dependence";
	public static final String SUGGESTED_PLAYER_AGE = "suggested_playerage";

	private static final Format FORMAT = new DecimalFormat("#0");

	private String pollType;
	private Uri pollResultUri;
	private int[] chartColors;
	private Snackbar snackbar;

	private Unbinder unbinder;
	@BindView(R.id.progress) ContentLoadingProgressBar progressView;
	@BindView(R.id.poll_scroll) ScrollView scrollView;
	@BindView(R.id.pie_chart) PieChart pieChart;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		int gameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		if (gameId == BggContract.INVALID_ID) dismiss();

		pollType = intent.getStringExtra(ActivityUtils.KEY_TYPE);
		pollResultUri = Games.buildPollResultsResultUri(gameId, pollType);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_poll, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		pieChart.setDrawSliceText(false);
		pieChart.setRotationEnabled(false);
		Legend legend = pieChart.getLegend();
		legend.setPosition(LegendPosition.BELOW_CHART_LEFT);
		legend.setWordWrapEnabled(true);
		pieChart.setDescription(null);
		pieChart.setOnChartValueSelectedListener(this);

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// size the graph to be 80% of the screen width
		DisplayMetrics display = this.getResources().getDisplayMetrics();
		ViewGroup.LayoutParams lp = pieChart.getLayoutParams();
		lp.width = (int) (display.widthPixels * .8);
		pieChart.setLayoutParams(lp);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (pollType == null) {
			Timber.w("Missing type");
			dismiss();
		}
		switch (pollType) {
			case LANGUAGE_DEPENDENCE:
				getDialog().setTitle(R.string.language_dependence);
				chartColors = ColorUtils.FIVE_STAGE_COLORS;
				break;
			case SUGGESTED_PLAYER_AGE:
				getDialog().setTitle(R.string.suggested_playerage);
				chartColors = null;
				break;
		}

		getLoaderManager().restartLoader(Query._TOKEN, null, this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == Query._TOKEN) {
			loader = new CursorLoader(getActivity(), pollResultUri, Query.PROJECTION, null, null, Query.SORT);
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) return;
		if (loader == null) return;

		if (loader.getId() == Query._TOKEN) {
			int totalVoteCount = cursor != null && cursor.moveToFirst() ? cursor.getInt(Query.POLL_TOTAL_VOTES) : 0;

			pieChart.setVisibility(totalVoteCount == 0 ? View.GONE : View.VISIBLE);
			if (totalVoteCount > 0) {
				createPieChart(cursor, totalVoteCount);
			}

			progressView.hide();
			scrollView.setVisibility(View.VISIBLE);
		} else {
			if (cursor != null) cursor.close();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@Override
	public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
		if (e == null || pieChart == null) {
			if (snackbar != null) {
				snackbar.dismiss();
			}
			return;
		}

		final View view = getView();
		if (view != null) {
			String message = getString(R.string.pie_chart_click_description, FORMAT.format(e.getVal()), pieChart.getXValue(e.getXIndex()));
			snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE);
			snackbar.show();
		}
	}

	@Override
	public void onNothingSelected() {
		if (snackbar != null) {
			snackbar.dismiss();
		}
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
		if (chartColors != null) {
			dataSet.setColors(chartColors);
		} else {
			dataSet.setColors(ColorUtils.createColors(index));
		}

		PieData data = new PieData(labels, dataSet);
		pieChart.setData(data);
		pieChart.setCenterText(getResources().getQuantityString(R.plurals.votes_suffix, voteCount, voteCount));

		pieChart.animateY(1000, EasingOption.EaseOutCubic);
	}

	private interface Query {
		int _TOKEN = 0x0;
		String[] PROJECTION = {
			GamePollResultsResult.POLL_RESULTS_RESULT_VALUE,
			GamePollResultsResult.POLL_RESULTS_RESULT_VOTES,
			GamePolls.POLL_TOTAL_VOTES
		};
		int POLL_RESULTS_RESULT_VALUE = 0;
		int POLL_RESULTS_RESULT_VOTES = 1;
		int POLL_TOTAL_VOTES = 2;

		String SORT = GamePollResultsResult.POLL_RESULTS_SORT_INDEX + " ASC, "
			+ GamePollResultsResult.POLL_RESULTS_RESULT_SORT_INDEX;
	}
}
