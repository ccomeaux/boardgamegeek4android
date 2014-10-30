package com.boardgamegeek.ui.widget;

import butterknife.ButterKnife;
import butterknife.InjectView;

import com.boardgamegeek.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TableRow;
import android.widget.TextView;

public class PlayStatView extends TableRow {
	@InjectView(R.id.label) TextView mLabel;
	@InjectView(R.id.value) TextView mValue;

	public PlayStatView(Context context) {
		super(context);
		LayoutInflater.from(context).inflate(R.layout.widget_play_stat, this);
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
}
