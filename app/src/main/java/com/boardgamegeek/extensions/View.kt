package com.boardgamegeek.extensions

import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.boardgamegeek.R
import com.boardgamegeek.util.ColorUtils
import com.boardgamegeek.util.ScrimUtils

fun View.fadeIn(animate: Boolean = true) {
    if (visibility != VISIBLE) {
        if (animate) {
            startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in))
        } else {
            clearAnimation()
        }
        visibility = VISIBLE
    }
}

fun View.fadeOut(visibility: Int = GONE, animate: Boolean = true) {
    if (this.visibility == VISIBLE) {
        if (animate) {
            val animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}

                override fun onAnimationEnd(animation: Animation) {
                    this@fadeOut.visibility = visibility
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
            startAnimation(animation)
        } else {
            clearAnimation()
            this.visibility = visibility
        }
    }
}

fun View.setColorViewValue(color: Int) {
    ColorUtils.setColorViewValue(this, color)
}

fun View.applyDarkScrim() {
    val color = ContextCompat.getColor(context, R.color.black_overlay)
    val drawable = ScrimUtils.makeCubicGradientScrimDrawable(color, 3, Gravity.BOTTOM)
    ViewCompat.setBackground(this, drawable)
}

/**
 * Set the background of a [View] o the specified color, with a darker version of the color as a 1dp border.
 */
fun View.setViewBackground(@ColorInt color: Int) {
    val r = this.resources

    val currentDrawable = background
    val backgroundDrawable = if (currentDrawable != null && currentDrawable is GradientDrawable) {
        // Reuse drawable
        currentDrawable
    } else {
        GradientDrawable()
    }

    backgroundDrawable.setColor(color)
    backgroundDrawable.setStroke(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, r.displayMetrics).toInt(), color.darkenColor())

    ViewCompat.setBackground(this, backgroundDrawable)
}

fun View.setSelectableBackgroundBorderless() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        setSelectableBackground(android.R.attr.selectableItemBackgroundBorderless)
    } else {
        setSelectableBackground()
    }
}

fun View.setSelectableBackground() {
    setSelectableBackground(android.R.attr.selectableItemBackground)
}

private fun View.setSelectableBackground(backgroundResId: Int) {
    val outValue = TypedValue()
    context.theme.resolveAttribute(backgroundResId, outValue, true)
    setBackgroundResource(outValue.resourceId)
    isClickable = true
    visibility = View.VISIBLE
}

fun View.setOrClearOnClickListener(clickable: Boolean, l: (View) -> Unit) {
    if (clickable) {
        setOnClickListener(l)
    } else {
        setOnClickListener { }
        isClickable = false
    }
}