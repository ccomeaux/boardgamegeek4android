package com.boardgamegeek.events;

public class CollectionItemChangedEvent {
	private final String collectionName;
	private final String thumbnailUrl;
	private final String imageUrl;

	public CollectionItemChangedEvent(String collectionName, String imageUrl, String thumbnailUrl) {
		this.collectionName = collectionName;
		this.thumbnailUrl = thumbnailUrl;
		this.imageUrl = imageUrl;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}
}
