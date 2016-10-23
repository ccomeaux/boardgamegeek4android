package com.boardgamegeek.util.fabric;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

public class AddFieldEvent extends CustomEvent {
	public AddFieldEvent() {
		super("AddField");
	}

	public static void log(String contentType, String sortBy) {
		AddFieldEvent event = newInstance(contentType);
		event.putCustomAttribute("SortBy", sortBy);
		Answers.getInstance().logCustom(event);
	}

	private static AddFieldEvent newInstance(String contentType) {
		AddFieldEvent event = new AddFieldEvent();
		event.putCustomAttribute("contentType", contentType);
		return event;
	}

}
