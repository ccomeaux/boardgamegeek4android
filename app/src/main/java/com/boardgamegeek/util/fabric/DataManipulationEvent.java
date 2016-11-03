package com.boardgamegeek.util.fabric;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

public class DataManipulationEvent extends CustomEvent {
	protected DataManipulationEvent(String contentType) {
		super("DataManipulation");
		putCustomAttribute("contentType", contentType);
	}

	public static void log(String contentType, String action) {
		DataManipulationEvent event = new DataManipulationEvent(contentType);
		event.putCustomAttribute("Action", action);
		Answers.getInstance().logCustom(event);
	}
}
