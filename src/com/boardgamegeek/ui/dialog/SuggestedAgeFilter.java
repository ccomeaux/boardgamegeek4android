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
import com.boardgamegeek.data.SuggestedAgeFilterData;
import com.boardgamegeek.ui.CollectionActivity;
import com.boardgamegeek.ui.widget.DualSliderView;
import com.boardgamegeek.ui.widget.DualSliderView.KnobValuesChangedListener;

public class SuggestedAgeFilter {
	private int mMinAge;
	private int mMaxAge;
	private boolean mUndefined;

	public void createDialog(final CollectionActivity activity, SuggestedAgeFilterData filter) {
		initValues(filter);

		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		// TODO: change layout
		View layout = inflater.inflate(R.layout.dialog_play_time, (ViewGroup) activity.findViewById(R.id.layout_root));

		final TextView textInterval = (TextView) layout.findViewById(R.id.text_interval);
		final DualSliderView sliderView = (DualSliderView) layout.findViewById(R.id.num_players_slider);
		final CheckBox checkbox = (CheckBox) layout.findViewById(R.id.undefined_checkbox);

		initSlider(activity, textInterval, sliderView);
		checkbox.setChecked(mUndefined);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(R.string.menu_suggested_age)
				.setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.removeFilter(new SuggestedAgeFilterData());
					}
				}).setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						captureForm(sliderView, checkbox);
						activity.addFilter(new SuggestedAgeFilterData(activity, mMinAge, mMaxAge, mUndefined));
					}

					private void captureForm(final DualSliderView sliderView, final CheckBox checkbox) {
						int start = sliderView.getStartKnobValue();
						int end = sliderView.getEndKnobValue();
						if (start < end) {
							mMinAge = start;
							mMaxAge = end;
						} else {
							mMinAge = end;
							mMaxAge = start;
						}
						mUndefined = checkbox.isChecked();
					}
				}).setView(layout);

		builder.create().show();
	}

	private void initSlider(final CollectionActivity activity, final TextView textInterval,
			final DualSliderView sliderView) {
		sliderView.setRange(SuggestedAgeFilterData.MIN_RANGE, SuggestedAgeFilterData.MAX_RANGE);
		sliderView.setStartKnobValue(mMinAge);
		sliderView.setEndKnobValue(mMaxAge);

		sliderView.setOnKnobValuesChangedListener(new KnobValuesChangedListener() {
			@Override
			public void onValuesChanged(boolean knobStartChanged, boolean knobEndChanged, int knobStart, int knobEnd) {
				String text = "";
				if (!sliderView.isSecondThumbEnabled() && knobEndChanged) {
					text = intervalText(knobEnd);
				} else if (knobStartChanged || knobEndChanged) {
					if (knobStart == knobEnd) {
						text = intervalText(knobEnd);
					} else if (knobStart < knobEnd) {
						text = intervalText(knobStart, knobEnd);
					} else {
						text = intervalText(knobEnd, knobStart);
					}
				}
				textInterval.setText(text);
			}
		});
	}

	private void initValues(SuggestedAgeFilterData filter) {
		if (filter == null) {
			mMinAge = SuggestedAgeFilterData.MIN_RANGE;
			mMaxAge = SuggestedAgeFilterData.MAX_RANGE;
			mUndefined = false;
		} else {
			mMinAge = filter.getMin();
			mMaxAge = filter.getMax();
			mUndefined = filter.isUndefined();
		}
	}

	private String intervalText(int number) {
		if (number == SuggestedAgeFilterData.MAX_RANGE) {
			return number + "+";
		} else {
			return "" + number;
		}
	}

	private String intervalText(int min, int max) {
		if (max == SuggestedAgeFilterData.MAX_RANGE) {
			return min + " - " + max + "+";
		} else {
			return min + " - " + max;
		}
	}
}
