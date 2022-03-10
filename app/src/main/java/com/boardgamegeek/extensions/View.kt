package com.boardgamegeek.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.ViewCompat
import androidx.core.view.children
import com.boardgamegeek.R
import com.google.android.material.snackbar.Snackbar
import java.util.*
import kotlin.math.pow

fun View.fade(fadeIn: Boolean, animate: Boolean = true) {
    if (fadeIn) this.fadeIn(animate) else this.fadeOut(animate = animate)
}

fun View.fadeIn(animate: Boolean = true) {
    clearAnimation()
    if (animate) {
        val animationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
        apply {
            alpha = 0f
            visibility = VISIBLE
            animate()
                .alpha(1f)
                .setDuration(animationDuration.toLong())
                .setListener(null)
        }
    } else {
        visibility = VISIBLE
    }
}

fun View.fadeOut(visibility: Int = GONE, animate: Boolean = true) {
    clearAnimation()
    if (animate) {
        val animationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
        apply {
            animate()
                .alpha(0f)
                .setDuration(animationDuration.toLong())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        this@apply.visibility = visibility
                    }
                })
        }
    } else {
        clearAnimation()
        this.visibility = visibility
    }
}

fun View.slideUpIn() {
    val animation = AnimationUtils.loadAnimation(this.context, R.anim.slide_up)
    this.startAnimation(animation)
    this.visibility = VISIBLE
}

fun View.slideDownOut() {
    val animation = AnimationUtils.loadAnimation(this.context, R.anim.slide_down)
    animation.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation) {}
        override fun onAnimationEnd(animation: Animation) {
            visibility = GONE
        }

        override fun onAnimationRepeat(animation: Animation) {}
    })
    this.startAnimation(animation)
}

/**
 * Set the background of an {@link android.widget.ImageView} to an oval of the specified color, with a darker
 * version of the color as a border. For a {@link android.widget.TextView}, changes the text color instead. Doesn't
 * do anything for other views. Modified from Roman Nurik's DashClock (https://code.google.com/p/dashclock/).
 */
fun View.setColorViewValue(color: Int) {
    if (this is ImageView) {
        val currentDrawable = drawable
        val colorChoiceDrawable = if (currentDrawable is GradientDrawable) {
            // Reuse drawable
            currentDrawable
        } else {
            GradientDrawable().apply { shape = GradientDrawable.OVAL }
        }

        colorChoiceDrawable.setColor(color)
        colorChoiceDrawable.setStroke(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1f,
                resources.displayMetrics
            ).toInt(), color.darkenColor()
        )

        setImageDrawable(colorChoiceDrawable)
    } else if (this is TextView) {
        if (color != Color.TRANSPARENT) {
            setTextColor(color)
        }
    }
}

fun View.applyDarkScrim() {
    val color = ContextCompat.getColor(context, R.color.black_overlay)
    val drawable = makeCubicGradientScrimDrawable(color)
    ViewCompat.setBackground(this, drawable)
}

@SuppressLint("RtlHardcoded")
private fun makeCubicGradientScrimDrawable(
    @ColorInt baseColor: Int,
    numberOfStops: Int = 3,
    gravity: Int = Gravity.BOTTOM
): Drawable {
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
                Shader.TileMode.CLAMP
            )
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
    backgroundDrawable.cornerRadius = resources.getDimensionPixelSize(R.dimen.colored_background_corner_radius).toFloat()
    backgroundDrawable.setStroke(
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, r.displayMetrics).toInt(),
        color.darkenColor()
    )

    ViewCompat.setBackground(this, backgroundDrawable)
}

fun View.setSelectableBackgroundBorderless() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        setSelectableBackground(android.R.attr.selectableItemBackgroundBorderless)
    } else {
        setSelectableBackground()
    }
}

fun View.setSelectableBackground(backgroundResId: Int = android.R.attr.selectableItemBackground) {
    val outValue = TypedValue()
    context.theme.resolveAttribute(backgroundResId, outValue, true)
    setBackgroundResource(outValue.resourceId)
    isClickable = true
    visibility = VISIBLE
}

fun View.setOrClearOnClickListener(clickable: Boolean = false, l: (View) -> Unit = { }) {
    if (clickable) {
        setOnClickListener(l)
    } else {
        setOnClickListener { }
        isClickable = false
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun View.snackbar(messageResourceId: Int) = Snackbar
    .make(this, messageResourceId, Snackbar.LENGTH_SHORT)
    .apply { show() }

@Suppress("NOTHING_TO_INLINE")
inline fun View.snackbar(message: CharSequence) = Snackbar
    .make(this, message, Snackbar.LENGTH_SHORT)
    .apply { show() }

@Suppress("NOTHING_TO_INLINE")
inline fun View.longSnackbar(messageResourceId: Int) = Snackbar
    .make(this, messageResourceId, Snackbar.LENGTH_LONG)
    .apply { show() }

@Suppress("NOTHING_TO_INLINE")
inline fun View.longSnackbar(message: CharSequence) = Snackbar
    .make(this, message, Snackbar.LENGTH_LONG)
    .apply { show() }

@Suppress("NOTHING_TO_INLINE")
inline fun View.indefiniteSnackbar(message: CharSequence) = Snackbar
    .make(this, message, Snackbar.LENGTH_INDEFINITE)
    .apply { show() }

@Suppress("NOTHING_TO_INLINE")
inline fun View.indefiniteSnackbar(message: CharSequence, actionText: CharSequence, noinline action: (View) -> Unit) = Snackbar
    .make(this, message, Snackbar.LENGTH_INDEFINITE)
    .setAction(actionText, action)
    .apply { show() }

fun View.childrenRecursiveSequence(): Sequence<View> = ViewChildrenRecursiveSequence(this)

private class ViewChildrenRecursiveSequence(private val view: View) : Sequence<View> {
    override fun iterator(): Iterator<View> {
        return if (view is ViewGroup) RecursiveViewIterator(view) else emptyList<View>().iterator()
    }

    private class RecursiveViewIterator(view: ViewGroup) : Iterator<View> {
        private val sequences = arrayListOf(view.children)
        private var current = sequences.removeLast().iterator()

        override fun next(): View {
            if (!hasNext()) throw NoSuchElementException()
            val view = current.next()
            if (view is ViewGroup && view.childCount > 0) {
                sequences.add(view.children)
            }
            return view
        }

        override fun hasNext(): Boolean {
            if (!current.hasNext() && sequences.isNotEmpty()) {
                current = sequences.removeLast().iterator()
            }
            return current.hasNext()
        }
    }
}
