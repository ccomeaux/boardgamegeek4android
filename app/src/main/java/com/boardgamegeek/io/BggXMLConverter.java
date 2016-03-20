package com.boardgamegeek.io;

import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import retrofit.converter.ConversionException;
import retrofit.converter.SimpleXMLConverter;
import retrofit.mime.TypedInput;
import timber.log.Timber;

public class BggXMLConverter extends SimpleXMLConverter {
	private static final String MESSAGE = "Your request for this collection has been accepted and will be processed.  Please try again later for access.";

	public BggXMLConverter() {
		super(false);
	}

	@Override
	public Object fromBody(TypedInput body, Type type) throws ConversionException {
		Object convertedObject = super.fromBody(body, type);
		assertRetry(body);
		return convertedObject;
	}

	private void assertRetry(TypedInput body) throws ConversionException {
		String content = getContent(body);
		if (content.contains(MESSAGE)) {
			throw new ConversionException(new RetryableException(MESSAGE));
		}
	}

	@NonNull
	protected String getContent(TypedInput body) throws ConversionException {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader;
		try {
			try {
				body.in().reset();
			} catch (IOException ex) {
				Timber.w("Error resetting input; ignoring.", ex);
			}

			reader = new BufferedReader(new InputStreamReader(body.in(), "UTF-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
		} catch (IOException e) {
			throw new ConversionException(e);
		}
		return sb.toString().trim();
	}
}
