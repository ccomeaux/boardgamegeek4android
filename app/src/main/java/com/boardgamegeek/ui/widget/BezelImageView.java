/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The original is from Google you can find here:
 * https://github.com/google/iosched/blob/master/android/src/main/java/com/google/samples/apps/iosched/ui/widget/BezelImageView.java
 */

package com.boardgamegeek.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.boardgamegeek.R;

/**
 * An {@link android.widget.ImageView} that draws its contents inside a mask and draws a border
 * drawable on top. This is useful for applying a beveled look to image contents, but is also
 * flexible enough for use with other desired aesthetics.
 */
public class BezelImageView extends ImageView {
	private final Paint blackPaint;
	private final Paint maskedPaint;

	private Rect bounds;
	private RectF boundsF;

	private final Drawable borderDrawable;
	private final Drawable maskDrawable;

	private ColorMatrixColorFilter desaturateColorFilter;
	private boolean shouldDesaturateOnPress = false;

	private boolean isCacheValid = false;
	private Bitmap cacheBitmap;
	private int cachedWidth;
	private int cachedHeight;

	public BezelImageView(Context context) {
		this(context, null);
	}

	public BezelImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public BezelImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		// Attribute initialization
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BezelImageView, defStyle, 0);

		maskDrawable = a.getDrawable(R.styleable.BezelImageView_maskDrawable);
		if (maskDrawable != null) {
			maskDrawable.setCallback(this);
		}

		borderDrawable = a.getDrawable(R.styleable.BezelImageView_borderDrawable);
		if (borderDrawable != null) {
			borderDrawable.setCallback(this);
		}

		shouldDesaturateOnPress = a.getBoolean(R.styleable.BezelImageView_desaturateOnPress,
			shouldDesaturateOnPress);

		a.recycle();

		// Other initialization
		blackPaint = new Paint();
		blackPaint.setColor(0xff000000);

		maskedPaint = new Paint();
		maskedPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

		// Always want a cache allocated.
		cacheBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

		if (shouldDesaturateOnPress) {
			// Create a desaturate color filter for pressed state.
			ColorMatrix cm = new ColorMatrix();
			cm.setSaturation(0);
			desaturateColorFilter = new ColorMatrixColorFilter(cm);
		}
	}

	@Override
	protected boolean setFrame(int l, int t, int r, int b) {
		final boolean changed = super.setFrame(l, t, r, b);
		bounds = new Rect(0, 0, r - l, b - t);
		boundsF = new RectF(bounds);

		if (borderDrawable != null) {
			borderDrawable.setBounds(bounds);
		}
		if (maskDrawable != null) {
			maskDrawable.setBounds(bounds);
		}

		if (changed) {
			isCacheValid = false;
		}

		return changed;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (bounds == null) {
			return;
		}

		int width = bounds.width();
		int height = bounds.height();

		if (width == 0 || height == 0) {
			return;
		}

		if (!isCacheValid || width != cachedWidth || height != cachedHeight) {
			// Need to redraw the cache
			if (width == cachedWidth && height == cachedHeight) {
				// Have a correct-sized bitmap cache already allocated. Just erase it.
				cacheBitmap.eraseColor(0);
			} else {
				// Allocate a new bitmap with the correct dimensions.
				cacheBitmap.recycle();
				//noinspection AndroidLintDrawAllocation
				cacheBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				cachedWidth = width;
				cachedHeight = height;
			}

			@SuppressLint("DrawAllocation") Canvas cacheCanvas = new Canvas(cacheBitmap);
			if (maskDrawable != null) {
				int sc = cacheCanvas.save();
				maskDrawable.draw(cacheCanvas);
				maskedPaint.setColorFilter((shouldDesaturateOnPress && isPressed())
					? desaturateColorFilter : null);
				cacheCanvas.saveLayer(boundsF, maskedPaint,
					Canvas.HAS_ALPHA_LAYER_SAVE_FLAG | Canvas.FULL_COLOR_LAYER_SAVE_FLAG);
				super.onDraw(cacheCanvas);
				cacheCanvas.restoreToCount(sc);
			} else if (shouldDesaturateOnPress && isPressed()) {
				int sc = cacheCanvas.save();
				cacheCanvas.drawRect(0, 0, cachedWidth, cachedHeight, blackPaint);
				maskedPaint.setColorFilter(desaturateColorFilter);
				cacheCanvas.saveLayer(boundsF, maskedPaint,
					Canvas.HAS_ALPHA_LAYER_SAVE_FLAG | Canvas.FULL_COLOR_LAYER_SAVE_FLAG);
				super.onDraw(cacheCanvas);
				cacheCanvas.restoreToCount(sc);
			} else {
				super.onDraw(cacheCanvas);
			}

			if (borderDrawable != null) {
				borderDrawable.draw(cacheCanvas);
			}
		}

		// Draw from cache
		canvas.drawBitmap(cacheBitmap, bounds.left, bounds.top, null);
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		if (borderDrawable != null && borderDrawable.isStateful()) {
			borderDrawable.setState(getDrawableState());
		}
		if (maskDrawable != null && maskDrawable.isStateful()) {
			maskDrawable.setState(getDrawableState());
		}
		if (isDuplicateParentStateEnabled()) {
			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

	@Override
	public void invalidateDrawable(@NonNull Drawable who) {
		if (who == borderDrawable || who == maskDrawable) {
			invalidate();
		} else {
			super.invalidateDrawable(who);
		}
	}

	@Override
	protected boolean verifyDrawable(@NonNull Drawable who) {
		return who == borderDrawable || who == maskDrawable || super.verifyDrawable(who);
	}
}
