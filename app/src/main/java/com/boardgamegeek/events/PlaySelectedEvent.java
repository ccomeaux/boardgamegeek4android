package com.boardgamegeek.events;

public class PlaySelectedEvent {
	private final long internalId;
	private final int playId;
	private final int gameId;
	private final String gameName;
	private final String thumbnailUrl;
	private final String imageUrl;

	public PlaySelectedEvent(long internalId, int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		this.internalId = internalId;
		this.playId = playId;
		this.gameId = gameId;
		this.gameName = gameName;
		this.thumbnailUrl = thumbnailUrl;
		this.imageUrl = imageUrl;
	}

	public long getInternalId() {
		return internalId;
	}

	public int getPlayId() {
		return playId;
	}

	public int getGameId() {
		return gameId;
	}

	public String getGameName() {
		return gameName;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public String getImageUrl() {
		return imageUrl;
	}
}
