package com.boardgamegeek.ui.widget;

import android.content.Context;
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
import com.boardgamegeek.extensions.ViewKt;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PlayStatView extends TableRow {
	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.00");
	private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

	@BindView(R.id.labelView) TextView labelView;
	@BindView(R.id.valueView) TextView valueView;
	@BindView(R.id.infoImageView) ImageView infoImageView;
	@BindView(R.id.labelContainer) View labelContainer;
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

	public void setValue(int value) {
		setValue(String.valueOf(value));
	}

	public void setValue(double value) {
		setValue(DOUBLE_FORMAT.format(value));
	}

	public void setValueAsDate(String date, Context context) {
		if (!TextUtils.isEmpty(date)) {
			try {
				long l = FORMAT.parse(date).getTime();
				setValue(DateUtils.formatDateTime(context, l,
					DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_MONTH));
			} catch (ParseException e) {
				setValue(date);
			}
		}
	}

	public void setValue(CharSequence text) {
		valueView.setText(text);
	}

	public void setInfoText(@StringRes int textResId) {
		setInfoText(getContext().getString(textResId));
	}

	public void setInfoText(String text) {
		if (TextUtils.isEmpty(text)) {
			infoImageView.setVisibility(View.GONE);
		} else {
			infoImageView.setVisibility(View.VISIBLE);
			ViewKt.setSelectableBackground(labelContainer);
			final SpannableString spannableString = new SpannableString(text);
			Linkify.addLinks(spannableString, Linkify.WEB_URLS);
			builder = new AlertDialog.Builder(getContext());
			builder.setTitle(labelView.getText()).setMessage(spannableString);
		}
	}

	@OnClick(R.id.labelContainer)
	public void onInfoClick() {
		if (builder != null) {
			AlertDialog dialog = builder.show();
			TextView textView = dialog.findViewById(android.R.id.message);
			if (textView != null) {
				textView.setMovementMethod(LinkMovementMethod.getInstance());
			}
		}
	}
}
