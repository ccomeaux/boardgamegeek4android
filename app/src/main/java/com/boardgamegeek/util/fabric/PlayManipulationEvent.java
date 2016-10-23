package com.boardgamegeek.util.fabric;

import com.crashlytics.android.answers.Answers;

public class PlayManipulationEvent extends DataManipulationEvent {
	public static void log(String action) {
		PlayManipulationEvent event = newInstance(action);
		Answers.getInstance().logCustom(event);
	}

	public static void log(String action, String gameName) {
		PlayManipulationEvent event = newInstance(action);
		event.putCustomAttribute("GameName", gameName);
		Answers.getInstance().logCustom(event);
	}

	protected static PlayManipulationEvent newInstance(String action) {
		return (PlayManipulationEvent) DataManipulationEvent
			.newInstance("Play")
			.putCustomAttribute("action", action);
	}
}
