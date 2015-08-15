package com.boardgamegeek.io;

import android.text.TextUtils;

import com.boardgamegeek.model.PlayPostResponse;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

public class PlaySaveConverter implements Converter {
	public static final String ERROR_DIV = "<div class='messagebox error'>";
	private final GsonConverter mGsonConverter;

	public PlaySaveConverter() {
		mGsonConverter = new GsonConverter(new Gson());
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
			return mGsonConverter.fromBody(body, type);
		} catch (ConversionException ce) {
			// we didn't get the expected JSON
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
			if ("class com.boardgamegeek.model.PlayPostResponse".equals(type.toString())) {
				String errorMessage = extractErrorMessage(content);
				if (!TextUtils.isEmpty(errorMessage)) {
					PlayPostResponse response = new PlayPostResponse(errorMessage);
					return response;
				}
			}
			throw new ConversionException(content, ce);
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
