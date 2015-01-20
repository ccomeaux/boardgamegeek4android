package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import com.boardgamegeek.R;

public class PlayerNumberBar extends View {
	private static final int PADDING = 2;
	private static final int MINIMUM_DIMENSION = 24;
	private static final int MAXIMUM_DIMENSION = 48;
	private static final int MAX_LEVEL = 10000;

	int mMinWidth;
	int mMaxWidth;
	int mMinHeight;
	int mMaxHeight;

	private int mBest;
	private int mRecommended;
	private int mNotRecommended;
	private int mTotal;

	private Drawable mDrawable;
	private RefreshRunnable mRefreshRunnable;
	private long mUiThreadId;

	public PlayerNumberBar(Context context) {
		this(context, null);
	}

	public PlayerNumberBar(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PlayerNumberBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mUiThreadId = Thread.currentThread().getId();
		initPlayerNumberBar();
	}

	private void initPlayerNumberBar() {
		mTotal = 0;
		mBest = 0;
		mRecommended = 0;
		mNotRecommended = 0;
		mMinWidth = MINIMUM_DIMENSION;
		mMaxWidth = MAXIMUM_DIMENSION;
		mMinHeight = MINIMUM_DIMENSION;
		mMaxHeight = MAXIMUM_DIMENSION;
		setDrawable(getResources().getDrawable(R.drawable.player_number_bar));
		setPadding(PADDING, PADDING, PADDING, PADDING);
	}

	public Drawable getDrawable() {
		return mDrawable;
	}

	public void setDrawable(Drawable d) {
		if (d != null) {
			d.setCallback(this);
		}
		mDrawable = d;
		postInvalidate();
	}

	@Override
	protected boolean verifyDrawable(Drawable who) {
		return who == mDrawable || super.verifyDrawable(who);
	}

	private class RefreshRunnable implements Runnable {

		private int mId;
		private int mLevelId;

		RefreshRunnable(int id, int levelId) {
			mId = id;
			mLevelId = levelId;
		}

		public void run() {
			doRefresh(mId, mLevelId);
			// Put ourselves back in the cache when we are done
			mRefreshRunnable = this;
		}

		public void setup(int id, int level) {
			mId = id;
			mLevelId = level;
		}
	}

	private synchronized void doRefresh(int id, int level) {
		float scale = mTotal > 0 ? (float) level / (float) mTotal : 0;
		final Drawable d = mDrawable;
		if (d != null) {
			Drawable drawable = ((LayerDrawable) d).findDrawableByLayerId(id);
			final int scaledLevel = (int) (scale * MAX_LEVEL);
			drawable.setLevel(scaledLevel);
		} else {
			invalidate();
		}
	}

	private synchronized void refresh(int id, int level) {
		if (mUiThreadId == Thread.currentThread().getId()) {
			doRefresh(id, level);
		} else {
			RefreshRunnable r;
			if (mRefreshRunnable != null) {
				// Use cached RefreshRunnable if available
				r = mRefreshRunnable;
				// Uncache it
				mRefreshRunnable = null;
				r.setup(id, level);
			} else {
				// Make a new one
				r = new RefreshRunnable(id, level);
			}
			post(r);
		}
	}

	public synchronized void setBest(int best) {
		if (best < 0) {
			best = 0;
		}

		if (best + mRecommended + mNotRecommended > mTotal) {
			best = mTotal - mRecommended - mNotRecommended;
		}

		if (best != mBest) {
			mBest = best;
			refresh(R.id.best, mBest);
		}
	}

	public synchronized void setRecommended(int recommended) {
		if (recommended < 0) {
			recommended = 0;
		}

		if (recommended + mBest + mNotRecommended > mTotal) {
			recommended = mTotal - mBest - mNotRecommended;
		}

		if (recommended != mRecommended) {
			mRecommended = recommended;
			refresh(R.id.recommended, mRecommended + mBest);
		}
	}

