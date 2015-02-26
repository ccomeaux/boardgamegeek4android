package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.StringUtils;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class GameCollectionRow extends LinearLayout {
	@InjectView(R.id.thumbnail) ImageView mThumbnail;
	@InjectView(R.id.name) TextView mNameView;
	@InjectView(R.id.year) TextView mYearView;
	@InjectView(R.id.status) TextView mStatusView;

	public GameCollectionRow(Context context) {
		super(context);
		init(context, null);
	}

	public GameCollectionRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public GameCollectionRow(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.widget_collection_row, this, true);
		ButterKnife.inject(this);
	}

	public void setName(String name) {
		mNameView.setText(name);
	}

	public void setYear(int year) {
		if (year > 0) {
			mYearView.setVisibility(View.VISIBLE);
			mYearView.setText(StringUtils.describeYear(getContext(), year));
		}
	}

	public void setStatus(List<String> status, int playCount) {
		String d = StringUtils.formatList(status);
		if (TextUtils.isEmpty(d)) {
			if (playCount > 0) {
				d = getContext().getString(R.string.played);
			}
		}
		if (!TextUtils.isEmpty(d)) {
			mStatusView.setVisibility(View.VISIBLE);
			mStatusView.setText(d);
		}
	}

	public void setThumbnail(String thumbnailUrl) {
		Picasso.with(getContext())
			.load(HttpUtils.ensureScheme(thumbnailUrl))
			.placeholder(R.drawable.thumbnail_image_empty)
			.error(R.drawable.thumbnail_image_empty)
			.resizeDimen(R.dimen.thumbnail_list_size, R.dimen.thumbnail_list_size)
			.centerCrop()
			.into(mThumbnail);
	}
}
