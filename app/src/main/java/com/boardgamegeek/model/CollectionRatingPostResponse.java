package com.boardgamegeek.model;

public class CollectionRatingPostResponse extends CollectionPostResponse {
	public static final double INVALID_RATING = -1.0;
	private double rating = INVALID_RATING;

	public CollectionRatingPostResponse(double rating) {
		this.rating = rating;
	}

	public CollectionRatingPostResponse(String errorMessage) {
		this.error = errorMessage;
	}

	public CollectionRatingPostResponse(Exception e) {
		exception = e;
	}

	public double getRating() {
		return this.rating;
	}

}
