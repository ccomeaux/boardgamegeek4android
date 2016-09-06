/*
 * ******************************************************************************
 *   Copyright (c)
 *   https://gist.github.com/chrisbanes/9091754
 *   https://github.com/gabrielemariotti/cardslib/blob/master/library-core/src/main/java/it/gmariotti/cardslib/library/view/ForegroundLinearLayout.java
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  *****************************************************************************
 */

package com.boardgamegeek.ui.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

import com.boardgamegeek.R;

public class ForegroundLinearLayout extends LinearLayout {
	private Drawable foregroundDrawable;
	private final Rect selfBounds = new Rect();
	private final Rect overlayBounds = new Rect();
	private int foregroundGravity = Gravity.FILL;
	protected boolean isForegroundInPadding = true;
	private boolean foregroundBoundsChanged = false;

	public ForegroundLinearLayout(Context context) {
		super(context);
	}

	public ForegroundLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ForegroundLinearLayout, 0, 0);
		try {
			foregroundGravity = a.getInt(R.styleable.ForegroundLinearLayout_android_foregroundGravity, foregroundGravity);

			final Drawable drawable = a.getDrawable(R.styleable.ForegroundLinearLayout_android_foreground);
			if (drawable != null) {
				setForeground(drawable);
			}

			isForegroundInPadding = a.getBoolean(R.styleable.ForegroundLinearLayout_android_foregroundInsidePadding, true);
		} finally {
			a.recycle();
		}
	}

	/**
	 * Describes how the foreground is positioned.
	 *
	 * @return foreground gravity.
	 * @see #setForegroundGravity(int)
	 */
	public int getForegroundGravity() {
		return foregroundGravity;
	}

	/**
	 * Describes how the foreground is positioned. Defaults to START and TOP.
	 *
	 * @param foregroundGravity See {@link android.view.Gravity}
	 * @see #getForegroundGravity()
	 */
	public void setForegroundGravity(int foregroundGravity) {
		if (this.foregroundGravity != foregroundGravity) {
			if ((foregroundGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
				foregroundGravity |= Gravity.START;
			}

			if ((foregroundGravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
				foregroundGravity |= Gravity.TOP;
			}

			this.foregroundGravity = foregroundGravity;

			if (this.foregroundGravity == Gravity.FILL && foregroundDrawable != null) {
				Rect padding = new Rect();
				foregroundDrawable.getPadding(padding);
			}

			requestLayout();
		}
	}

	@Override
	protected boolean verifyDrawable(Drawable who) {
		return super.verifyDrawable(who) || (who == foregroundDrawable);
	}

	@Override
	public void jumpDrawablesToCurrentState() {
		super.jumpDrawablesToCurrentState();
		if (foregroundDrawable != null) {
			foregroundDrawable.jumpToCurrentState();
		}
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		if (foregroundDrawable != null && foregroundDrawable.isStateful()) {
			foregroundDrawable.setState(getDrawableState());
		}
	}

	/**
	 * Supply a Drawable that is to be rendered on top of all of the child
	 * views in the frame layout.  Any padding in the Drawable will be taken
	 * into account by ensuring that the children are inset to be placed
	 * inside of the padding area.
	 *
	 * @param drawable The Drawable to be drawn on top of the children.
	 */
	public void setForeground(Drawable drawable) {
		if (foregroundDrawable != drawable) {
			if (foregroundDrawable != null) {
				foregroundDrawable.setCallback(null);
				unscheduleDrawable(foregroundDrawable);
			}

			foregroundDrawable = drawable;

			if (drawable != null) {
				setWillNotDraw(false);
				drawable.setCallback(this);
				if (drawable.isStateful()) {
					drawable.setState(getDrawableState());
				}
				if (foregroundGravity == Gravity.FILL) {
					Rect padding = new Rect();
					drawable.getPadding(padding);
				}
			} else {
				setWillNotDraw(true);
			}
			requestLayout();
			invalidate();
		}
	}

	/**
	 * Returns the drawable used as the foreground of this FrameLayout. The
	 * foreground drawable, if non-null, is always drawn on top of the children.
	 *
	 * @return A Drawable or null if no foreground was set.
	 */
	public Drawable getForeground() {
		return foregroundDrawable;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		foregroundBoundsChanged = changed;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		foregroundBoundsChanged = true;
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);

		if (foregroundDrawable != null) {
			final Drawable foreground = foregroundDrawable;

			if (foregroundBoundsChanged) {
				foregroundBoundsChanged = false;
				final Rect selfBounds = this.selfBounds;
				final Rect overlayBounds = this.overlayBounds;

				final int w = getRight() - getLeft();
				final int h = getBottom() - getTop();

				if (isForegroundInPadding) {
					selfBounds.set(0, 0, w, h);
				} else {
					selfBounds.set(getPaddingLeft(), getPaddingTop(), w - getPaddingRight(), h - getPaddingBottom());
				}

				Gravity.apply(foregroundGravity, foreground.getIntrinsicWidth(), foreground.getIntrinsicHeight(), selfBounds, overlayBounds);
				foreground.setBounds(overlayBounds);
			}

			foreground.draw(canvas);
		}
	}


	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	public void drawableHotspotChanged(float x, float y) {
		super.drawableHotspotChanged(x, y);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			if (foregroundDrawable != null) {
				foregroundDrawable.setHotspot(x, y);
			}
		}
	}
}