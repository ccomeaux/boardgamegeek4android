package com.boardgamegeek.ui.widget;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.VersionUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class PlayStatView extends TableRow {
	@InjectView(R.id.label) TextView mLabel;
	@InjectView(R.id.value) TextView mValue;
	@InjectView(R.id.info) ImageView mInfo;
	@InjectView(R.id.label_container) View mContainer;
	private AlertDialog.Builder mBuilder;

	public PlayStatView(Context context) {
		super(context);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.widget_play_stat, this);
		ButterKnife.inject(this);
	}

	public void setLabel(CharSequence text) {
		mLabel.setText(text);
	}

	public void setLabel(int textId) {
		mLabel.setText(textId);
	}

	public void setValue(CharSequence text) {
		mValue.setText(text);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void setInfoText(int textId) {
		mInfo.setVisibility(View.VISIBLE);
		setClickBackground();
		final SpannableString s = new SpannableString(getContext().getString(textId));
		Linkify.addLinks(s, Linkify.ALL);
		if (VersionUtils.hasHoneycomb()) {
			mBuilder = new AlertDialog.Builder(getContext(), R.style.Theme_bgglight_Dialog_Custom);
		} else {
			mBuilder = new AlertDialog.Builder(getContext());
		}
		mBuilder.setTitle(mLabel.getText()).setMessage(s);
	}

	@OnClick(R.id.label_container)
	public void onInfoClick(View v) {
		if (mBuilder != null) {
			AlertDialog dialog = mBuilder.show();
			TextView textView = (TextView) dialog.findViewById(android.R.id.message);
			textView.setMovementMethod(LinkMovementMethod.getInstance());
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setClickBackground() {
		if (VersionUtils.hasHoneycomb()) {
			int resId = 0;
			TypedArray a = getContext().obtainStyledAttributes(new int[] { android.R.attr.selectableItemBackground });
			try {
				resId = a.getResourceId(0, resId);
			} finally {
				a.recycle();
			}
			mContainer.setBackgroundResource(resId);
		}
	}
}
