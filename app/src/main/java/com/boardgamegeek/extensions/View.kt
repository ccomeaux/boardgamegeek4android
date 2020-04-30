package com.boardgamegeek.extensions

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
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
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.ViewCompat
import com.boardgamegeek.R
import com.boardgamegeek.util.ColorUtils
import kotlin.math.pow

fun View.fade(fadeIn: Boolean, animate: Boolean = true) {
    if (fadeIn) this.fadeIn(animate) else this.fadeOut(animate = animate)
}

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
    val drawable = makeCubicGradientScrimDrawable(color)
    ViewCompat.setBackground(this, drawable)
}

@SuppressLint("RtlHardcoded")
private fun makeCubicGradientScrimDrawable(@ColorInt baseColor: Int, numberOfStops: Int = 3, gravity: Int = Gravity.BOTTOM): Drawable {
    val numStops = numberOfStops.coerceAtLeast(2)
    val paintDrawable = PaintDrawable().apply {
        shape = RectShape()
    }
    val stopColors = IntArray(numStops)

    for (i in 0 until numStops) {
        val opacity = (i * 1f / (numStops - 1)).toDouble().pow(3).toFloat().coerceIn(0f, 1f)
        stopColors[i] = Color.argb((baseColor.alpha * opacity).toInt(), baseColor.red, baseColor.green, baseColor.blue)
    }

    val x0 = if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.LEFT) 1f else 0f
    val x1 = if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.RIGHT) 1f else 0f
    val y0 = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.TOP) 1f else 0f
    val y1 = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.BOTTOM) 1f else 0f

    paintDrawable.shaderFactory = object : ShapeDrawable.ShaderFactory() {
        override fun resize(width: Int, height: Int): Shader {
            return LinearGradient(
                    width * x0,
                    height * y0,
                    width * x1,
                    height * y1,
                    stopColors,
                    null,
                    Shader.TileMode.CLAMP)
        }
    }

    return paintDrawable
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

@JvmOverloads
fun View.setSelectableBackground(backgroundResId: Int = android.R.attr.selectableItemBackground) {
    val outValue = TypedValue()
    context.theme.resolveAttribute(backgroundResId, outValue, true)
    setBackgroundResource(outValue.resourceId)
    isClickable = true
    visibility = VISIBLE
}

fun View.setOrClearOnClickListener(clickable: Boolean, l: (View) -> Unit) {
    if (clickable) {
        setOnClickListener(l)
    } else {
        setOnClickListener { }
        isClickable = false
    }
}
