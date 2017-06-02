package com.boardgamegeek.events;

public class GameInfoChangedEvent {
	private final String gameName;
	private final String subtype;
	private final String thumbnailUrl;
	private final String imageUrl;
	private final boolean arePlayersCustomSorted;
	private final boolean isFavorite;

	public GameInfoChangedEvent(String gameName, String subtype, String imageUrl, String thumbnailUrl, boolean arePlayersCustomSorted, boolean isFavorite) {
		this.gameName = gameName;
		this.subtype = subtype;
		this.thumbnailUrl = thumbnailUrl;
		this.imageUrl = imageUrl;
		this.arePlayersCustomSorted = arePlayersCustomSorted;
		this.isFavorite = isFavorite;
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

	public boolean isFavorite() {
		return isFavorite;
	}
}
