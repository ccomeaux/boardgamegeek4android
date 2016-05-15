package com.boardgamegeek.ui.widget;

import android.graphics.Point;
import android.support.annotation.IdRes;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

public class ToolbarActionItemTarget implements Target {
	private final Toolbar toolbar;
	private final int menuItemId;

	public ToolbarActionItemTarget(@IdRes int itemId, Toolbar toolbar) {
		this.menuItemId = itemId;
		this.toolbar = toolbar;
	}

	@Override
	public Point getPoint() {
		final View view = toolbar.findViewById(menuItemId);
		if (view == null) return Target.NONE.getPoint();
		final ViewTarget viewTarget = new ViewTarget(view);
		return viewTarget.getPoint();
	}
}
