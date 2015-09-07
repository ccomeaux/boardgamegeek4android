package com.boardgamegeek.events;

public class SearchResultsCountChangedEvent extends CountChangedEvent {
	public SearchResultsCountChangedEvent(int count) {
		super(count);
	}
}
