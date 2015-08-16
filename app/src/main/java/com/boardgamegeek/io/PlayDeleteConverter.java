package com.boardgamegeek.io;

import android.text.TextUtils;

import com.boardgamegeek.model.PlayPostResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

public class PlayDeleteConverter implements Converter {
	private static final String ERROR_DIV = "<div class='messagebox error'>";
	private static final String CLASS_NAME = "class com.boardgamegeek.model.PlayPostResponse";

	public PlayDeleteConverter() {
	}

	@Override
	public Object fromBody(TypedInput body, Type type) throws ConversionException {
		try {
			if (body.in().markSupported()) {
				body.in().mark(10000);
			}
		} catch (IOException e) {
			throw new ConversionException(e);
		}
		try {
			StringBuilder sb = new StringBuilder();
			BufferedReader reader;
			try {
				body.in().reset();
				reader = new BufferedReader(new InputStreamReader(body.in(), "UTF-8"));
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line).append('\n');
				}
			} catch (IOException e) {
				throw new ConversionException(e);
			}

			String content = sb.toString().trim();
			if (CLASS_NAME.equals(type.toString())) {
				String errorMessage = extractErrorMessage(content);
				if (!TextUtils.isEmpty(errorMessage)) {
					PlayPostResponse response = new PlayPostResponse(errorMessage);
					return response;
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
		} catch (ConversionException e) {
			throw new ConversionException(e);
		}
	}

	private String extractErrorMessage(String content) {
		if (content.contains(ERROR_DIV)) {
			int index = content.indexOf(ERROR_DIV) + ERROR_DIV.length();
			String message = content.substring(index);
			index = message.indexOf("<");
			if (index > 0) {
				message = message.substring(0, index);
			}
			return message.trim();
		}
		return null;
	}

	@Override
	public TypedOutput toBody(Object object) {
		throw new UnsupportedOperationException();
	}
}
