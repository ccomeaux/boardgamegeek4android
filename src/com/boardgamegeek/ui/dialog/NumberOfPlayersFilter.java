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
import com.boardgamegeek.data.CollectionFilter;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.CollectionActivity;
import com.boardgamegeek.ui.widget.DualSliderView;
import com.boardgamegeek.ui.widget.DualSliderView.KnobValuesChangedListener;

public class NumberOfPlayersFilter {

	private static final int MIN_RANGE = 0;
	private static final int MAX_RANGE = 12;

	private int mMinPlayers = MIN_RANGE;
	private int mMaxPlayers = MAX_RANGE;

	public void createDialog(final CollectionActivity activity) {
		AlertDialog.Builder builder;
		AlertDialog alertDialog;

		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater
				.inflate(R.layout.dialog_num_players, (ViewGroup) activity.findViewById(R.id.layout_root));

		final TextView textInterval = (TextView) layout.findViewById(R.id.text_interval);
		final DualSliderView sliderView = (DualSliderView) layout.findViewById(R.id.num_players_slider);

		sliderView.setRange(MIN_RANGE, MAX_RANGE);
		sliderView.setStartKnobValue(mMinPlayers);
		sliderView.setEndKnobValue(mMaxPlayers);

		Bitmap knobImage = BitmapFactory.decodeResource(activity.getResources(), R.drawable.knob);

		sliderView.setOnKnobValuesChangedListener(new KnobValuesChangedListener() {
			@Override
			public void onValuesChanged(boolean knobStartChanged, boolean knobEndChanged, int knobStart, int knobEnd) {
				if (!sliderView.isSecondThumbEnabled() && knobEndChanged)
					textInterval.setText("" + knobEnd);
				else if (knobStartChanged || knobEndChanged) {
					if (knobStart == knobEnd) {
						textInterval.setText("" + knobEnd);
					} else if (knobStart < knobEnd) {
						textInterval.setText(knobStart + " - " + knobEnd);
					} else {
						textInterval.setText(knobEnd + " - " + knobStart);
					}
				}
			}
		});

		builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.menu_number_of_players);
		builder.setNegativeButton("Clear", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				CollectionFilter filter = new CollectionFilter().id(R.id.menu_number_of_players);
				activity.removeFilter(filter);
			}
		}).setPositiveButton("Set", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int id) {
				// Sliders can be on either side so need to check which one is
				// smaller
				if (sliderView.getFirstKnobValue() < sliderView.getSecondKnobValue()) {
					mMinPlayers = sliderView.getFirstKnobValue();
					mMaxPlayers = sliderView.getSecondKnobValue();
				} else {
					mMinPlayers = sliderView.getSecondKnobValue();
					mMaxPlayers = sliderView.getFirstKnobValue();
				}
				String startValue = String.valueOf(mMinPlayers);
				String endValue = String.valueOf(mMaxPlayers);

				CollectionFilter filter = null;
				if (sliderView.isSecondThumbEnabled()) {
					filter = new CollectionFilter()
							.selection(Games.MIN_PLAYERS + "<=? AND " + Games.MAX_PLAYERS + ">=?")
							.selectionargs(endValue, startValue).id(R.id.menu_number_of_players);
					if (mMinPlayers == mMaxPlayers) {
						filter.name(endValue + " Players");
					} else {
						filter.name(startValue + "-" + endValue + " Players");
					}
				} else {
					filter = new CollectionFilter().name(endValue + " Players")
							.selection(Games.MIN_PLAYERS + "=? AND " + Games.MAX_PLAYERS + "=?")
							.selectionargs(endValue, endValue).id(R.id.menu_number_of_players);
				}

				activity.addFilter(filter);
			}
		});

		final CheckBox checkbox = (CheckBox) layout.findViewById(R.id.exact_checkbox);
		checkbox.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				sliderView.setSecondThumbEnabled(!checkbox.isChecked());
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
}
