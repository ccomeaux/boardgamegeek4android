package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.PresentationUtils;

public class TimestampView extends TextView {
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec
	private boolean isForumTimeStamp;
	private Runnable timeHintUpdateRunnable = null;

	public TimestampView(Context context) {
		super(context);
		init(context, null, 0);
	}

	public TimestampView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, 0);
	}

	public TimestampView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs, defStyleAttr);
	}

	private void init(Context context, AttributeSet attrs, int defStyleAttr) {
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TimestampView, defStyleAttr, 0);
		try {
			isForumTimeStamp = a.getBoolean(R.styleable.TimestampView_isForumTimestamp, false);
		} finally {
			a.recycle();
		}
	}

	public void setTimestamp(final long timestamp, @StringRes final int prefix) {
		if (timestamp == 0) {
			setText(R.string.text_not_available);
		} else {
			timeHintUpdateRunnable = new Runnable() {
				@Override
				public void run() {
					setText(getContext().getString(prefix, PresentationUtils.formatTimestamp(getContext(), timestamp, isForumTimeStamp)));
					postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);
				}
			};
			post(timeHintUpdateRunnable);
		}
	}
}
