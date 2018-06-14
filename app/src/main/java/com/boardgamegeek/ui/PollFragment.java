package com.boardgamegeek.ui;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.GamePollEntity;
import com.boardgamegeek.entities.GamePollResultEntity;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.viewmodel.GameViewModel;
import com.boardgamegeek.ui.widget.IntegerValueFormatter;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.DialogUtils;
import com.github.mikephil.charting.animation.Easing.EasingOption;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment;
import com.github.mikephil.charting.components.Legend.LegendVerticalAlignment;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
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

public class PollFragment extends DialogFragment {
	private static final String KEY_TYPE = "TYPE";
	private static final String KEY_GAME_ID = "GAME_ID";
	public static final String LANGUAGE_DEPENDENCE = "language_dependence";
	public static final String SUGGESTED_PLAYER_AGE = "suggested_playerage";
	private static final Format FORMAT = new DecimalFormat("#0");

	private int gameId = BggContract.INVALID_ID;
	private String pollType;
	private int[] chartColors;
	private Snackbar snackbar;

	GameViewModel viewModel;

	private Unbinder unbinder;
	@BindView(R.id.progressView) ContentLoadingProgressBar progressView;
	@BindView(R.id.scrollView) ScrollView scrollView;
	@BindView(R.id.chartView) PieChart pieChart;

	public static void launchLanguageDependence(Fragment host, int gameId) {
		launch(host, gameId, PollFragment.LANGUAGE_DEPENDENCE);
	}

	public static void launchSuggestedPlayerAge(Fragment host, int gameId) {
		launch(host, gameId, PollFragment.SUGGESTED_PLAYER_AGE);
	}

	private static void launch(Fragment host, int gameId, String type) {
		Bundle arguments = new Bundle(2);
		arguments.putInt(KEY_GAME_ID, gameId);
		arguments.putString(KEY_TYPE, type);
		final PollFragment dialog = new PollFragment();
		dialog.setArguments(arguments);
		dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_bgglight_Dialog);
		DialogUtils.showAndSurvive(host, dialog);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		readBundle(getArguments());
		if (gameId == BggContract.INVALID_ID) {
			Toast.makeText(getContext(), R.string.msg_invalid_game_id, Toast.LENGTH_SHORT).show();
			dismiss();
		}
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID);
		pollType = bundle.getString(KEY_TYPE);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_poll, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		unbinder = ButterKnife.bind(this, view);

		pieChart.setDrawEntryLabels(false);
		pieChart.setRotationEnabled(false);
		Legend legend = pieChart.getLegend();
		legend.setHorizontalAlignment(LegendHorizontalAlignment.LEFT);
		legend.setVerticalAlignment(LegendVerticalAlignment.BOTTOM);
		legend.setWordWrapEnabled(true);
		pieChart.setDescription(null);
		pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
			@Override
			public void onValueSelected(Entry e, Highlight h) {
				PieEntry pe = (PieEntry) e;
				if (pe == null || pieChart == null) {
					if (snackbar != null) {
						snackbar.dismiss();
					}
					return;
				}

				final View view = getView();
				if (view != null) {
					String message = getString(R.string.pie_chart_click_description, FORMAT.format(pe.getY()), pe.getLabel());
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
		});

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

		viewModel = ViewModelProviders.of(getActivity()).get(GameViewModel.class);
		viewModel.setId(gameId);

		switch (pollType) {
			case LANGUAGE_DEPENDENCE:
				getDialog().setTitle(R.string.language_dependence);
				chartColors = ColorUtils.FIVE_STAGE_COLORS;
				viewModel.getLanguagePoll().observe(this, new Observer<GamePollEntity>() {
					@Override
					public void onChanged(@Nullable GamePollEntity gamePollEntity) {
						createPieChart(gamePollEntity);
					}
				});
				break;
			case SUGGESTED_PLAYER_AGE:
				getDialog().setTitle(R.string.suggested_playerage);
				chartColors = ColorUtils.TWELVE_STAGE_COLORS;
				viewModel.getAgePoll().observe(this, new Observer<GamePollEntity>() {
					@Override
					public void onChanged(@Nullable GamePollEntity gamePollEntity) {
						createPieChart(gamePollEntity);
					}
				});
				break;
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	private void createPieChart(GamePollEntity gamePollEntity) {
		int totalVoteCount = gamePollEntity == null ? 0 : gamePollEntity.getTotalVotes();
		pieChart.setVisibility(totalVoteCount == 0 ? View.GONE : View.VISIBLE);
		if (totalVoteCount > 0) {
			List<PieEntry> entries = new ArrayList<>();
			for (GamePollResultEntity result : gamePollEntity.getResults()) {
				entries.add(new PieEntry(result.getNumberOfVotes(), result.getValue()));
			}
			PieDataSet dataSet = new PieDataSet(entries, "");
			dataSet.setValueFormatter(new IntegerValueFormatter(true));
			if (chartColors != null) dataSet.setColors(chartColors);

			PieData data = new PieData(dataSet);
			pieChart.setData(data);
			pieChart.setCenterText(getResources().getQuantityString(R.plurals.votes_suffix, totalVoteCount, totalVoteCount));

			pieChart.animateY(1000, EasingOption.EaseOutCubic);
		}
		progressView.hide();
		scrollView.setVisibility(View.VISIBLE);
	}
}
