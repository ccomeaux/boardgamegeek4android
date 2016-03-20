package com.boardgamegeek.model;

import android.text.TextUtils;

public class CollectionPostResponse {
	private static final String AUTH_ERROR_TEXT = "You must login to use the collection utilities.";
	protected String error;
	protected Exception exception;

	public boolean hasError() {
		return !TextUtils.isEmpty(getErrorMessage());
	}

	public boolean hasAuthError() {
		return AUTH_ERROR_TEXT.equals(error);
	}

	public String getErrorMessage() {
		if (exception != null) {
			return getExceptionMessage();
		}
		return error;
	}

	private String getExceptionMessage() {
		String message = exception.getMessage();
		if (!TextUtils.isEmpty(message)) {
			return message;
		}
		return exception.toString();
	}
}
