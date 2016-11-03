package com.boardgamegeek.util.fabric;

import com.crashlytics.android.answers.Answers;

public class PlayerColorsManipulationEvent extends DataManipulationEvent {
	public static void log(String action) {
		PlayerColorsManipulationEvent event = new PlayerColorsManipulationEvent(action);
		Answers.getInstance().logCustom(event);
	}

	public static void log(String action, String color) {
		PlayerColorsManipulationEvent event = new PlayerColorsManipulationEvent(action);
		event.putCustomAttribute("Color", color);
		Answers.getInstance().logCustom(event);
	}

	protected PlayerColorsManipulationEvent(String action) {
		super("PlayerColors");
		putCustomAttribute("action", action);
	}
}
