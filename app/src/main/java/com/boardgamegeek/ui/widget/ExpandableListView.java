package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.GameDetailActivity;
import com.boardgamegeek.util.ActivityUtils;

import java.util.Random;

public class ExpandableListView extends RelativeLayout {
	private static int mLimit = -1;

	private TextView mLabelView;
	private TextView mSummaryView;
	private int mQueryToken;
	private String mOneMore;
	private String mSomeMore;
	private int mNameColumnIndex;
	private int mCount;
	private int mGameId;
	private String mGameName;
	private String mLabel;
	private String mMany = null;

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
		state.count = mCount;
		state.many = mMany;
		return state;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		if (!(state instanceof SavedState)) {
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState saved = (SavedState) state;
		super.onRestoreInstanceState(saved.getSuperState());

		mCount = saved.count;
		mMany = saved.many;
	}

	private void init(Context context, AttributeSet attrs) {
		mOneMore = context.getString(R.string.one_more);
		mSomeMore = context.getString(R.string.some_more);

		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.widget_expandable_list, this, true);
		mLabelView = (TextView) findViewById(R.id.label);
		mSummaryView = (TextView) findViewById(R.id.summary);

		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ExpandableListView);
			try {
				mLabel = a.getString(R.styleable.ExpandableListView_label);
				mQueryToken = a.getInt(R.styleable.ExpandableListView_query_token, BggContract.INVALID_ID);
			} finally {
				a.recycle();
			}
		}

		mLabelView.setText(mLabel);

		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getContext(), GameDetailActivity.class);
				intent.putExtra(ActivityUtils.KEY_TITLE, mLabel);
				intent.putExtra(ActivityUtils.KEY_GAME_ID, mGameId);
				intent.putExtra(ActivityUtils.KEY_GAME_NAME, mGameName);
				intent.putExtra(ActivityUtils.KEY_QUERY_TOKEN, mQueryToken);
				getContext().startActivity(intent);
			}
		});
	}

	public void setLabel(CharSequence label) {
		mLabelView.setText(label);
	}

	public void setLimit(int limit) {
		mLimit = limit;
	}

	public void clear() {
		mSummaryView.setText("");
	}

	public void bind(Cursor cursor, int nameColumnIndex, int gameId, String gameName) {
		mNameColumnIndex = nameColumnIndex;
		mGameId = gameId;
		mGameName = gameName;
		updateData(cursor);
	}

	private void updateData(Cursor cursor) {
		mCount = cursor.getCount();
		CharSequence summary = null;
		if (mCount >= mLimit) {
			if (TextUtils.isEmpty(mMany)) {
				String[] many = getResources().getStringArray(R.array.many);
				Random r = new Random();
				mMany = many[r.nextInt(many.length)];
			}
			summary = mMany;
		} else {
			final CharSequence text = joinNames(cursor);
			if (!TextUtils.isEmpty(text)) {
				TextPaint paint = new TextPaint();
				paint.setTextSize(mSummaryView.getTextSize());
				summary = TextUtils.commaEllipsize(text, paint, mSummaryView.getWidth() - mSummaryView.getPaddingLeft()
					- mSummaryView.getPaddingRight(), mOneMore, mSomeMore);
				if (TextUtils.isEmpty(summary)) {
					summary = String.format(mSomeMore, mCount);
				}
			}
		}
		mSummaryView.setText(summary);
	}

	private String joinNames(Cursor cursor) {
		StringBuilder sb = new StringBuilder();
		int count = 0;
		if (cursor != null && cursor.moveToFirst()) {
			boolean firstTime = true;
			do {
				if (firstTime) {
					firstTime = false;
				} else {
					sb.append(", ");
				}
				sb.append(cursor.getString(mNameColumnIndex));
				count++;
				if (mLimit > 0 && count >= mLimit) {
					sb.append(getResources().getString(R.string.and_more));
					break;
				}
			} while (cursor.moveToNext());
		}
		return sb.toString();
	}

	private static class SavedState extends BaseSavedState {
		public int count;
		public String many;

		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			count = in.readInt();
			many = in.readString();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(@NonNull Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(count);
			dest.writeString(many);
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