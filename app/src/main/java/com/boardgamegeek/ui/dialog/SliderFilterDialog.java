package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.appyvet.rangebar.RangeBar;
import com.appyvet.rangebar.RangeBar.OnRangeBarChangeListener;
import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.interfaces.CollectionView;
import com.boardgamegeek.util.StringUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public abstract class SliderFilterDialog implements CollectionFilterDialog {
	private Integer low;
	private Integer high;
	@SuppressWarnings("unused") @InjectView(R.id.explanation) TextView explanationView;
	@SuppressWarnings("unused") @InjectView(R.id.range_description) TextView rangeDescriptionView;
	@SuppressWarnings("unused") @InjectView(R.id.checkbox) CheckBox checkBox;
	@SuppressWarnings("unused") @InjectView(R.id.range_bar) RangeBar rangeBar;

	public void createDialog(final Context context, final CollectionView view, CollectionFilterer filter) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_slider_filter, null);
		ButterKnife.inject(this, layout);

		InitialValues initialValues = initValues(filter);
		low = initialValues.min;
		high = initialValues.max;

		initSlider();

		checkBox.setVisibility(getCheckboxVisibility());
		checkBox.setText(getCheckboxTextId());
		checkBox.setChecked(initialValues.isChecked);

		initExplanation();
		rangeDescriptionView.setText(intervalText(low, high));

		AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(getTitleId())
			.setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					view.removeFilter(getType(context));
				}
			}).setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					view.addFilter(getPositiveData(context, low, high, checkBox.isChecked()));
				}
			}).setView(layout);

		builder.create().show();
	}

	private void initSlider() {
		rangeBar.setTickStart(getAbsoluteMin());
		rangeBar.setTickEnd(getAbsoluteMax());
		rangeBar.setRangePinsByValue(low, high);

		rangeBar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
			@Override
			public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex, String leftPinValue, String rightPinValue) {
				adjustSeekBar(leftPinValue, rightPinValue);
			}
		});
	}

	@OnClick(R.id.min_up)
	public void onMinUpClick(View v) {
		if (rangeBar.getLeftIndex() < rangeBar.getTickCount() - 1) {
			rangeBar.setRangePinsByIndices(rangeBar.getLeftIndex() + 1, rangeBar.getRightIndex());
			adjustSeekBar(rangeBar.getLeftPinValue(), rangeBar.getRightPinValue());
		}
	}

	@OnClick(R.id.min_down)
	public void onMinDownClick(View v) {
		if (rangeBar.getLeftIndex() > 0) {
			rangeBar.setRangePinsByIndices(rangeBar.getLeftIndex() - 1, rangeBar.getRightIndex());
			adjustSeekBar(rangeBar.getLeftPinValue(), rangeBar.getRightPinValue());
		}
	}

	@OnClick(R.id.max_up)
	public void onMaxUpClick(View v) {
		if (rangeBar.getRightIndex() < rangeBar.getTickCount() - 1) {
			rangeBar.setRangePinsByIndices(rangeBar.getLeftIndex(), rangeBar.getRightIndex() + 1);
			adjustSeekBar(rangeBar.getLeftPinValue(), rangeBar.getRightPinValue());
		}
	}

	@OnClick(R.id.max_down)
	public void onMaxDownClick(View v) {
		if (rangeBar.getRightIndex() > 0) {
			rangeBar.setRangePinsByIndices(rangeBar.getLeftIndex(), rangeBar.getRightIndex() - 1);
			adjustSeekBar(rangeBar.getLeftPinValue(), rangeBar.getRightPinValue());
		}
	}

	private void adjustSeekBar(String minValue, String maxValue) {
		low = StringUtils.parseInt(minValue);
		high = StringUtils.parseInt(maxValue);
		CharSequence text;
		if (low.equals(high)) {
			text = intervalText(low);
		} else {
			text = intervalText(low, high);
		}
		rangeDescriptionView.setText(text);
	}

	private void initExplanation() {
		if (getDescriptionId() == -1) {
			explanationView.setVisibility(View.GONE);
		} else {
			explanationView.setText(getDescriptionId());
			explanationView.setVisibility(View.VISIBLE);
		}
	}

	protected abstract InitialValues initValues(CollectionFilterer filter);

	@StringRes
	protected abstract int getTitleId();

	protected abstract CollectionFilterer getPositiveData(final Context context, int min, int max, boolean checkbox);

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

	protected abstract String intervalText(int number);

	protected abstract String intervalText(int min, int max);

	class InitialValues {
		final int min;
		final int max;
		final boolean isChecked;

		InitialValues(int min, int max) {
			this.min = min;
			this.max = max;
			this.isChecked = false;
		}

		InitialValues(int min, int max, boolean isChecked) {
			this.min = min;
			this.max = max;
			this.isChecked = isChecked;
		}
	}
}
