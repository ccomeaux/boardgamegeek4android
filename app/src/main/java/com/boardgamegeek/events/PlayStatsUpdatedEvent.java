package com.boardgamegeek.events;

import com.boardgamegeek.ui.model.PlayStats;

public class PlayStatsUpdatedEvent {
	private final PlayStats playStats;

	public PlayStatsUpdatedEvent(PlayStats playStats) {
		this.playStats = playStats;
	}

	public PlayStats getPlayStats() {
		return playStats;
	}
}
