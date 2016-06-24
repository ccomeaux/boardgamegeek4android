package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.PresentationUtils;

public class TimestampView extends TextView {
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec
	private static final int NO_PREFIX_OVERRIDE = 0;
	private Runnable timeHintUpdateRunnable = null;

	private boolean isForumTimeStamp;
	private String prefix;
	private String defaultMessage;
	private boolean hideWhenEmpty;

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
			prefix = a.getString(R.styleable.TimestampView_prefix);
			defaultMessage = a.getString(R.styleable.TimestampView_emptyMessage);
			hideWhenEmpty = a.getBoolean(R.styleable.TimestampView_hideWhenEmpty, false);
		} finally {
			a.recycle();
		}
	}

	public void setTimestamp(final long timestamp) {
		setTimestampText(timestamp, NO_PREFIX_OVERRIDE);
	}

	public void setTimestamp(final long timestamp, @StringRes final int prefix) {
		setTimestampText(timestamp, prefix);
	}

	private void setTimestampText(final long timestamp, @StringRes final int prefix) {
		if (timestamp == 0) {
			if (hideWhenEmpty) setVisibility(View.GONE);
			setText(defaultMessage);
		} else {
			if (hideWhenEmpty) setVisibility(View.VISIBLE);
			timeHintUpdateRunnable = new Runnable() {
				@Override
				public void run() {
					final CharSequence formattedTimestamp = PresentationUtils.formatTimestamp(getContext(), timestamp, isForumTimeStamp);
					if (prefix != NO_PREFIX_OVERRIDE) {
						setText(getContext().getString(prefix, formattedTimestamp));
					} else if (!TextUtils.isEmpty(TimestampView.this.prefix)) {
						setText(String.format(TimestampView.this.prefix, formattedTimestamp));
					} else {
						setText(formattedTimestamp);
					}
					postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);
				}
			};
			post(timeHintUpdateRunnable);
		}
	}
}
