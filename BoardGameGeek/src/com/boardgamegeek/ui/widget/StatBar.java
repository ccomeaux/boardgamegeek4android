package com.boardgamegeek.ui.widget;

import java.text.NumberFormat;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.boardgamegeek.R;

public class StatBar extends RelativeLayout {
	private ProgressBar mProgressBar;
	private TextView mTextView;
	private NumberFormat mFormat = NumberFormat.getInstance();

	public StatBar(Context context) {
		this(context, null);
	}

	public StatBar(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public StatBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		mProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
		mProgressBar.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, (int) TypedValue
				.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics())));
		mProgressBar.setProgressDrawable(context.getResources().getDrawable(R.drawable.progress));

		mTextView = new TextView(context);
		mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_small));
		mTextView.setTextColor(Color.WHITE);
		mTextView.setGravity(Gravity.CENTER);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		lp.addRule(ALIGN_TOP, mProgressBar.getId());
		lp.addRule(ALIGN_BOTTOM, mProgressBar.getId());
		mTextView.setLayoutParams(lp);

		addView(mProgressBar);
		addView(mTextView);
	}

	public void setBar(int id, double progress) {
		setBar(id, progress, 10.0);
	}

	public void setBar(int id, double progress, double max) {
		setBar(id, progress, max, 0.0);
	}

	public void setBar(int id, double progress, double max, double min) {
		mTextView.setText(String.format(getContext().getResources().getString(id), mFormat.format(progress)));
		mProgressBar.setMax((int) ((max - min) * 1000));
		mProgressBar.setProgress((int) ((progress - min) * 1000));
	}
}
