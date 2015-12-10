package com.boardgamegeek.io;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.model.PlayPostResponse;

public class PlayDeleteConverter extends PostConverter {
	public PlayDeleteConverter() {
	}

	@NonNull
	@Override
	protected Object convertContent(String content) {
		String errorMessage = extractErrorMessage(content);
		if (!TextUtils.isEmpty(errorMessage)) {
			return new PlayPostResponse(errorMessage);
		}
		if (content.contains("<title>Plays") && content.contains("User:")) {
			// This is the response if the delete was successful
			return new PlayPostResponse("");
		} else {
			// This probably means the user wasn't authenticated
			return new PlayPostResponse("You must login to save plays");
		}
	}
}
