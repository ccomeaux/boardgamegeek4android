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
import com.boardgamegeek.data.PlayTimeFilterData;
import com.boardgamegeek.ui.CollectionActivity;
import com.boardgamegeek.ui.widget.DualSliderView;
import com.boardgamegeek.ui.widget.DualSliderView.KnobValuesChangedListener;

public class PlayTimeFilter {

	private static final int INCREMENT = 5;
	private static final int LINE_SPACING = 30 / INCREMENT;

	private int mMinTime;
	private int mMaxTime;
	private boolean mUndefined;

	public void createDialog(final CollectionActivity activity, PlayTimeFilterData filter) {
		initValues(filter);

		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_play_time, (ViewGroup) activity.findViewById(R.id.layout_root));

		final TextView textInterval = (TextView) layout.findViewById(R.id.text_interval);
		final DualSliderView sliderView = (DualSliderView) layout.findViewById(R.id.num_players_slider);
		final CheckBox checkbox = (CheckBox) layout.findViewById(R.id.undefined_checkbox);

		sliderView.setRange(PlayTimeFilterData.MIN_RANGE / INCREMENT, PlayTimeFilterData.MAX_RANGE / INCREMENT);
		sliderView.setStartKnobValue(mMinTime / INCREMENT);
		sliderView.setEndKnobValue(mMaxTime / INCREMENT);
		sliderView.setLineSpacing(LINE_SPACING);
		sliderView.setOnKnobValuesChangedListener(new KnobValuesChangedListener() {
			@Override
			public void onValuesChanged(boolean knobStartChanged, boolean knobEndChanged, int knobStart, int knobEnd) {
				int start = knobStart * INCREMENT;
				int end = knobEnd * INCREMENT;
				String s = "";
				if (!sliderView.isSecondThumbEnabled() && knobEndChanged) {
					s = intervalText(end);
				} else if (knobStartChanged || knobEndChanged) {
					if (start == end) {
						s = intervalText(end);
					} else if (start < end) {
						s = intervalText(start, end);
					} else {
						s = intervalText(end, start);
					}
				}
				textInterval.setText(s + " " + activity.getResources().getString(R.string.time_suffix));
			}
		});

		checkbox.setChecked(mUndefined);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(R.string.menu_play_time)
				.setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.removeFilter(new PlayTimeFilterData());
					}
				}).setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						int first = sliderView.getFirstKnobValue() * INCREMENT;
						int second = sliderView.getSecondKnobValue() * INCREMENT;
						// Sliders can be on either side so need to check which one is smaller
						if (sliderView.getFirstKnobValue() < sliderView.getSecondKnobValue()) {
							mMinTime = first;
							mMaxTime = second;
						} else {
							mMinTime = first;
							mMaxTime = first;
						}
						mUndefined = checkbox.isChecked();

						activity.addFilter(new PlayTimeFilterData(activity, mMinTime, mMaxTime, mUndefined));
					}
				}).setView(layout);

		builder.create().show();
	}

	private void initValues(PlayTimeFilterData filter) {
		if (filter == null) {
			mMinTime = PlayTimeFilterData.MIN_RANGE;
			mMaxTime = PlayTimeFilterData.MAX_RANGE;
			mUndefined = false;
		} else {
			mMinTime = filter.getMin();
			mMaxTime = filter.getMax();
			mUndefined = filter.isUndefined();
		}
	}

	private String intervalText(int number) {
		if (number == PlayTimeFilterData.MAX_RANGE) {
			return number + "+";
		} else {
			return "" + number;
		}
	}

	private String intervalText(int min, int max) {
		if (max == PlayTimeFilterData.MAX_RANGE) {
			return min + " - " + max + "+";
		} else {
			return min + " - " + max;
		}
	}
}
