package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.interfaces.CollectionView;
import com.boardgamegeek.ui.widget.RangeSeekBar;
import com.boardgamegeek.ui.widget.RangeSeekBar.OnRangeSeekBarChangeListener;

public abstract class SliderFilter {
	private Integer low;
	private Integer high;
	private TextView explanationView;
	private TextView rangeDescriptionView;
	private RangeSeekBar<Integer> rangeSeekBar;
	private CheckBox checkBox;

	public void createDialog(final Context context, final CollectionView view, CollectionFilterer filter) {
		initValues(filter);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_slider_filter, null);

		rangeDescriptionView = (TextView) layout.findViewById(R.id.range_description);
		checkBox = (CheckBox) layout.findViewById(R.id.checkbox);
		explanationView = (TextView) layout.findViewById(R.id.explanation);
		FrameLayout container = (FrameLayout) layout.findViewById(R.id.range_seek_bar_container);
		rangeSeekBar = new RangeSeekBar<>(getAbsoluteMin(), getAbsoluteMax(), context);
		container.addView(rangeSeekBar);

		low = getMin();
		high = getMax();

		initSlider();
		initCheckbox();
		initExplanation();
		rangeDescriptionView.setText(intervalText(low, high));

		AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(getTitleId())
			.setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					view.removeFilter(getNegativeData());
				}
			}).setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					captureForm(low, high, checkBox.isChecked());
					view.addFilter(getPositiveData(context));
				}
			}).setView(layout);

		builder.create().show();
	}

	private void initSlider() {
		rangeSeekBar.setNotifyWhileDragging(true);
		rangeSeekBar.setSelectedMinValue(low);
		rangeSeekBar.setSelectedMaxValue(high);

		rangeSeekBar.setOnRangeSeekBarChangeListener(new OnRangeSeekBarChangeListener<Integer>() {
			@Override
			public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
				low = minValue;
				high = maxValue;
				CharSequence text;
				if (minValue.equals(maxValue)) {
					text = intervalText(minValue);
				} else {
					text = intervalText(minValue, maxValue);
				}
				rangeDescriptionView.setText(text);
			}
		});
	}

	private void initCheckbox() {
		//noinspection ResourceType
		checkBox.setVisibility(getCheckboxVisibility());
		checkBox.setText(getCheckboxTextId());
		checkBox.setChecked(isChecked());
	}

	private void initExplanation() {
		if (getDescriptionId() == -1) {
			explanationView.setVisibility(View.GONE);
		} else {
			explanationView.setText(getDescriptionId());
			explanationView.setVisibility(View.VISIBLE);
		}
	}

	protected abstract void initValues(CollectionFilterer filter);

	@StringRes
	protected abstract int getTitleId();

	protected abstract CollectionFilterer getNegativeData();

	protected abstract CollectionFilterer getPositiveData(final Context context);

	protected abstract int getMin();

	protected abstract int getMax();

	protected abstract boolean isChecked();

	protected int getCheckboxVisibility() {
		return View.VISIBLE;
	}

	@StringRes
	protected int getCheckboxTextId() {
		return R.string.include_missing_values;
	}

	@StringRes
	protected int getDescriptionId() {
		return -1;
	}

	protected abstract int getAbsoluteMin();

	protected abstract int getAbsoluteMax();

	protected abstract void captureForm(int min, int max, boolean checkbox);

	protected abstract String intervalText(int number);

	protected abstract String intervalText(int min, int max);
}
