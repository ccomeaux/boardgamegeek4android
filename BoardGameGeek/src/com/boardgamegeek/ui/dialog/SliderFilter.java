package com.boardgamegeek.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.CollectionView;
import com.boardgamegeek.ui.widget.DualSliderView;
import com.boardgamegeek.ui.widget.DualSliderView.KnobValuesChangedListener;

public abstract class SliderFilter {
	public void createDialog(final Context context, final CollectionView view, CollectionFilterData filter) {
		initValues(filter);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_slider_filter, null);

		final TextView descriptionView = (TextView) layout.findViewById(R.id.slider_filter_description);
		final TextView textInterval = (TextView) layout.findViewById(R.id.slider_filter_text);
		final DualSliderView sliderView = (DualSliderView) layout.findViewById(R.id.slider_filter_slider);
		final CheckBox checkbox = (CheckBox) layout.findViewById(R.id.slider_filter_checkbox);

		initDescription(descriptionView);
		initSlider(textInterval, sliderView);
		initCheckbox(checkbox, sliderView);

		AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(getTitleId())
			.setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					view.removeFilter(getNegativeData());
				}
			}).setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					captureForm(sliderView.getMinKnobValue(), sliderView.getMaxKnobValue(), checkbox.isChecked());
					view.addFilter(getPositiveData(context));
				}
			}).setView(layout);

		builder.create().show();
	}

	private void initDescription(TextView view) {
		if (getDescriptionId() == -1) {
			view.setVisibility(View.GONE);
		} else {
			view.setText(getDescriptionId());
			view.setVisibility(View.VISIBLE);
		}
	}

	private void initSlider(final TextView textInterval, final DualSliderView sliderView) {
		sliderView.setStartOffset(getStartOffset());
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

	protected int getStartOffset() {
		return 0;
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

	protected abstract CollectionFilterData getPositiveData(final Context context);

	protected abstract int getStart();

	protected abstract int getEnd();

	protected abstract boolean getCheckbox();

	protected int getCheckboxVisibility() {
		return View.VISIBLE;
	}

	protected int getCheckboxTextId() {
		return R.string.include_missing_values;
	}

	protected int getDescriptionId() {
		return -1;
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
