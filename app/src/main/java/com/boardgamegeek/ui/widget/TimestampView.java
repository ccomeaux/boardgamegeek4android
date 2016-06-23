package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.widget.TextView;

import com.boardgamegeek.util.DateTimeUtils;

public class TimestampView extends TextView {
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec
	private Runnable timeHintUpdateRunnable = null;

	public TimestampView(Context context) {
		super(context);
	}

	public TimestampView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TimestampView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void setTimestamp(final long timestamp, @StringRes final int prefix) {
		timeHintUpdateRunnable = new Runnable() {
			@Override
			public void run() {
				setText(getContext().getString(prefix, DateTimeUtils.formatForumDate(getContext(), timestamp)));
				postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);
			}
		};
		post(timeHintUpdateRunnable);
	}
}
