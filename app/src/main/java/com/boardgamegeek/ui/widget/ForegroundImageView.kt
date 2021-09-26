/*
 * Copyright 2014 DogmaLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.boardgamegeek.ui.widget

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.boardgamegeek.R

class ForegroundImageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0)
    : AppCompatImageView(context, attrs, defStyle) {
    private var foreground: Drawable? = null
    private val rectPadding = Rect()
    private var foregroundPadding = false
    private var foregroundBoundsChanged = false

    init {

        val a = context.obtainStyledAttributes(attrs, R.styleable.ForegroundImageView, defStyle, 0)
        try {
            val d = a.getDrawable(R.styleable.ForegroundImageView_android_foreground)
            foregroundPadding = a.getBoolean(R.styleable.ForegroundImageView_foregroundInsidePadding, false)

            // Apply foreground padding for nine patches automatically
            if (!foregroundPadding) {
                val npd = background as? NinePatchDrawable
                if (npd?.getPadding(rectPadding) == true) {
                    foregroundPadding = true
                }
            }
            setForeground(d)
        } finally {
            a.recycle()
        }
    }

    /**
     * Supply a Drawable that is to be rendered on top of all of the child views in the layout.
     *
     * @param drawable The Drawable to be drawn on top of the children.
     */
    override fun setForeground(drawable: Drawable?) {
        if (foreground !== drawable) {
            foreground?.callback = null
            unscheduleDrawable(foreground)

            foreground = drawable

            if (drawable != null) {
                setWillNotDraw(false)
                drawable.callback = this
                if (drawable.isStateful) drawable.state = drawableState
            } else {
                setWillNotDraw(true)
            }
            requestLayout()
            invalidate()
        }
    }

    /**
     * Returns the drawable used as the foreground of this layout. The foreground drawable,
     * if non-null, is always drawn on top of the children.
     *
     * @return A Drawable or null if no foreground was set.
     */
    override fun getForeground(): Drawable? {
        return foreground
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()

        if (foreground?.isStateful == true) {
            foreground?.state = drawableState
        }
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return super.verifyDrawable(who) || who === foreground
    }

    override fun jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState()
        foreground?.jumpToCurrentState()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        foregroundBoundsChanged = true
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if (foreground != null) {
            val foreground = this.foreground as Drawable

            if (foregroundBoundsChanged) {
                foregroundBoundsChanged = false

                val w = right - left
                val h = bottom - top

                if (foregroundPadding) {
                    foreground.setBounds(rectPadding.left, rectPadding.top, w - rectPadding.right, h - rectPadding.bottom)
                } else {
                    foreground.setBounds(0, 0, w, h)
                }
            }
            foreground.draw(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @TargetApi(VERSION_CODES.LOLLIPOP)
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                foreground?.setHotspot(e.x, e.y)
            }
        }
        return super.onTouchEvent(e)
    }

    override fun drawableHotspotChanged(x: Float, y: Float) {
        super.drawableHotspotChanged(x, y)
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            foreground?.setHotspot(x, y)
        }
    }
}
