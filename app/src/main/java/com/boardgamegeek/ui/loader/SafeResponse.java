package com.boardgamegeek.ui.loader;

import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParserException;

import retrofit2.Call;
import retrofit2.Response;

public class SafeResponse<T> {
	protected T body;
	private String errorMessage;
	private boolean hasParseError;

	public SafeResponse(Call<T> call) {
		hasParseError = false;
		try {
			final Response<T> response = call.execute();
			if (response.isSuccessful()) {
				body = response.body();
			} else {
				errorMessage = "Error code " + response.code();
			}
		} catch (Exception e) {
			if (e instanceof RuntimeException && e.getCause() instanceof XmlPullParserException) {
				hasParseError = true;
				errorMessage = e.getCause().getMessage();
			} else {
				errorMessage = e.getMessage();
			}
		}
	}

	public T getBody() {
		return body;
	}

	public boolean hasError() {
		return !TextUtils.isEmpty(errorMessage);
	}

	public boolean hasParseError() {
		return hasParseError;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
