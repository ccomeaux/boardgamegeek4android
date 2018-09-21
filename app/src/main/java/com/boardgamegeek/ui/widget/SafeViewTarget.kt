package com.boardgamegeek.ui.widget

import android.app.Activity
import android.graphics.Point
import android.view.View

import com.github.amlcurran.showcaseview.targets.Target

class SafeViewTarget : Target {
    private var view: View?
    private val viewId: Int
    private val activity: Activity?

    constructor(view: View) {
        this.view = view
        this.viewId = 0
        this.activity = null
    }

    constructor(viewId: Int, activity: Activity) {
        this.viewId = viewId
        this.activity = activity
        this.view = activity.findViewById(viewId)
    }

    override fun getPoint(): Point {
        if (view == null && activity != null && viewId != 0) {
            view = activity.findViewById(this.viewId)
        }
        return if (view == null) {
            Target.NONE.point
        } else {
            val v = view as View
            val location = IntArray(2)
            v.getLocationInWindow(location)
            val x = location[0] + v.width / 2
            val y = location[1] + v.height / 2
            Point(x, y)
        }
    }
}
