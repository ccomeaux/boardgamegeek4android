package com.boardgamegeek.events;

public class PlaySelectedEvent {
	public int playId;
	public int gameId;
	public String gameName;
	public String thumbnailUrl;
	public String imageUrl;

	public PlaySelectedEvent(int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		this.playId = playId;
		this.gameId = gameId;
		this.gameName = gameName;
		this.thumbnailUrl = thumbnailUrl;
		this.imageUrl = imageUrl;
	}
}
