package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.DateTimeUtils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PlayStatView extends TableRow {
	@Bind(R.id.label) TextView labelView;
	@Bind(R.id.value) TextView valueView;
	@Bind(R.id.info) ImageView infoImageView;
	@Bind(R.id.label_container) View container;
	private AlertDialog.Builder builder;

	public PlayStatView(Context context) {
		super(context);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.widget_play_stat, this);
		ButterKnife.bind(this);
	}

	public void setLabel(CharSequence text) {
		labelView.setText(text);
	}

	public void setLabel(@StringRes int textId) {
		labelView.setText(textId);
	}

	public void setValue(CharSequence text) {
		valueView.setText(text);
	}

	public void setInfoText(@StringRes int textId) {
		setInfoText(getContext().getString(textId));
	}

	public void setInfoText(String text) {
		infoImageView.setVisibility(View.VISIBLE);
		setClickBackground();
		final SpannableString s = new SpannableString(text);
		Linkify.addLinks(s, Linkify.ALL);
		builder = new AlertDialog.Builder(getContext());
		builder.setTitle(labelView.getText()).setMessage(s);
	}

	@OnClick(R.id.label_container)
	public void onInfoClick() {
		if (builder != null) {
			AlertDialog dialog = builder.show();
			TextView textView = (TextView) dialog.findViewById(android.R.id.message);
			if (textView != null) {
				textView.setMovementMethod(LinkMovementMethod.getInstance());
			}
		}
	}

	private void setClickBackground() {
		int resId = 0;
		TypedArray a = getContext().obtainStyledAttributes(new int[] { android.R.attr.selectableItemBackground });
		try {
			resId = a.getResourceId(0, resId);
		} finally {
			a.recycle();
		}
		container.setBackgroundResource(resId);
	}

	public static class Builder {
		private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("0.0");
		private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.00");
		private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		private int labelId;
		private String labelText;
		private String value;
		private int infoId;
		private String infoText;
		private int backgroundResource;

		public Builder labelId(@StringRes int id) {
			this.labelId = id;
			this.labelText = "";
			return this;
		}

		public Builder labelText(String text) {
			this.labelText = text;
			this.labelId = 0;
			return this;
		}

		public Builder value(String value) {
			this.value = value;
			return this;
		}

		public Builder value(int value) {
			this.value = String.valueOf(value);
			return this;
		}

		public Builder value(double value) {
			return value(value, DOUBLE_FORMAT);
		}

		public Builder value(double value, DecimalFormat format) {
			this.value = format.format(value);
			return this;
		}

		public Builder valueInMinutes(int value) {
			this.value = DateTimeUtils.formatMinutes(value);
			return this;
		}

		public Builder valueAsPercentage(double value) {
			return valueAsPercentage(value, PERCENTAGE_FORMAT);
		}

		public Builder valueAsPercentage(double value, DecimalFormat format) {
			this.value = format.format(value * 100) + "%";
			return this;
		}

		public Builder valueAsDate(String date, Context context) {
			if (!TextUtils.isEmpty(date)) {
				try {
					long l = FORMAT.parse(date).getTime();
					this.value = DateUtils.formatDateTime(context, l,
						DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_MONTH);
				} catch (ParseException e) {
					this.value = date;
				}
			}
			return this;
		}

		public Builder infoId(@StringRes int id) {
			this.infoId = id;
			this.infoText = "";
			return this;
		}

		public Builder infoText(String text) {
			this.infoText = text;
			this.infoId = 0;
			return this;
		}

		public Builder backgroundResource(@DrawableRes int resId) {
			this.backgroundResource = resId;
			return this;
		}

		public boolean hasValue() {
			return !TextUtils.isEmpty(value);
		}

		public PlayStatView build(Context context) {
			PlayStatView view = new PlayStatView(context);
			if (labelId > 0) {
				view.setLabel(labelId);
			} else if (!TextUtils.isEmpty(labelText)) {
				view.setLabel(labelText);
			}
			view.setValue(value);
			if (infoId > 0) {
				view.setInfoText(infoId);
			} else if (!TextUtils.isEmpty(infoText)) {
				view.setInfoText(infoText);
			}
			if (backgroundResource > 0) {
				view.setBackgroundResource(backgroundResource);
			}
			return view;
		}
	}
}
