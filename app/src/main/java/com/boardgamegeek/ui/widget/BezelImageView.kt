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

package com.boardgamegeek.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.withStyledAttributes
import androidx.core.view.ViewCompat
import com.boardgamegeek.R

/**
 * An [android.widget.ImageView] that draws its contents inside a mask and draws a border
 * drawable on top. This is useful for applying a beveled look to image contents, but is also
 * flexible enough for use with other desired aesthetics.
 */
class BezelImageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private val blackPaint: Paint
    private val maskedPaint: Paint

    private var bounds: Rect = Rect(0, 0, 0, 0)
    private var boundsF: RectF = RectF(bounds)

    private var borderDrawable: Drawable? = null
    private var maskDrawable: Drawable? = null

    private var desaturateColorFilter: ColorMatrixColorFilter? = null
    private var shouldDesaturateOnPress = false

    private var isCacheValid = false
    private var cacheBitmap: Bitmap
    private var cachedWidth: Int = 0
    private var cachedHeight: Int = 0

    init {
        context.withStyledAttributes(attrs, R.styleable.BezelImageView, defStyleAttr, defStyleRes) {
            maskDrawable = getDrawable(R.styleable.BezelImageView_maskDrawable)
            borderDrawable = getDrawable(R.styleable.BezelImageView_borderDrawable)
            shouldDesaturateOnPress = getBoolean(R.styleable.BezelImageView_desaturateOnPress, shouldDesaturateOnPress)
        }
        maskDrawable?.callback = this
        borderDrawable?.callback = this

        // Other initialization
        blackPaint = Paint()
        blackPaint.color = -0x1000000

        maskedPaint = Paint()
        maskedPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        // Always want a cache allocated.
        cacheBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        if (shouldDesaturateOnPress) {
            // Create a desaturate color filter for pressed state.
            val cm = ColorMatrix()
            cm.setSaturation(0f)
            desaturateColorFilter = ColorMatrixColorFilter(cm)
        }
    }

    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        val changed = super.setFrame(l, t, r, b)
        bounds = Rect(0, 0, r - l, b - t)
        boundsF = RectF(bounds)

        borderDrawable?.bounds = bounds
        maskDrawable?.bounds = bounds

        if (changed) isCacheValid = false
        return changed
    }

    @Suppress("DEPRECATION")
    override fun onDraw(canvas: Canvas) {
        val width = bounds.width()
        val height = bounds.height()

        if (width == 0 || height == 0) {
            return
        }

        if (!isCacheValid || width != cachedWidth || height != cachedHeight) {
            // Need to redraw the cache
            if (width == cachedWidth && height == cachedHeight) {
                // Have a correct-sized bitmap cache already allocated. Just erase it.
                cacheBitmap.eraseColor(0)
            } else {
                // Allocate a new bitmap with the correct dimensions.
                cacheBitmap.recycle()

                @SuppressLint("DrawAllocation")
                cacheBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                cachedWidth = width
                cachedHeight = height
            }

            val cacheCanvas = Canvas(cacheBitmap)
            if (maskDrawable != null) {
                val savedCanvas = cacheCanvas.save()
                maskDrawable?.draw(cacheCanvas)
                maskedPaint.colorFilter = if (shouldDesaturateOnPress && isPressed) desaturateColorFilter else null
                cacheCanvas.saveLayer(boundsF, maskedPaint, Canvas.ALL_SAVE_FLAG)
                super.onDraw(cacheCanvas)
                cacheCanvas.restoreToCount(savedCanvas)
            } else if (shouldDesaturateOnPress && isPressed) {
                val savedCanvas = cacheCanvas.save()
                cacheCanvas.drawRect(0f, 0f, cachedWidth.toFloat(), cachedHeight.toFloat(), blackPaint)
                maskedPaint.colorFilter = desaturateColorFilter
                cacheCanvas.saveLayer(boundsF, maskedPaint, Canvas.ALL_SAVE_FLAG)
                super.onDraw(cacheCanvas)
                cacheCanvas.restoreToCount(savedCanvas)
            } else {
                super.onDraw(cacheCanvas)
            }

            borderDrawable?.draw(cacheCanvas)
        }

        // Draw from cache
        canvas.drawBitmap(cacheBitmap, bounds.left.toFloat(), bounds.top.toFloat(), null)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        if (borderDrawable?.isStateful == true) {
            borderDrawable?.state = drawableState
        }
        if (maskDrawable?.isStateful == true) {
            maskDrawable?.state = drawableState
        }
        if (isDuplicateParentStateEnabled) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    override fun invalidateDrawable(who: Drawable) {
        if (who === borderDrawable || who === maskDrawable) {
            invalidate()
        } else {
            super.invalidateDrawable(who)
        }
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return who === borderDrawable || who === maskDrawable || super.verifyDrawable(who)
    }
}
