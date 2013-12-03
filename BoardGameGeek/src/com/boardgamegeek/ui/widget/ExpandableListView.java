package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.widget.CursorAdapter;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;

public class ExpandableListView extends RelativeLayout {

	private TextView mLabelView;
	private TextView mSummaryView;
	private ImageView mToggleView;
	private ListView mDetailView;
	private boolean mClickable;
	private boolean mExpanded;
	private String mOneMore;
	private String mSomeMore;
	private CursorAdapter mAdapter;
	private int mNameColumnIndex;
	private int mIdColumnIndex;
	private String mPath;

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
		mDetailView = (ListView) findViewById(R.id.detail);

		mAdapter = new Adapter(context);
		mDetailView.setAdapter(mAdapter);

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
		updateSummary(mAdapter.getCursor());
	}

	public void setLabel(CharSequence label) {
		mLabelView.setText(label);
	}

	public void clear() {
		mAdapter.swapCursor(null);
		updateData(mAdapter.getCursor());
	}

	public void bind(Cursor cursor, int nameColumnIndex, int idColumnIndex, String path) {
		mNameColumnIndex = nameColumnIndex;
		mIdColumnIndex = idColumnIndex;
		mPath = path;
		mAdapter.swapCursor(cursor);
		updateData(cursor);
	}

	private void updateData(Cursor cursor) {
		updateSummary(cursor);
		updateDetail();
	}

	private void updateSummary(Cursor cursor) {
		CharSequence summary = null;
		if (cursor != null) {
			TextPaint paint = new TextPaint();
			paint.setTextSize(mSummaryView.getTextSize());
			summary = TextUtils.commaEllipsize(joinFirsts(cursor), paint,
				mSummaryView.getWidth() - mSummaryView.getPaddingLeft() - mSummaryView.getPaddingRight(), mOneMore,
				mSomeMore);
			if (TextUtils.isEmpty(summary)) {
				summary = String.format(mSomeMore, cursor.getCount());
			}
		}
		mSummaryView.setText(summary);
	}

	private void updateDetail() {
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
		requestLayout();
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

	private String joinFirsts(Cursor cursor) {
		StringBuilder sb = new StringBuilder();
		if (cursor != null && cursor.moveToFirst()) {
			boolean firstTime = true;
			do {
				if (firstTime) {
					firstTime = false;
				} else {
					sb.append(", ");
				}
				sb.append(cursor.getString(mNameColumnIndex));
			} while (cursor.moveToNext());
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

	private class Adapter extends CursorAdapter {
		private LayoutInflater mInflater;

		public Adapter(Context context) {
			super(context, null, false);
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(R.layout.row_expanded_list, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();

			String name = cursor.getString(mNameColumnIndex);
			int id = cursor.getInt(mIdColumnIndex);
			Uri uri = (mPath != null ? BggContract.buildBasicUri(mPath, id) : null);

			if (mClickable && uri != null) {
				holder.button.setClickable(true);
				holder.button.setOnClickListener(onClick());
				holder.button.setTag(null);
			} else {
				holder.button.setClickable(false);
				holder.button.setOnClickListener(null);
				holder.button.setTag(uri);
			}

			holder.button.setText(name);
		}
	}

	static class ViewHolder {
		Button button;

		public ViewHolder(View view) {
			button = (Button) view.findViewById(R.id.button);
		}
	}
}