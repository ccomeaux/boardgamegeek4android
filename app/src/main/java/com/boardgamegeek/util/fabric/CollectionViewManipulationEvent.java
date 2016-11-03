package com.boardgamegeek.util.fabric;

import com.crashlytics.android.answers.Answers;

public class CollectionViewManipulationEvent extends DataManipulationEvent {
	public static void log(String action) {
		CollectionViewManipulationEvent event = new CollectionViewManipulationEvent(action);
		Answers.getInstance().logCustom(event);
	}

	public static void log(String action, String viewName) {
		CollectionViewManipulationEvent event = new CollectionViewManipulationEvent(action);
		event.putCustomAttribute("ViewName", viewName);
		Answers.getInstance().logCustom(event);
	}

	protected CollectionViewManipulationEvent(String action) {
		super("CollectionView");
		putCustomAttribute("action", action);
	}
}
