/**
 * From: https://github.com/square/retrofit/tree/master/retrofit-converters/simplexml/src/main/java/retrofit/converter
 */
package com.boardgamegeek.io.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

public class SimpleXMLConverter implements Converter {
	private static final String CHARSET = "UTF-8";
	private static final String MIME_TYPE = "application/xml; charset=" + CHARSET;

	private final Serializer serializer;

	public SimpleXMLConverter() {
		this(new Persister());
	}

	public SimpleXMLConverter(Serializer serializer) {
		this.serializer = serializer;
	}

	@Override
	public Object fromBody(TypedInput body, Type type) throws ConversionException {
		try {
			return serializer.read((Class<?>) type, body.in());
		} catch (Exception e) {
			throw new ConversionException(e);
		}
	}

	@Override
	public TypedOutput toBody(Object source) {
		OutputStreamWriter osw = null;

		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			osw = new OutputStreamWriter(bos, CHARSET);
			serializer.write(source, osw);
			osw.flush();
			return new TypedByteArray(MIME_TYPE, bos.toByteArray());
		} catch (Exception e) {
			throw new AssertionError(e);
		} finally {
			try {
				if (osw != null) {
					osw.close();
				}
			} catch (IOException e) {
				throw new AssertionError(e);
			}
		}
	}
}