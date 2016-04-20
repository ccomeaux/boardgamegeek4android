package com.boardgamegeek.model;

import android.text.Html;
import android.text.TextUtils;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class CollectionPostResponse {
	private static final String ERROR_DIV = "<div class='messagebox error'>";
	private static final String AUTH_ERROR_TEXT = "You must login to use the collection utilities.";
	protected String error;
	protected Exception exception;

	public CollectionPostResponse(OkHttpClient client, Request request) {
		try {
			Response response = client.newCall(request).execute();
			if (response.isSuccessful()) {
				final String content = response.body().string();
				if (content.startsWith(ERROR_DIV)) {
					error = Html.fromHtml(content).toString().trim();
				}
				saveContent(content);
			} else {
				error = "Unsuccessful post: " + response.code();
			}
		} catch (IOException e) {
			exception = e;
		}
	}

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

	protected abstract void saveContent(String content);
}
