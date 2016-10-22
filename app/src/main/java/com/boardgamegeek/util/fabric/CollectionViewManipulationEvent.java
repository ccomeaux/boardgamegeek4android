package com.boardgamegeek.util.fabric;

import com.crashlytics.android.answers.Answers;

public class CollectionViewManipulationEvent extends DataManipulationEvent {
	public static void log(String action) {
		CollectionViewManipulationEvent event = newInstance(action);
		Answers.getInstance().logCustom(event);
	}

	public static void log(String action, String viewName) {
		CollectionViewManipulationEvent event = newInstance(action);
		event.putCustomAttribute("ViewName", viewName);
		Answers.getInstance().logCustom(event);
	}

	protected static CollectionViewManipulationEvent newInstance(String action) {
		return (CollectionViewManipulationEvent) DataManipulationEvent
			.newInstance("CollectionView")
			.putCustomAttribute("action", action);
	}
}
