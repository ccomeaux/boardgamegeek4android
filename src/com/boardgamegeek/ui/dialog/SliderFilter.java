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
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.ui.CollectionActivity;
import com.boardgamegeek.ui.widget.DualSliderView;
import com.boardgamegeek.ui.widget.DualSliderView.KnobValuesChangedListener;

public abstract class SliderFilter {
	public void createDialog(final CollectionActivity activity, CollectionFilterData filter) {
		initValues(filter);

		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_slider_filter,
				(ViewGroup) activity.findViewById(R.id.layout_root));

		final TextView textInterval = (TextView) layout.findViewById(R.id.slider_filter_text);
		final DualSliderView sliderView = (DualSliderView) layout.findViewById(R.id.slider_filter_slider);
		final CheckBox checkbox = (CheckBox) layout.findViewById(R.id.slider_filter_checkbox);

		initSlider(activity, textInterval, sliderView);
		initCheckbox(checkbox, sliderView);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(getTitleId())
				.setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.removeFilter(getNegativeData());
					}
				}).setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						captureForm(sliderView.getMinKnobValue(), sliderView.getMaxKnobValue(), checkbox.isChecked());
						activity.addFilter(getPositiveData(activity));
					}
				}).setView(layout);

		builder.create().show();
	}

	private void initSlider(final CollectionActivity activity, final TextView textInterval,
			final DualSliderView sliderView) {
		sliderView.setRange(getMin(), getMax(), getStep());
		sliderView.setStartKnobValue(getStart());
		sliderView.setEndKnobValue(getEnd());
		sliderView.setLineSpacing(getLineSpacing());
		if (getCheckboxDisablesSecondThumb()) {
			sliderView.setSecondThumbEnabled(!getCheckbox());
		}

		sliderView.setOnKnobValuesChangedListener(new KnobValuesChangedListener() {
			@Override
			public void onValuesChanged(boolean knobStartChanged, boolean knobEndChanged, int knobStart, int knobEnd) {
				String text = "";
				if (!sliderView.isSecondThumbEnabled() && knobEndChanged) {
					text = intervalText(knobEnd);
				} else if (knobStartChanged || knobEndChanged) {
					if (knobStart == knobEnd) {
						text = intervalText(knobEnd);
					} else {
						text = intervalText(Math.min(knobStart, knobEnd), Math.max(knobStart, knobEnd));
					}
				}
				textInterval.setText(text);
			}
		});
	}

	private void initCheckbox(final CheckBox checkbox, final DualSliderView sliderView) {
		checkbox.setVisibility(getCheckboxVisibility());
		checkbox.setText(getCheckboxTextId());
		checkbox.setChecked(getCheckbox());
		if (getCheckboxDisablesSecondThumb()) {
			checkbox.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					sliderView.setSecondThumbEnabled(!checkbox.isChecked());
				}
			});
		}
	}

	protected abstract void initValues(CollectionFilterData filter);

	protected abstract int getTitleId();

	protected abstract CollectionFilterData getNegativeData();

	protected abstract CollectionFilterData getPositiveData(final CollectionActivity activity);

	protected abstract int getStart();

	protected abstract int getEnd();

	protected abstract boolean getCheckbox();

	protected int getCheckboxVisibility() {
		return View.VISIBLE;
	}

	protected int getCheckboxTextId() {
		return R.string.include_missing_values;
	}

	protected boolean getCheckboxDisablesSecondThumb() {
		return false;
	}

	protected double getStep() {
		return 1.0;
	}

	protected int getLineSpacing() {
		return 1;
	}

	protected abstract int getMin();

	protected abstract int getMax();

	protected abstract void captureForm(int min, int max, boolean checkbox);

	protected abstract String intervalText(int number);

	protected abstract String intervalText(int min, int max);
}
