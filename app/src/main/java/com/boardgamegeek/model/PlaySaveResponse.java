package com.boardgamegeek.model;

import com.boardgamegeek.provider.BggContract;

import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class PlaySaveResponse extends PlayPostResponse {
	private PlaySave playSave;

	public PlaySaveResponse(@NonNull OkHttpClient client, @NonNull Request request) {
		super(client, request);
	}

	@Override
	protected void saveContent(String content) {
		playSave = gson.fromJson(content, PlaySave.class);
		error = playSave.error;
	}

	public int getPlayCount() {
		if (hasError() || playSave == null) {
			return -1;
		} else {
			return playSave.getNumberOfPlays();
		}
	}

	public int getPlayId() {
		if (hasError() || playSave == null) {
			return BggContract.INVALID_ID;
		} else {
			return playSave.getPlayId();
		}
	}

	private static class PlaySave {
		@SuppressWarnings("unused") private int playid;
		@SuppressWarnings("unused") private int numplays;
		@SuppressWarnings("unused") private String html;
		@SuppressWarnings("unused") private String error;

		public int getPlayId() {
			return playid;
		}

		public int getNumberOfPlays() {
			return numplays;
		}
	}
}