	public synchronized void setNotRecommended(int notRecommended) {
		if (notRecommended < 0) {
			notRecommended = 0;
		}

		if (notRecommended + mBest + mRecommended > mTotal) {
			notRecommended = mTotal - mBest - mRecommended;
		}

		if (notRecommended != mRecommended) {
			mNotRecommended = notRecommended;
			refresh(R.id.background, mTotal - mNotRecommended);
		}
	}

	public synchronized int getBest() {
		return mBest;
	}

	public synchronized int getRecommended() {
		return mRecommended;
	}

	public synchronized int getNotRecommended() {
		return mNotRecommended;
	}

	public synchronized int getTotal() {
		return mTotal;
	}

	public synchronized void setTotal(int total) {
		// HACK when total is 0, this doesn't display right
		if (total < 1) {
			total = 1;
		}
		if (total != mTotal) {
			mTotal = total;
			postInvalidate();

			if (mBest + mRecommended + mNotRecommended > total) {
				mBest = 0;
				mRecommended = 0;
				mNotRecommended = 0;
			}
			refresh(R.id.background, mTotal);
		}
	}

	@Override
	public void setVisibility(int v) {
		if (getVisibility() != v) {
			super.setVisibility(v);
		}
	}

	@Override
	public void invalidateDrawable(@NonNull Drawable dr) {
		if (verifyDrawable(dr)) {
			final Rect dirty = dr.getBounds();
			final int scrollX = getScrollX() + getPaddingLeft();
			final int scrollY = getScrollY() + getPaddingTop();

			invalidate(dirty.left + scrollX, dirty.top + scrollY, dirty.right + scrollX, dirty.bottom + scrollY);
		} else {
			super.invalidateDrawable(dr);
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// onDraw will translate the canvas so we draw starting at 0,0

		int right = w - getPaddingRight() - getPaddingLeft();
		int bottom = h - getPaddingBottom() - getPaddingTop();

		if (mDrawable != null) {
			mDrawable.setBounds(0, 0, right, bottom);
		}
	}

	@Override
	protected synchronized void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		Drawable d = mDrawable;
		if (d != null) {
			// TODO - not sure we need this
			// Translate canvas so a indeterminate circular progress bar
			// with padding rotates properly in its animation
			canvas.save();
			canvas.translate(getPaddingLeft(), getPaddingTop());
			d.draw(canvas);
			canvas.restore();
		}
	}

	@Override
	protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Drawable d = mDrawable;

		int dw = 0;
		int dh = 0;
		if (d != null) {
			dw = Math.max(mMinWidth, Math.min(mMaxWidth, d.getIntrinsicWidth()));
			dh = Math.max(mMinHeight, Math.min(mMaxHeight, d.getIntrinsicHeight()));
		}
		dw += getPaddingLeft() + getPaddingRight();
		dh += getPaddingTop() + getPaddingBottom();

		setMeasuredDimension(resolveSize(dw, widthMeasureSpec), resolveSize(dh, heightMeasureSpec));
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();

		int[] state = getDrawableState();

		if (mDrawable != null && mDrawable.isStateful()) {
			mDrawable.setState(state);
		}
	}

	static class SavedState extends BaseSavedState {
		int total;
		int best;
		int recommended;
		int notRecommended;

		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			total = in.readInt();
			best = in.readInt();
			recommended = in.readInt();
			notRecommended = in.readInt();
		}

		@Override
		public void writeToParcel(@NonNull Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeInt(total);
			out.writeInt(best);
			out.writeInt(recommended);
			out.writeInt(notRecommended);
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

	@Override
	public Parcelable onSaveInstanceState() {
		// Force our ancestor class to save its state
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);

		ss.total = mTotal;
		ss.best = mBest;
		ss.recommended = mRecommended;
		ss.notRecommended = mNotRecommended;

		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());

		setBest(ss.best);
		setRecommended(ss.recommended);
	}
}
