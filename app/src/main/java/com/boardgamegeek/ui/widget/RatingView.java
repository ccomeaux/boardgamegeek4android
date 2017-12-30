package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.dialog.NumberPadDialogFragment;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.MathUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.StringUtils;

import java.text.DecimalFormat;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hugo.weaving.DebugLog;

public class RatingView extends ForegroundLinearLayout {
	private static final DecimalFormat RATING_EDIT_FORMAT = new DecimalFormat("0.#");
	@BindView(R.id.rating_value) TextView ratingView;
	@BindView(R.id.rating_timestamp) TimestampView timestampView;
	@BindDimen(R.dimen.edit_row_height) int minHeight;
	private boolean hideWhenZero;
	private boolean isInEditMode;
	private FragmentActivity activity;
	private Listener listener;

	public interface Listener {
		void onRatingChanged(double rating);
	}

	public RatingView(Context context) {
		super(context);
		init(null);
	}

	public RatingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public void setOnChangeListener(FragmentActivity activity, Listener listener) {
		this.activity = activity;
		this.listener = listener;
	}

	private void init(AttributeSet attrs) {
		LayoutInflater.from(getContext()).inflate(R.layout.widget_rating, this, true);
		ButterKnife.bind(this);

		setVisibility(View.GONE);
		setGravity(Gravity.CENTER_VERTICAL);
		setMinimumHeight(minHeight);
		setOrientation(VERTICAL);
		PresentationUtils.setSelectableBackgroundBorderless(this);

		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.RatingView);
			try {
				hideWhenZero = a.getBoolean(R.styleable.RatingView_hideWhenZero, false);
			} finally {
				a.recycle();
			}
		}
	}

	public void setContent(double rating, long timestamp) {
		setRating(rating);
		timestampView.setTimestamp(timestamp);
		setEditMode();
	}

	private void setRating(double rating) {
		final double constrainedRating = MathUtils.constrain(rating, 0.0, 10.0);
		ratingView.setText(PresentationUtils.describePersonalRating(getContext(), constrainedRating));
		ratingView.setTag(constrainedRating);
		ColorUtils.setTextViewBackground(ratingView, ColorUtils.getRatingColor(constrainedRating));
	}

	public void enableEditMode(boolean enable) {
		isInEditMode = enable;
		setEditMode();
	}

	private void setEditMode() {
		if (isInEditMode) {
			setClickable(true);
			setVisibility(View.VISIBLE);
		} else {
			setClickable(false);
			if (hideWhenZero && getRating() == 0) {
				setVisibility(GONE);
			} else {
				setVisibility(VISIBLE);
			}
		}
	}

	private double getRating() {
		final Double rating = (Double) ratingView.getTag();
		if (rating == null) return 0.0;
		return rating;
	}

	@DebugLog
	@OnClick
	public void onClick() {
		String output = RATING_EDIT_FORMAT.format((double) ratingView.getTag());
		if ("0".equals(output)) output = "";
		final NumberPadDialogFragment fragment = NumberPadDialogFragment.newInstance(getContext().getString(R.string.rating), output);
		fragment.setMinValue(1.0);
		fragment.setMaxValue(10.0);
		fragment.setMaxMantissa(6);
		fragment.setOnDoneClickListener(new NumberPadDialogFragment.OnClickListener() {
			@Override
			public void onDoneClick(String output) {
				if (listener != null) {
					double rating = StringUtils.parseDouble(output);
					setRating(rating);
					listener.onRatingChanged(rating);
				}
			}
		});
		DialogUtils.showFragment(activity, fragment, "rating_dialog");
	}
}
