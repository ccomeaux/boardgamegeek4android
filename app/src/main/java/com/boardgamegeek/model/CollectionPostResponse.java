package com.boardgamegeek.model;

import android.text.TextUtils;

public class CollectionPostResponse {
	public static final double INVALID_RATING = -1.0;
	private static final String AUTH_ERROR_TEXT = "You must login to use the collection utilities.";
	private double rating = INVALID_RATING;
	private String error;
	private Exception exception;

	public CollectionPostResponse(double rating) {
		this.rating = rating;
	}

	public CollectionPostResponse(String errorMessage) {
		this.error = errorMessage;
	}

	public CollectionPostResponse(Exception e) {
		exception = e;
	}

	public double getRating() {
		return this.rating;
	}

	public boolean hasError() {
		return !TextUtils.isEmpty(getErrorMessage());
	}

	public boolean hasAuthError() {
		return AUTH_ERROR_TEXT.equals(error);
	}

	public String getErrorMessage() {
		if (exception != null) {
			return exception.getMessage();
		}
		return error;
	}
}
