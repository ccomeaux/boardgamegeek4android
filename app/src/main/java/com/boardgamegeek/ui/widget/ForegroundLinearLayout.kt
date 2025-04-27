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

package com.boardgamegeek.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import com.boardgamegeek.R

open class ForegroundLinearLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var foregroundDrawable: Drawable? = null
    private val selfBounds = Rect()
    private val overlayBounds = Rect()
    private var foregroundGravity = Gravity.FILL
    private var isForegroundInPadding = true
    private var foregroundBoundsChanged = false

    init {
        context.withStyledAttributes(attrs, R.styleable.ForegroundLinearLayout, defStyleAttr, defStyleRes) {
            foregroundGravity = getInt(R.styleable.BggForegroundLinearLayout_android_foregroundGravity, foregroundGravity)
            getDrawable(R.styleable.BggForegroundLinearLayout_android_foreground)?.let { foreground = it }
            isForegroundInPadding = getBoolean(R.styleable.BggForegroundLinearLayout_foregroundInsidePadding, true)
        }
    }

    /**
     * Describes how the foreground is positioned.
     *
     * @return foreground gravity.
     * @see .setForegroundGravity
     */
    override fun getForegroundGravity(): Int {
        return foregroundGravity
    }

    /**
     * Describes how the foreground is positioned. Defaults to START and TOP.
     *
     * @param foregroundGravity See [android.view.Gravity]
     * @see .getForegroundGravity
     */
    override fun setForegroundGravity(foregroundGravity: Int) {
        var gravity = foregroundGravity
        if (this.foregroundGravity != gravity) {
            if (gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK == 0) {
                gravity = foregroundGravity or Gravity.START
            }

            if (gravity and Gravity.VERTICAL_GRAVITY_MASK == 0) {
                gravity = foregroundGravity or Gravity.TOP
            }

            this.foregroundGravity = gravity

            if (this.foregroundGravity == Gravity.FILL) {
                foregroundDrawable?.getPadding(Rect())
            }

            requestLayout()
        }
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return super.verifyDrawable(who) || who === foregroundDrawable
    }

    override fun jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState()
        foregroundDrawable?.jumpToCurrentState()
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        if (foregroundDrawable?.isStateful == true) {
            foregroundDrawable?.state = drawableState
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
    override fun setForeground(drawable: Drawable?) {
        if (foregroundDrawable !== drawable) {
            foregroundDrawable?.callback = null
            unscheduleDrawable(foregroundDrawable)

            foregroundDrawable = drawable

            if (drawable != null) {
                setWillNotDraw(false)
                drawable.callback = this
                if (drawable.isStateful) drawable.state = drawableState
                if (foregroundGravity == Gravity.FILL) {
                    drawable.getPadding(Rect())
                }
            } else {
                setWillNotDraw(true)
            }
            requestLayout()
            invalidate()
        }
    }

    /**
     * Returns the drawable used as the foreground of this FrameLayout. The
     * foreground drawable, if non-null, is always drawn on top of the children.
     *
     * @return A Drawable or null if no foreground was set.
     */
    override fun getForeground(): Drawable? {
        return foregroundDrawable
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        foregroundBoundsChanged = changed
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        foregroundBoundsChanged = true
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if (foregroundDrawable != null) {
            val foreground = foregroundDrawable

            if (foregroundBoundsChanged) {
                foregroundBoundsChanged = false
                val selfBounds = this.selfBounds
                val overlayBounds = this.overlayBounds

                val w = right - left
                val h = bottom - top

                if (isForegroundInPadding) {
                    selfBounds.set(0, 0, w, h)
                } else {
                    selfBounds.set(paddingLeft, paddingTop, w - paddingRight, h - paddingBottom)
                }

                Gravity.apply(foregroundGravity, foreground!!.intrinsicWidth, foreground.intrinsicHeight, selfBounds, overlayBounds)
                foreground.bounds = overlayBounds
            }

            foreground!!.draw(canvas)
        }
    }


    override fun drawableHotspotChanged(x: Float, y: Float) {
        super.drawableHotspotChanged(x, y)
        foregroundDrawable?.setHotspot(x, y)
    }
}
