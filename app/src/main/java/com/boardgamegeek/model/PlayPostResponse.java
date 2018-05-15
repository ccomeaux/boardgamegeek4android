package com.boardgamegeek.model;

import android.support.annotation.NonNull;
import android.text.Html;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

public abstract class PlayPostResponse {
	protected final Gson gson = new Gson();
	protected static final String ERROR_DIV = "<div class='messagebox error'>";
	protected String error;
	protected Exception exception;

	public PlayPostResponse(@NonNull OkHttpClient client, @NonNull Request request) {
		try {
			Response response = client.newCall(request).execute();
			if (response.isSuccessful()) {
				final ResponseBody body = response.body();
				final String content = body == null ? "" : body.string().trim();
				if (content.startsWith(ERROR_DIV)) {
					//noinspection deprecation
					error = Html.fromHtml(content).toString().trim();
				} else {
					try {
						saveContent(content);
					} catch (IllegalStateException e) {
						Timber.w("Couldn't parse JSON - %s", content);
						throw e;
					}
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
			"You can't delete this play".equalsIgnoreCase(error) ||
			"You are not permitted to edit this play.".equalsIgnoreCase(error);
	}

	/**
	 * Indicates the user attempted to modify a play that doesn't exist.
	 */
	public boolean hasInvalidIdError() {
		return "Play does not exist.".equalsIgnoreCase(error);
	}

	public String getErrorMessage() {
		if (exception != null) {
			return exception.getMessage();
		}
		return error;
	}
}
