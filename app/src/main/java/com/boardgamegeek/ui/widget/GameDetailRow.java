package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
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

import butterknife.ButterKnife;
import butterknife.InjectView;

public class GameDetailRow extends RelativeLayout {
	@InjectView(android.R.id.icon) ImageView mIconView;
	@InjectView(R.id.label) TextView mLabelView;
	@InjectView(R.id.data) TextView mDataView;
	private int mQueryToken;
	private String mOneMore;
	private String mSomeMore;
	private int mNameColumnIndex;
	private int mCount;
	private int mGameId;
	private String mGameName;
	private String mLabel;
	private Drawable mIcon;

	public GameDetailRow(Context context) {
		super(context);
		init(context, null);
	}

	public GameDetailRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public GameDetailRow(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable p = super.onSaveInstanceState();
		SavedState state = new SavedState(p);
		state.count = mCount;
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
	}

	private void init(Context context, AttributeSet attrs) {
		mOneMore = context.getString(R.string.one_more);
		mSomeMore = context.getString(R.string.some_more);

		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.widget_game_detail_row, this, true);
		ButterKnife.inject(this);

		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GameDetailRow);
			try {
				mLabel = a.getString(R.styleable.GameDetailRow_label);
				mIcon = a.getDrawable(R.styleable.GameDetailRow_icon_res);
				mQueryToken = a.getInt(R.styleable.GameDetailRow_query_token, BggContract.INVALID_ID);
			} finally {
				a.recycle();
			}
		}

		if (mIcon == null) {
			mIconView.setVisibility(View.GONE);
			mLabelView.setVisibility(View.VISIBLE);
			mLabelView.setText(mLabel);
		} else {
			mLabelView.setVisibility(View.GONE);
			mIconView.setVisibility(View.VISIBLE);
			mIconView.setImageDrawable(mIcon);
		}

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

	public void clear() {
		mDataView.setText("");
	}

	public void color(Palette.Swatch swatch) {
		colorIcon(swatch);
		colorText(swatch);
	}

	public void colorIcon(Palette.Swatch swatch) {
		mIconView.setColorFilter(swatch.getRgb());
	}

	public static final ButterKnife.Setter<GameDetailRow, Palette.Swatch> colorIconSetter =
		new ButterKnife.Setter<GameDetailRow, Palette.Swatch>() {
			@Override
			public void set(GameDetailRow view, Palette.Swatch value, int index) {
				if (view != null && value != null) {
					view.colorIcon(value);
				}
			}
		};

	public void colorText(Palette.Swatch swatch) {
		mDataView.setTextColor(swatch.getRgb());
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
		final CharSequence text = joinNames(cursor);
		if (!TextUtils.isEmpty(text)) {
			TextPaint paint = new TextPaint();
			paint.setTextSize(mDataView.getTextSize());
			summary = TextUtils.commaEllipsize(text, paint, mDataView.getWidth() * 2, mOneMore, mSomeMore);
			if (TextUtils.isEmpty(summary)) {
				summary = String.format(mSomeMore, mCount);
			}
		}
		mDataView.setText(summary);
	}

	private String joinNames(Cursor cursor) {
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
		public int count;

		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			count = in.readInt();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(@NonNull Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(count);
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