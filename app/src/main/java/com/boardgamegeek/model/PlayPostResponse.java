package com.boardgamegeek.model;

import android.text.Html;
import android.text.TextUtils;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class PlayPostResponse {
	protected static final String ERROR_DIV = "<div class='messagebox error'>";
	protected String error;
	protected Exception exception;

	public PlayPostResponse(OkHttpClient client, Request request) {
		try {
			Response response = client.newCall(request).execute();
			if (response.isSuccessful()) {
				final String content = response.body().string().trim();
				if (content.startsWith(ERROR_DIV)) {
					//noinspection deprecation
					error = Html.fromHtml(content).toString().trim();
				} else {
					saveContent(content);
				}
			} else {
				error = "Unsuccessful post: " + response.code();
			}
		} catch (IOException | IllegalStateException e) {
			exception = e;
		}
	}

	protected abstract void saveContent(String content);

	public boolean hasError() {
		return !TextUtils.isEmpty(getErrorMessage());
	}

	/**
	 * Indicates the user attempted to modify a play without being authenticated.
	 */
	public boolean hasAuthError() {
		return "You must login to save plays".equalsIgnoreCase(error) ||
			"You can't delete this play".equalsIgnoreCase(error);
	}

	/**
	 * Indicates the user attempted to modify a play that doesn't exist.
	 */
	public boolean hasInvalidIdError() {
		if ("You are not permitted to edit this play.".equalsIgnoreCase(error)) {
			return true;
		} else if ("Play does not exist.".equalsIgnoreCase(error)) {
			return true;
		}
		return false;
	}

	public String getErrorMessage() {
		if (exception != null) {
			return exception.getMessage();
		}
		return error;
	}
}
