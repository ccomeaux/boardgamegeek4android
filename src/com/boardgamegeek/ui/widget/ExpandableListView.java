package com.boardgamegeek.ui.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.boardgamegeek.R;

public class ExpandableListView extends RelativeLayout {

	private TextView mLabelView;
	private TextView mSummaryView;
	private ImageView mToggleView;
	private LinearLayout mDetailView;
	private boolean mClickable;
	private boolean mExpanded;
	private String mOneMore;
	private String mSomeMore;
	private List<Pair<String, Uri>> mData;

	public ExpandableListView(Context context) {
		super(context);
		init(context, null);
	}

	public ExpandableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public ExpandableListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable p = super.onSaveInstanceState();
		SavedState state = new SavedState(p);
		state.expanded = mExpanded;
		return p;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		if (!(state instanceof SavedState)) {
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState saved = (SavedState) state;
		super.onRestoreInstanceState(saved.getSuperState());

		mExpanded = saved.expanded;
		expandOrCollapse();
	}

	private void init(Context context, AttributeSet attrs) {
		mOneMore = context.getString(R.string.one_more);
		mSomeMore = context.getString(R.string.some_more);

		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.widget_expandable_list, this, true);
		mLabelView = (TextView) findViewById(R.id.label);
		mSummaryView = (TextView) findViewById(R.id.summary);
		mToggleView = (ImageView) findViewById(R.id.toggle);
		mDetailView = (LinearLayout) findViewById(R.id.detail);

		mExpanded = false;
		expandOrCollapse();
		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mExpanded = !mExpanded;
				expandOrCollapse();
			}
		});

		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ExpandableListView);
			try {
				mLabelView.setText(a.getString(R.styleable.ExpandableListView_label));
				mClickable = a.getBoolean(R.styleable.ExpandableListView_clickable, true);
			} finally {
				a.recycle();
			}
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		updateSummary();
	}

	public void setLabel(CharSequence label) {
		mLabelView.setText(label);
	}

	public void clear() {
		if (mData != null) {
			mData.clear();
		}
		updateData();
	}

	public void addItem(String label, Uri uri) {
		if (mData == null) {
			mData = new ArrayList<Pair<String, Uri>>();
		}
		mData.add(new Pair<String, Uri>(label, uri));
		updateData();
	}

	private void updateData() {
		updateSummary();
		updateDetail();
	}

	private void updateSummary() {
		CharSequence summary = null;
		if (mData != null) {
			TextPaint paint = new TextPaint();
			paint.setTextSize(mSummaryView.getTextSize());
			summary = TextUtils.commaEllipsize(joinFirsts(), paint,
				mSummaryView.getWidth() - mSummaryView.getPaddingLeft() - mSummaryView.getPaddingRight(), mOneMore,
				mSomeMore);
			if (TextUtils.isEmpty(summary)) {
				summary = String.format(mSomeMore, mData.size());
			}
		}
		mSummaryView.setText(summary);
	}

	private void updateDetail() {
		mDetailView.removeAllViews();
		if (mData != null) {
			List<Pair<String, Uri>> data = mData;
			for (final Pair<String, Uri> item : data) {
				Button button = new Button(getContext(), null, android.R.attr.buttonStyleSmall);
				button.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT));
				button.setText(item.first);
				button.setTag(item.second);
				button.setEnabled(mClickable && item.second != null);
				button.setOnClickListener(onClick());
				mDetailView.addView(button);
			}
		}
	}

	private OnClickListener onClick() {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				Uri uri = (Uri) v.getTag();
				if (uri != null) {
					getContext().startActivity(new Intent(Intent.ACTION_VIEW, uri));
				}
			}
		};
	}

	private void expandOrCollapse() {
		mSummaryView.setVisibility(mExpanded ? GONE : VISIBLE);
		mDetailView.setVisibility(mExpanded ? VISIBLE : GONE);
		mToggleView.setImageResource(mExpanded ? R.drawable.expander_close : R.drawable.expander_open);
		requestLayout();
	}

	private String joinFirsts() {
		StringBuilder sb = new StringBuilder();
		boolean firstTime = true;
		for (Pair<String, Uri> item : mData) {
			if (firstTime) {
				firstTime = false;
			} else {
				sb.append(", ");
			}
			sb.append(item.first);
		}
		return sb.toString();
	}

	private static class SavedState extends BaseSavedState {
		public boolean expanded;

		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			expanded = in.readInt() != 0;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(expanded ? 1 : 0);
		}

		@SuppressWarnings("unused")
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