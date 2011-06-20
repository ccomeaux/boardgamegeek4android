package com.boardgamegeek.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.PlayTimeFilterData;
import com.boardgamegeek.ui.CollectionActivity;
import com.boardgamegeek.ui.widget.DualSliderView;
import com.boardgamegeek.ui.widget.DualSliderView.KnobValuesChangedListener;

public class PlayTimeFilter {

	private int mMinTime;
	private int mMaxTime;
	private boolean mUndefined;

	public void createDialog(final CollectionActivity activity, PlayTimeFilterData filter) {
		initValues(filter);

		AlertDialog.Builder builder;
		AlertDialog alertDialog;

		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_play_time, (ViewGroup) activity.findViewById(R.id.layout_root));

		final TextView textInterval = (TextView) layout.findViewById(R.id.text_interval);
		final DualSliderView sliderView = (DualSliderView) layout.findViewById(R.id.num_players_slider);
		final CheckBox checkbox = (CheckBox) layout.findViewById(R.id.undefined_checkbox);

		sliderView.setRange(PlayTimeFilterData.MIN_RANGE, PlayTimeFilterData.MAX_RANGE);
		sliderView.setStartKnobValue(mMinTime);
		sliderView.setEndKnobValue(mMaxTime);
		checkbox.setChecked(mUndefined);

		Bitmap knobImage = BitmapFactory.decodeResource(activity.getResources(), R.drawable.knob);

		sliderView.setOnKnobValuesChangedListener(new KnobValuesChangedListener() {
			@Override
			public void onValuesChanged(boolean knobStartChanged, boolean knobEndChanged, int knobStart, int knobEnd) {
				if (!sliderView.isSecondThumbEnabled() && knobEndChanged) {
					textInterval.setText(intervalText(knobEnd));
				} else if (knobStartChanged || knobEndChanged) {
					if (knobStart == knobEnd) {
						textInterval.setText(intervalText(knobEnd));
					} else if (knobStart < knobEnd) {
						textInterval.setText(intervalText(knobStart, knobEnd));
					} else {
						textInterval.setText(intervalText(knobEnd, knobStart));
					}
				}
			}
		});

		builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.menu_play_time);
		builder.setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				CollectionFilterData filter = new CollectionFilterData().id(R.id.menu_play_time);
				activity.removeFilter(filter);
			}
		}).setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int id) {
				// Sliders can be on either side so need to check which one is
				// smaller
				if (sliderView.getFirstKnobValue() < sliderView.getSecondKnobValue()) {
					mMinTime = sliderView.getFirstKnobValue();
					mMaxTime = sliderView.getSecondKnobValue();
				} else {
					mMinTime = sliderView.getSecondKnobValue();
					mMaxTime = sliderView.getFirstKnobValue();
				}
				mUndefined = checkbox.isChecked();

				PlayTimeFilterData filter = new PlayTimeFilterData(activity, mMinTime, mMaxTime, mUndefined);
				activity.addFilter(filter);
			}
		});

		builder.setView(layout);
		alertDialog = builder.create();

		// we use the sizes for the slider
		LayoutParams params = sliderView.getLayoutParams();
		params.width = alertDialog.getWindow().getAttributes().width;
		params.height = 2 * knobImage.getHeight();
		sliderView.setLayoutParams(params);

		alertDialog.show();
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
