package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

/**
 * ContentLoadingProgressBar implements a ProgressBar that waits a minimum time to be
 * dismissed before showing. Once visible, the progress bar will be visible for
 * a minimum amount of time to avoid "flashes" in the UI when an event could take
 * a largely variable time to complete (from none, to a user perceivable amount).
 * <p/>
 * This version is similar to the support library version but implemented "the right way".
 *
 * @author Christophe Beyls
 */
public class ContentLoadingProgressBar extends ProgressBar {
	private static final long MIN_SHOW_TIME = 500L; // ms
	private static final long MIN_DELAY = 500L; // ms

	private boolean isAttachedToWindow = false;
	private boolean isShown;
	long startTime = -1L;

	private final Runnable delayedHide = new Runnable() {
		@Override
		public void run() {
			setVisibility(GONE);
			startTime = -1L;
		}
	};

	private final Runnable delayedShow = new Runnable() {
		@Override
		public void run() {
			startTime = SystemClock.uptimeMillis();
			setVisibility(VISIBLE);
		}
	};

	public ContentLoadingProgressBar(Context context) {
		this(context, null, 0);
	}

	public ContentLoadingProgressBar(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ContentLoadingProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		isShown = getVisibility() == VISIBLE;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		isAttachedToWindow = true;
		if (isShown && (getVisibility() != VISIBLE)) {
			postDelayed(delayedShow, MIN_DELAY);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		isAttachedToWindow = false;
		removeCallbacks(delayedHide);
		removeCallbacks(delayedShow);
		if (!isShown && startTime != -1L) setVisibility(GONE);
		startTime = -1L;
	}

	/**
	 * Hide the progress view if it is visible. The progress view will not be
	 * hidden until it has been shown for at least a minimum show time. If the
	 * progress view was not yet visible, cancels showing the progress view.
	 */
	public void hide() {
		if (isShown) {
			isShown = false;
			if (isAttachedToWindow) removeCallbacks(delayedShow);
			long diff = SystemClock.uptimeMillis() - startTime;
			if (startTime == -1L || diff >= MIN_SHOW_TIME) {
				// The progress spinner has been shown long enough
				// OR was not shown yet. If it wasn't shown yet,
				// it will just never be shown.
				setVisibility(View.GONE);
				startTime = -1L;
			} else {
				// The progress spinner is shown, but not long enough,
				// so put a delayed message in to hide it when its been
				// shown long enough.
				postDelayed(delayedHide, MIN_SHOW_TIME - diff);
			}
		}
	}

	/**
	 * Show the progress view after waiting for a minimum delay. If
	 * during that time, hide() is called, the view is never made visible.
	 */
	public void show() {
		if (!isShown) {
			isShown = true;
			if (isAttachedToWindow) {
				removeCallbacks(delayedHide);
				if (startTime == -1L) {
					postDelayed(delayedShow, MIN_DELAY);
				}
			}
		}
	}
}