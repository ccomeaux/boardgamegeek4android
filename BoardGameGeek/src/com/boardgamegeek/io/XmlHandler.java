package com.boardgamegeek.io;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.Context;

public abstract class XmlHandler {
	private final String mAuthority;
	private Context mContext;

	protected Context getContext() {
		return mContext;
	}

	public XmlHandler(String authority) {
		mAuthority = authority;
	}

	public boolean parseAndHandle(XmlPullParser parser, Context context) throws HandlerException {
		mContext = context;
		try {
			return parse(parser, mContext == null ? null : mContext.getContentResolver(), mAuthority);
		} catch (XmlPullParserException e) {
			throw new HandlerException("Problem parsing XML response", e);
		} catch (IOException e) {
			throw new HandlerException("Problem reading response", e);
		}
	}

	public abstract boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
		throws XmlPullParserException, IOException;

	public static class HandlerException extends IOException {
		// I don't know what this is, but Eclipse likes it
		private static final long serialVersionUID = 2924984447559937039L;

		public HandlerException(String message) {
			super(message);
		}

		public HandlerException(String message, Throwable cause) {
			super(message);
			initCause(cause);
		}

		@Override
		public String toString() {
			if (getCause() != null) {
				return getLocalizedMessage() + ": " + getCause();
			} else {
				return getLocalizedMessage();
			}
		}
	}
}
