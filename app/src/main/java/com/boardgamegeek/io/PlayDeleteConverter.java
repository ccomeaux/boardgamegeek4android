package com.boardgamegeek.io;

import android.text.TextUtils;

import com.boardgamegeek.model.PlayPostResponse;

import java.lang.reflect.Type;

import retrofit.converter.ConversionException;
import retrofit.mime.TypedInput;

public class PlayDeleteConverter extends PostConverter {
	public PlayDeleteConverter() {
	}

	@Override
	public Object fromBody(TypedInput body, Type type) throws ConversionException {
		markBody(body);
		String content = getContent(body);
		if (typeIsExpected(type)) {
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
		throw new ConversionException(content);
	}
}
