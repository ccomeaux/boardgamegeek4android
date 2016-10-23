package com.boardgamegeek.util.fabric;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

public class DataManipulationEvent extends CustomEvent {
	public DataManipulationEvent() {
		super("DataManipulation");
	}

	public static void log(String contentType, String action) {
		DataManipulationEvent event = newInstance(contentType);
		event.putCustomAttribute("Action", action);
		Answers.getInstance().logCustom(event);
	}

	protected static DataManipulationEvent newInstance(String contentType) {
		DataManipulationEvent event = new DataManipulationEvent();
		event.putCustomAttribute("contentType", contentType);
		return event;
	}
}
