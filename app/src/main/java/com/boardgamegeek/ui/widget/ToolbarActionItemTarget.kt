package com.boardgamegeek.ui.widget

import android.graphics.Point
import android.support.annotation.IdRes
import android.support.v7.widget.Toolbar
import android.view.View

import com.github.amlcurran.showcaseview.targets.Target
import com.github.amlcurran.showcaseview.targets.ViewTarget

class ToolbarActionItemTarget(@param:IdRes private val menuItemId: Int, private val toolbar: Toolbar) : Target {
    override fun getPoint(): Point {
        val view = toolbar.findViewById<View>(menuItemId) ?: return Target.NONE.point
        return ViewTarget(view).point
    }
}
