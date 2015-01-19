package com.boardgamegeek.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

import com.google.gson.Gson;

public class JsonConverter implements Converter {
	private GsonConverter mGsonConverter;

	public JsonConverter() {
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
			return  mGsonConverter.fromBody(body, type);
		} catch (ConversionException ce) {
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
			String message = sb.toString().trim();
			if ("You must login to save plays".equals(message)
				|| message.contains("You are not permitted to edit this play.")) {
				throw new ConversionException(new AuthException(message));
			} else if (message.contains("That play doesn't exist") || message.contains("Play does not exist.")) {
				throw new ConversionException(new InvalidIdException(message));
			} else if (message.contains("<title>Plays") && message.contains("User:")) {
				throw new ConversionException(new PossibleSuccessException(message));
			}
			throw new ConversionException(message, ce);
		}
	}

	@Override
	public TypedOutput toBody(Object object) {
		return mGsonConverter.toBody(object);
	}
}
