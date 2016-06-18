package com.boardgamegeek.ui.widget;

import android.os.Handler;

import hugo.weaving.DebugLog;

public class MinuteUpdater {

	public interface Callback {
		void updateTimeBasedUi();
	}

	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec

	private Handler timeHintUpdateHandler = new Handler();
	private Runnable timeHintUpdateRunnable = null;
	private Callback callback;

	public MinuteUpdater(Callback callback) {
		this.callback = callback;
		initialize();
	}

	@DebugLog
	public void initialize() {
		if (callback != null) {
			callback.updateTimeBasedUi();
		}
		if (timeHintUpdateRunnable != null) {
			timeHintUpdateHandler.removeCallbacks(timeHintUpdateRunnable);
		}
		timeHintUpdateRunnable = new Runnable() {
			@Override
			public void run() {
				if (callback != null) {
					callback.updateTimeBasedUi();
				}
				timeHintUpdateHandler.postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);
			}
		};
		timeHintUpdateHandler.postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);
	}
}
