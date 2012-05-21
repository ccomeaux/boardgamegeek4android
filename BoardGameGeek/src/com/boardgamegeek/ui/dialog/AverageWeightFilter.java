package com.boardgamegeek.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.data.AverageWeightFilterData;
import com.boardgamegeek.ui.CollectionActivity;
import com.boardgamegeek.ui.widget.DualSliderView;
import com.boardgamegeek.ui.widget.DualSliderView.KnobValuesChangedListener;

public class AverageWeightFilter {
	private double mMinWeight;
	private double mMaxWeight;
	private boolean mUndefined;
	private static final int FACTOR = 10;
	private static final double STEP = 2;

	public void createDialog(final CollectionActivity activity, AverageWeightFilterData filter) {
		initValues(filter);

		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		// TODO: change layout
		View layout = inflater.inflate(R.layout.dialog_play_time, (ViewGroup) activity.findViewById(R.id.layout_root));

		final TextView textInterval = (TextView) layout.findViewById(R.id.text_interval);
		final DualSliderView sliderView = (DualSliderView) layout.findViewById(R.id.num_players_slider);
		final CheckBox checkbox = (CheckBox) layout.findViewById(R.id.undefined_checkbox);

		initSlider(activity, textInterval, sliderView);
		checkbox.setChecked(mUndefined);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(R.string.menu_average_weight)
				.setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.removeFilter(new AverageWeightFilterData());
					}
				}).setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						captureForm(sliderView, checkbox);
						activity.addFilter(new AverageWeightFilterData(activity, mMinWeight, mMaxWeight, mUndefined));
					}

					private void captureForm(final DualSliderView sliderView, final CheckBox checkbox) {
						double start = (double) (sliderView.getStartKnobValue()) / FACTOR;
						double end = (double) (sliderView.getEndKnobValue()) / FACTOR;
						if (start < end) {
							mMinWeight = start;
							mMaxWeight = end;
						} else {
							mMinWeight = end;
							mMaxWeight = start;
						}
						mUndefined = checkbox.isChecked();
					}
				}).setView(layout);

		builder.create().show();
	}

	private void initSlider(final CollectionActivity activity, final TextView textInterval,
			final DualSliderView sliderView) {
		sliderView.setRange((int) (AverageWeightFilterData.MIN_RANGE * FACTOR),
				(int) (AverageWeightFilterData.MAX_RANGE * FACTOR), STEP);
		sliderView.setStartKnobValue((int) (mMinWeight * FACTOR));
		sliderView.setEndKnobValue((int) (mMaxWeight * FACTOR));

		sliderView.setOnKnobValuesChangedListener(new KnobValuesChangedListener() {
			@Override
			public void onValuesChanged(boolean knobStartChanged, boolean knobEndChanged, int knobStart, int knobEnd) {
				double start = (double) knobStart / FACTOR;
				double end = (double) knobEnd / FACTOR;
				String text = "";
				if (!sliderView.isSecondThumbEnabled() && knobEndChanged) {
					text = intervalText(end);
				} else if (knobStartChanged || knobEndChanged) {
					if (start == end) {
						text = intervalText(end);
					} else if (start < end) {
						text = intervalText(start, end);
					} else {
						text = intervalText(end, start);
					}
				}
				textInterval.setText(text);
			}
		});
	}

	private void initValues(AverageWeightFilterData filter) {
		if (filter == null) {
			mMinWeight = AverageWeightFilterData.MIN_RANGE;
			mMaxWeight = AverageWeightFilterData.MAX_RANGE;
			mUndefined = false;
		} else {
			mMinWeight = filter.getMin();
			mMaxWeight = filter.getMax();
			mUndefined = filter.isUndefined();
		}
	}

	private String intervalText(double number) {
		return String.valueOf(number);
	}

	private String intervalText(double min, double max) {
		return min + " - " + max;
	}
}
