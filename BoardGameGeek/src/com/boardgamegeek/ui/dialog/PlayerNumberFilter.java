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
import com.boardgamegeek.data.PlayerNumberFilterData;
import com.boardgamegeek.ui.CollectionActivity;
import com.boardgamegeek.ui.widget.DualSliderView;
import com.boardgamegeek.ui.widget.DualSliderView.KnobValuesChangedListener;

public class PlayerNumberFilter {

	private int mMinPlayers;
	private int mMaxPlayers;
	private boolean mExact;

	public void createDialog(final CollectionActivity activity, PlayerNumberFilterData filter) {
		initValues(filter);

		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater
				.inflate(R.layout.dialog_num_players, (ViewGroup) activity.findViewById(R.id.layout_root));

		final TextView textInterval = (TextView) layout.findViewById(R.id.text_interval);
		final DualSliderView sliderView = (DualSliderView) layout.findViewById(R.id.num_players_slider);
		final CheckBox checkbox = (CheckBox) layout.findViewById(R.id.exact_checkbox);

		sliderView.setRange(PlayerNumberFilterData.MIN_RANGE, PlayerNumberFilterData.MAX_RANGE);
		sliderView.setStartKnobValue(mMinPlayers);
		sliderView.setEndKnobValue(mMaxPlayers);
		sliderView.setSecondThumbEnabled(!mExact);
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

		checkbox.setChecked(mExact);
		checkbox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sliderView.setSecondThumbEnabled(!checkbox.isChecked());
			}
		});

		AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(R.string.menu_number_of_players)
				.setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.removeFilter(new PlayerNumberFilterData());
					}
				}).setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						// Sliders can be on either side so need to check which
						// one is smaller
						if (sliderView.getStartKnobValue() < sliderView.getEndKnobValue()) {
							mMinPlayers = sliderView.getStartKnobValue();
							mMaxPlayers = sliderView.getEndKnobValue();
						} else {
							mMinPlayers = sliderView.getEndKnobValue();
							mMaxPlayers = sliderView.getStartKnobValue();
						}
						mExact = checkbox.isChecked();

						activity.addFilter(new PlayerNumberFilterData(activity, mMinPlayers, mMaxPlayers, mExact));
					}
				}).setView(layout);

		builder.create().show();
	}

	private void initValues(PlayerNumberFilterData filter) {
		if (filter == null) {
			mMinPlayers = PlayerNumberFilterData.MIN_RANGE;
			mMaxPlayers = PlayerNumberFilterData.MAX_RANGE;
			mExact = false;
		} else {
			mMinPlayers = filter.getMin();
			mMaxPlayers = filter.getMax();
			mExact = filter.isExact();
		}
	}

	private String intervalText(int number) {
		return String.valueOf(number);
	}

	private String intervalText(int min, int max) {
		return min + " - " + max;
	}
}
