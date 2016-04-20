package com.boardgamegeek.model;

import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class PlayDeleteResponse extends PlayPostResponse {

	private PlayDelete playDelete;

	public PlayDeleteResponse(OkHttpClient client, Request request) {
		super(client, request);
	}

	@Override
	protected void saveContent(String content) {
		Gson gson = new Gson();
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
