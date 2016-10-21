package com.boardgamegeek.util.fabric;

import com.crashlytics.android.answers.Answers;

public class PlayerColorsManipulationEvent extends DataManipulationEvent {
	public static void log(String action) {
		PlayerColorsManipulationEvent event = newInstance(action);
		Answers.getInstance().logCustom(event);
	}

	public static void log(String action, String color) {
		PlayerColorsManipulationEvent event = newInstance(action);
		event.putCustomAttribute("Color", color);
		Answers.getInstance().logCustom(event);
	}

	protected static PlayerColorsManipulationEvent newInstance(String action) {
		return (PlayerColorsManipulationEvent) DataManipulationEvent
			.newInstance("PlayerColors")
			.putCustomAttribute("action", action);
	}
}
