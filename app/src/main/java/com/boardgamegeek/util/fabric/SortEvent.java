package com.boardgamegeek.util.fabric;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

public class SortEvent extends CustomEvent {
	public SortEvent() {
		super("Sort");
	}

	public static void log(String contentType, String sortBy) {
		SortEvent event = newInstance(contentType);
		event.putCustomAttribute("SortBy", sortBy);
		Answers.getInstance().logCustom(event);
	}

	private static SortEvent newInstance(String contentType) {
		SortEvent event = new SortEvent();
		event.putCustomAttribute("contentType", contentType);
		return event;
	}

}
