package com.boardgamegeek.util.fabric;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

public class FilterEvent extends CustomEvent {
	public FilterEvent() {
		super("Filter");
	}

	public static void log(String contentType, String filterBy) {
		FilterEvent event = newInstance(contentType);
		event.putCustomAttribute("FilterBy", filterBy);
		Answers.getInstance().logCustom(event);
	}

	private static FilterEvent newInstance(String contentType) {
		FilterEvent event = new FilterEvent();
		event.putCustomAttribute("contentType", contentType);
		return event;
	}

}
