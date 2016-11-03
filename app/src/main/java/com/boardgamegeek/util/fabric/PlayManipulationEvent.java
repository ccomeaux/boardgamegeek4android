package com.boardgamegeek.util.fabric;

import com.crashlytics.android.answers.Answers;

public class PlayManipulationEvent extends DataManipulationEvent {
	public static void log(String action, String gameName) {
		PlayManipulationEvent event = new PlayManipulationEvent(action);
		event.putCustomAttribute("GameName", gameName);
		Answers.getInstance().logCustom(event);
	}

	protected PlayManipulationEvent(String action) {
		super("Play");
		putCustomAttribute("action", action);
	}
}
