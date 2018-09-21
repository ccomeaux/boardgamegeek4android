package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build.VERSION;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.StringRes;
import android.support.v4.view.ViewCompat;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.PresentationUtils;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;

public class TimestampView extends TextView {
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec
	private Runnable timeHintUpdateRunnable = null;

	private long timestamp;
	private String format;
	private String formatArg;

	private boolean isForumTimeStamp;
	private boolean includeTime;
	private String defaultMessage;
	private boolean hideWhenEmpty;

	public TimestampView(Context context) {
		this(context, null);
	}

	public TimestampView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.textViewStyle);
	}

	public TimestampView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TimestampView, defStyleAttr, 0);
		try {
			isForumTimeStamp = a.getBoolean(R.styleable.TimestampView_isForumTimestamp, false);
			includeTime = a.getBoolean(R.styleable.TimestampView_includeTime, false);
			format = a.getString(R.styleable.TimestampView_format);
			defaultMessage = a.getString(R.styleable.TimestampView_emptyMessage);
			hideWhenEmpty = a.getBoolean(R.styleable.TimestampView_hideWhenEmpty, false);
		} finally {
			a.recycle();
		}
		if (VERSION.SDK_INT >= JELLY_BEAN) {
			int maxLines = getMaxLines();
			if (maxLines == -1 || maxLines == Integer.MAX_VALUE) {
				setMaxLines(1);
			}
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		post(timeHintUpdateRunnable);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		removeCallbacks(timeHintUpdateRunnable);
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);
		ss.timestamp = timestamp;
		ss.format = format;
		ss.formatArg = formatArg;
		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		timestamp = ss.timestamp;
		format = ss.format;
		formatArg = ss.formatArg;
	}

	public void setFormat(@StringRes int formatResId) {
		this.format = getContext().getString(formatResId);
		setTimestampText();
	}

	public void setFormat(String format) {
		this.format = format;
		setTimestampText();
	}

	public void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
		setTimestampText();
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setFormatArg(String formatArg) {
		this.formatArg = formatArg;
		setTimestampText();
	}

	private void setTimestampText() {
		removeCallbacks(timeHintUpdateRunnable);
		if (timestamp <= 0) {
			if (hideWhenEmpty) setVisibility(View.GONE);
			setText(defaultMessage);
		} else {
			if (hideWhenEmpty) setVisibility(View.VISIBLE);
			timeHintUpdateRunnable = new Runnable() {
				@SuppressWarnings("deprecation")
				@Override
				public void run() {
					if (!ViewCompat.isAttachedToWindow(TimestampView.this)) return;
					final CharSequence formattedTimestamp = PresentationUtils.formatTimestamp(getContext(), timestamp, isForumTimeStamp, includeTime);
					if (!TextUtils.isEmpty(format)) {
						String format = Html.toHtml(new SpannedString(TimestampView.this.format));
						Spanned text = Html.fromHtml(String.format(format, formattedTimestamp, formatArg));
						setText(PresentationUtils.trimTrailingWhitespace(text));
					} else {
						setText(formattedTimestamp);
					}
					postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);
				}
			};
			post(timeHintUpdateRunnable);
		}
	}

	private static class SavedState extends BaseSavedState {
		long timestamp;
		String format;
		String formatArg;

		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			format = in.readString();
			timestamp = in.readLong();
			formatArg = in.readString();
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeLong(timestamp);
			out.writeString(format);
			out.writeString(formatArg);
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}
}
