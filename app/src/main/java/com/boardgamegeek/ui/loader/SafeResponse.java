package com.boardgamegeek.ui.loader;

import android.text.TextUtils;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class SafeResponse<T> {
	private T body;
	private String errorMessage;

	public SafeResponse(Call<T> call) {
		try {
			final Response<T> response = call.execute();
			if (response.isSuccessful()) {
				body = response.body();
			} else {
				errorMessage = "Error code " + response.code();
			}
		} catch (IOException e) {
			errorMessage = e.getMessage();
		}
	}

	public T getBody() {
		return body;
	}

	public boolean hasError() {
		return !TextUtils.isEmpty(errorMessage);
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
