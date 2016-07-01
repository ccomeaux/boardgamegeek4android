package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.support.annotation.StringRes;
import android.text.Html;
import android.text.SpannedString;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.PresentationUtils;

import icepick.Icepick;
import icepick.State;

public class TimestampView extends TextView {
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec
	private Runnable timeHintUpdateRunnable = null;

	private boolean isForumTimeStamp;
	private String defaultMessage;
	private boolean hideWhenEmpty;
	@State CharSequence format;
	@State long timestamp;
	@State String formatArg;

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

	@Override
	public Parcelable onSaveInstanceState() {
		return Icepick.saveInstanceState(this, super.onSaveInstanceState());
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		super.onRestoreInstanceState(Icepick.restoreInstanceState(this, state));
	}

	private void init(Context context, AttributeSet attrs, int defStyleAttr) {
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TimestampView, defStyleAttr, 0);
		try {
			isForumTimeStamp = a.getBoolean(R.styleable.TimestampView_isForumTimestamp, false);
			format = a.getText(R.styleable.TimestampView_format);
			defaultMessage = a.getString(R.styleable.TimestampView_emptyMessage);
			hideWhenEmpty = a.getBoolean(R.styleable.TimestampView_hideWhenEmpty, false);
		} finally {
			a.recycle();
		}
		setMaxLines(1);
	}

	public void setFormat(@StringRes final int formatResId) {
		format = getContext().getText(formatResId);
		setTimestampText();
	}

	public void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
		setTimestampText();
	}

	public void setFormatArg(String formatArg) {
		this.formatArg = formatArg;
		setTimestampText();
	}

	private void setTimestampText() {
		removeCallbacks(timeHintUpdateRunnable);
		if (timestamp == 0) {
			if (hideWhenEmpty) setVisibility(View.GONE);
			setText(defaultMessage);
		} else {
			if (hideWhenEmpty) setVisibility(View.VISIBLE);
			timeHintUpdateRunnable = new Runnable() {
				@Override
				public void run() {
					final CharSequence formattedTimestamp = PresentationUtils.formatTimestamp(getContext(), timestamp, isForumTimeStamp);
					if (!TextUtils.isEmpty(format)) {
						setText(Html.fromHtml(String.format(Html.toHtml(new SpannedString(format)), formattedTimestamp, formatArg)));
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
