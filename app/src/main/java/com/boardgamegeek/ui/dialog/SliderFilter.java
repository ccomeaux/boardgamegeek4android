package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
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
	private Integer mMin;
	private Integer mMax;
	private TextView mDescriptionView;
	private TextView mTextInterval;
	private RangeSeekBar<Integer> mRangeSeekBar;
	private CheckBox mCheckBox;

	public void createDialog(final Context context, final CollectionView view, CollectionFilterer filter) {
		initValues(filter);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_slider_filter, null);

		mDescriptionView = (TextView) layout.findViewById(R.id.slider_filter_description);
		mTextInterval = (TextView) layout.findViewById(R.id.slider_filter_text);
		mCheckBox = (CheckBox) layout.findViewById(R.id.slider_filter_checkbox);
		FrameLayout container = (FrameLayout) layout.findViewById(R.id.slider_filter_rangeseekbar_container);
		mRangeSeekBar = new RangeSeekBar<>(getAbsoluteMin(), getAbsoluteMax(), context);
		container.addView(mRangeSeekBar);

		mMin = getMin();
		mMax = getMax();

		initDescription();
		initSlider();
		initCheckbox();
		mTextInterval.setText(intervalText(getMin(), getMax()));

		AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(getTitleId())
			.setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					view.removeFilter(getNegativeData());
				}
			}).setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					captureForm(mMin, mMax, mCheckBox.isChecked());
					view.addFilter(getPositiveData(context));
				}
			}).setView(layout);

		builder.create().show();
	}

	private void initDescription() {
		if (getDescriptionId() == -1) {
			mDescriptionView.setVisibility(View.GONE);
		} else {
			mDescriptionView.setText(getDescriptionId());
			mDescriptionView.setVisibility(View.VISIBLE);
		}
	}

	private void initSlider() {
		mRangeSeekBar.setNotifyWhileDragging(true);
		mRangeSeekBar.setSelectedMinValue(getMin());
		mRangeSeekBar.setSelectedMaxValue(getMax());

		mRangeSeekBar.setOnRangeSeekBarChangeListener(new OnRangeSeekBarChangeListener<Integer>() {
			@Override
			public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
				mMin = minValue;
				mMax = maxValue;
				CharSequence text;
				if (minValue.equals(maxValue)) {
					text = intervalText(minValue);
				} else {
					text = intervalText(minValue, maxValue);
				}
				mTextInterval.setText(text);
			}
		});
	}

	private void initCheckbox() {
		//noinspection ResourceType
		mCheckBox.setVisibility(getCheckboxVisibility());
		mCheckBox.setText(getCheckboxTextId());
		mCheckBox.setChecked(isChecked());
	}

	protected abstract void initValues(CollectionFilterer filter);

	protected abstract int getTitleId();

	protected abstract CollectionFilterer getNegativeData();

	protected abstract CollectionFilterer getPositiveData(final Context context);

	protected abstract int getMin();

	protected abstract int getMax();

	protected abstract boolean isChecked();

	protected int getCheckboxVisibility() {
		return View.VISIBLE;
	}

	protected int getCheckboxTextId() {
		return R.string.include_missing_values;
	}

	protected int getDescriptionId() {
		return -1;
	}

	protected abstract int getAbsoluteMin();

	protected abstract int getAbsoluteMax();

	protected abstract void captureForm(int min, int max, boolean checkbox);

	protected abstract String intervalText(int number);

	protected abstract String intervalText(int min, int max);
}
