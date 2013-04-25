package com.boardgamegeek.util;

import android.content.Context;
import android.view.View;

public class AnimationUtils {

	public static void fadeIn(Context context, View view, boolean animate) {
		if (view == null || view.getVisibility() == View.VISIBLE)
			return;

		if (animate) {
			view.startAnimation(android.view.animation.AnimationUtils.loadAnimation(context, android.R.anim.fade_in));
		} else {
			view.clearAnimation();
		}
		view.setVisibility(View.VISIBLE);
	}

	public static void fadeOut(Context context, View view, boolean animate) {
		if (view == null || view.getVisibility() != View.VISIBLE)
			return;

		if (animate) {
			view.startAnimation(android.view.animation.AnimationUtils.loadAnimation(context, android.R.anim.fade_out));
		} else {
			view.clearAnimation();
		}
		view.setVisibility(View.GONE);
	}
}
