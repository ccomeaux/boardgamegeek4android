package com.boardgamegeek.ui.widget;

import android.app.Activity;
import android.graphics.Point;
import android.view.View;

import com.github.amlcurran.showcaseview.targets.Target;

public class SafeViewTarget implements Target {

	private View view;
	private int viewId;
	private Activity activity;

	public SafeViewTarget(View view) {
		this.view = view;
	}

	public SafeViewTarget(int viewId, Activity activity) {
		this.viewId = viewId;
		this.activity = activity;
		view = activity.findViewById(viewId);
	}

	@Override
	public Point getPoint() {
		if (view == null && activity != null) {
			view = activity.findViewById(this.viewId);
		}
		if (view == null) {
			return Target.NONE.getPoint();
		}
		int[] location = new int[2];
		view.getLocationInWindow(location);
		int x = location[0] + view.getWidth() / 2;
		int y = location[1] + view.getHeight() / 2;
		return new Point(x, y);
	}
}
