package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.webkit.WebView;

/**
 * Copyright (c) Tuenti Technologies. All rights reserved.
 * <p>
 * WebView compatible with CoordinatorLayout.
 * The implementation based on NestedScrollView of design library
 * From: https://gist.github.com/alexmiragall/0c4c7163f7a17938518ce9794c4a5236
 */
public class NestedScrollWebView extends WebView implements NestedScrollingChild, NestedScrollingParent {

	private static final int INVALID_POINTER = -1;
	private static final String TAG = "NestedWebView";

	private final int[] scrollOffset = new int[2];
	private final int[] scrollConsumed = new int[2];

	private int lastMotionY;
	private final NestedScrollingChildHelper childHelper;
	private boolean isBeingDragged = false;
	private VelocityTracker velocityTracker;
	private int touchSlop;
	private int activePointerId = INVALID_POINTER;
	private int nestedYOffset;
	private ScrollerCompat scroller;
	private int minimumVelocity;
	private int maximumVelocity;

	public NestedScrollWebView(Context context) {
		this(context, null);
	}

	public NestedScrollWebView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.webViewStyle);
	}

	public NestedScrollWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setOverScrollMode(WebView.OVER_SCROLL_NEVER);
		initScrollView();
		childHelper = new NestedScrollingChildHelper(this);
		setNestedScrollingEnabled(true);
	}

	private void initScrollView() {
		scroller = ScrollerCompat.create(getContext(), null);
		final ViewConfiguration configuration = ViewConfiguration.get(getContext());
		touchSlop = configuration.getScaledTouchSlop();
		minimumVelocity = configuration.getScaledMinimumFlingVelocity();
		maximumVelocity = configuration.getScaledMaximumFlingVelocity();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {

		final int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE) && (isBeingDragged)) {
			return true;
		}

		switch (action & MotionEventCompat.ACTION_MASK) {
			case MotionEvent.ACTION_MOVE: {
				final int activePointerId = this.activePointerId;
				if (activePointerId == INVALID_POINTER) {
					break;
				}

				final int pointerIndex = ev.findPointerIndex(activePointerId);
				if (pointerIndex == -1) {
					Log.e(TAG, "Invalid pointerId=" + activePointerId
						+ " in onInterceptTouchEvent");
					break;
				}

				final int y = (int) ev.getY(pointerIndex);
				final int yDiff = Math.abs(y - lastMotionY);
				if (yDiff > touchSlop
					&& (getNestedScrollAxes() & ViewCompat.SCROLL_AXIS_VERTICAL) == 0) {
					isBeingDragged = true;
					lastMotionY = y;
					initVelocityTrackerIfNotExists();
					velocityTracker.addMovement(ev);
					nestedYOffset = 0;
					final ViewParent parent = getParent();
					if (parent != null) {
						parent.requestDisallowInterceptTouchEvent(true);
					}
				}
				break;
			}

			case MotionEvent.ACTION_DOWN: {

				lastMotionY = (int) ev.getY();
				activePointerId = ev.getPointerId(0);

				initOrResetVelocityTracker();
				velocityTracker.addMovement(ev);

				scroller.computeScrollOffset();
				isBeingDragged = !scroller.isFinished();
				startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
				break;
			}

			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				isBeingDragged = false;
				activePointerId = INVALID_POINTER;
				recycleVelocityTracker();
				if (scroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange())) {
					ViewCompat.postInvalidateOnAnimation(this);
				}
				stopNestedScroll();
				break;
			case MotionEventCompat.ACTION_POINTER_UP:
				onSecondaryPointerUp(ev);
				break;
		}

		return isBeingDragged;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		initVelocityTrackerIfNotExists();

		MotionEvent motionEvent = MotionEvent.obtain(ev);

		final int actionMasked = MotionEventCompat.getActionMasked(ev);

		if (actionMasked == MotionEvent.ACTION_DOWN) {
			nestedYOffset = 0;
		}
		motionEvent.offsetLocation(0, nestedYOffset);

		switch (actionMasked) {
			case MotionEvent.ACTION_DOWN: {
				if (isBeingDragged = !scroller.isFinished()) {
					final ViewParent parent = getParent();
					if (parent != null) {
						parent.requestDisallowInterceptTouchEvent(true);
					}
				}

				if (!scroller.isFinished()) {
					scroller.abortAnimation();
				}

				lastMotionY = (int) ev.getY();
				activePointerId = ev.getPointerId(0);
				startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
				break;
			}
			case MotionEvent.ACTION_MOVE:
				final int activePointerIndex = ev.findPointerIndex(activePointerId);
				if (activePointerIndex == -1) {
					Log.e(TAG, "Invalid pointerId=" + activePointerId + " in onTouchEvent");
					break;
				}

				final int y = (int) ev.getY(activePointerIndex);
				int deltaY = lastMotionY - y;
				if (dispatchNestedPreScroll(0, deltaY, scrollConsumed, scrollOffset)) {
					deltaY -= scrollConsumed[1];
					motionEvent.offsetLocation(0, scrollOffset[1]);
					nestedYOffset += scrollOffset[1];
				}
				if (!isBeingDragged && Math.abs(deltaY) > touchSlop) {
					final ViewParent parent = getParent();
					if (parent != null) {
						parent.requestDisallowInterceptTouchEvent(true);
					}
					isBeingDragged = true;
					if (deltaY > 0) {
						deltaY -= touchSlop;
					} else {
						deltaY += touchSlop;
					}
				}
				if (isBeingDragged) {
					lastMotionY = y - scrollOffset[1];

					final int oldY = getScrollY();
					final int scrolledDeltaY = getScrollY() - oldY;
					final int unconsumedY = deltaY - scrolledDeltaY;
					if (dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY, scrollOffset)) {
						lastMotionY -= scrollOffset[1];
						motionEvent.offsetLocation(0, scrollOffset[1]);
						nestedYOffset += scrollOffset[1];
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				if (isBeingDragged) {
					final VelocityTracker velocityTracker = this.velocityTracker;
					velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
					int initialVelocity = (int) VelocityTrackerCompat.getYVelocity(velocityTracker,
						activePointerId);

					if (Math.abs(initialVelocity) > minimumVelocity) {
						flingWithNestedDispatch(-initialVelocity);
					} else if (scroller.springBack(getScrollX(), getScrollY(), 0, 0, 0,
						getScrollRange())) {
						ViewCompat.postInvalidateOnAnimation(this);
					}
				}
				activePointerId = INVALID_POINTER;
				endDrag();
				break;
			case MotionEvent.ACTION_CANCEL:
				if (isBeingDragged && getChildCount() > 0) {
					if (scroller.springBack(getScrollX(), getScrollY(), 0, 0, 0,
						getScrollRange())) {
						ViewCompat.postInvalidateOnAnimation(this);
					}
				}
				activePointerId = INVALID_POINTER;
				endDrag();
				break;
			case MotionEventCompat.ACTION_POINTER_DOWN: {
				final int index = MotionEventCompat.getActionIndex(ev);
				lastMotionY = (int) ev.getY(index);
				activePointerId = ev.getPointerId(index);
				break;
			}
			case MotionEventCompat.ACTION_POINTER_UP:
				onSecondaryPointerUp(ev);
				lastMotionY = (int) ev.getY(ev.findPointerIndex(activePointerId));
				break;
		}

		if (velocityTracker != null) {
			velocityTracker.addMovement(motionEvent);
		}
		motionEvent.recycle();
		return super.onTouchEvent(ev);
	}

	int getScrollRange() {
		//Using scroll range of webView instead of child as NestedScrollView does.
		return computeVerticalScrollRange();
	}

	private void endDrag() {
		isBeingDragged = false;

		recycleVelocityTracker();
		stopNestedScroll();
	}

	private void onSecondaryPointerUp(MotionEvent ev) {
		final int pointerIndex = (ev.getAction() & MotionEventCompat.ACTION_POINTER_INDEX_MASK)
			>> MotionEventCompat.ACTION_POINTER_INDEX_SHIFT;
		final int pointerId = ev.getPointerId(pointerIndex);
		if (pointerId == activePointerId) {
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			lastMotionY = (int) ev.getY(newPointerIndex);
			activePointerId = ev.getPointerId(newPointerIndex);
			if (velocityTracker != null) {
				velocityTracker.clear();
			}
		}
	}

	private void initOrResetVelocityTracker() {
		if (velocityTracker == null) {
			velocityTracker = VelocityTracker.obtain();
		} else {
			velocityTracker.clear();
		}
	}

	private void initVelocityTrackerIfNotExists() {
		if (velocityTracker == null) {
			velocityTracker = VelocityTracker.obtain();
		}
	}

	private void recycleVelocityTracker() {
		if (velocityTracker != null) {
			velocityTracker.recycle();
			velocityTracker = null;
		}
	}

	private void flingWithNestedDispatch(int velocityY) {
		final int scrollY = getScrollY();
		final boolean canFling = (scrollY > 0 || velocityY > 0)
			&& (scrollY < getScrollRange() || velocityY < 0);
		if (!dispatchNestedPreFling(0, velocityY)) {
			dispatchNestedFling(0, velocityY, canFling);
			if (canFling) {
				fling(velocityY);
			}
		}
	}

	public void fling(int velocityY) {
		if (getChildCount() > 0) {
			int height = getHeight() - getPaddingBottom() - getPaddingTop();
			int bottom = getChildAt(0).getHeight();

			scroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, 0, 0,
				Math.max(0, bottom - height), 0, height / 2);

			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

	@Override
	public boolean isNestedScrollingEnabled() {
		return childHelper.isNestedScrollingEnabled();
	}

	@Override
	public void setNestedScrollingEnabled(boolean enabled) {
		childHelper.setNestedScrollingEnabled(enabled);
	}

	@Override
	public boolean startNestedScroll(int axes) {
		return childHelper.startNestedScroll(axes);
	}

	@Override
	public void stopNestedScroll() {
		childHelper.stopNestedScroll();
	}

	@Override
	public boolean hasNestedScrollingParent() {
		return childHelper.hasNestedScrollingParent();
	}

	@Override
	public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
										int[] offsetInWindow) {
		return childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
	}

	@Override
	public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
		return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
	}

	@Override
	public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
		return childHelper.dispatchNestedFling(velocityX, velocityY, consumed);
	}

	@Override
	public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
		return childHelper.dispatchNestedPreFling(velocityX, velocityY);
	}

	@Override
	public int getNestedScrollAxes() {
		return ViewCompat.SCROLL_AXIS_NONE;
	}
}