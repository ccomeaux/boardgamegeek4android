package com.boardgamegeek.events;

public class GameInfoChangedEvent {
	private final String gameName;
	private final String subtype;
	private final String thumbnailUrl;
	private final String imageUrl;
	private final boolean arePlayersCustomSorted;

	public GameInfoChangedEvent(String gameName, String subtype, String imageUrl, String thumbnailUrl, boolean arePlayersCustomSorted) {
		this.gameName = gameName;
		this.subtype = subtype;
		this.thumbnailUrl = thumbnailUrl;
		this.imageUrl = imageUrl;
		this.arePlayersCustomSorted = arePlayersCustomSorted;
	}

	public String getGameName() {
		return gameName;
	}

	public String getSubtype() {
		return subtype;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public boolean arePlayersCustomSorted() {
		return arePlayersCustomSorted;
	}
}
