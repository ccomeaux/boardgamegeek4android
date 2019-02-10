package com.boardgamegeek.model;

import androidx.annotation.NonNull;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class PlayDeleteResponse extends PlayPostResponse {
	private PlayDelete playDelete;

	public PlayDeleteResponse(@NonNull OkHttpClient client, @NonNull Request request) {
		super(client, request);
	}

	@Override
	protected void saveContent(String content) {
		playDelete = gson.fromJson(content, PlayDelete.class);
	}

	public boolean isSuccessful() {
		return !(hasError() || playDelete == null) && playDelete.isSuccessful();
	}

	private static class PlayDelete {
		@SuppressWarnings("unused") private boolean success;

		public boolean isSuccessful() {
			return success;
		}
	}
}
