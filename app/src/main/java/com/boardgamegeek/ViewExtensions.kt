package com.boardgamegeek

import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.Animation
import android.view.animation.AnimationUtils
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
