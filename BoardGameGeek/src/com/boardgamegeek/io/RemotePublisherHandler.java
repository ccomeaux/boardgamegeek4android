package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Publishers;

public class RemotePublisherHandler extends XmlHandler {
	private static final String TAG = "RemotePublisherHandler";

	private int mPublisherId;

	public RemotePublisherHandler() {
		super(BggContract.CONTENT_AUTHORITY);
	}

	public RemotePublisherHandler(int publisherId) {
		super(BggContract.CONTENT_AUTHORITY);
		mPublisherId = publisherId;
	}

	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
			throws XmlPullParserException, IOException {

		if (mPublisherId == 0) {
			return false;
		}

		String[] projection = { Publishers.PUBLISHER_ID};

		int type;
		while ((type = parser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.COMPANY.equals(parser.getName())) {

				Uri uri = Publishers.buildPublisherUri(mPublisherId);
				Cursor cursor = resolver.query(uri, projection, null, null, null);

				if (!cursor.moveToFirst()) {
					Log.w(TAG, "Tried to parse publisher, but ID not in database: " + mPublisherId);
				} else {
					parsePublisher(parser, resolver, uri);
				}

				cursor.close();
			}
		}

		return false;
	}

	private void parsePublisher(XmlPullParser parser, ContentResolver resolver, Uri uri) throws XmlPullParserException,
			IOException {

		final int depth = parser.getDepth();
		ContentValues values = new ContentValues();
		String tag = null;

		int type;
		while (((type = parser.next()) != END_TAG || parser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG) {
				tag = parser.getName();
			} else if (type == END_TAG) {
				tag = null;
			} else if (type == TEXT) {
				String text = parser.getText();

				if (Tags.NAME.equals(tag)) {
					values.put(Publishers.PUBLISHER_NAME, text);
				} else if (Tags.DESCRIPTION.equals(tag)) {
					values.put(Publishers.PUBLISHER_DESCRIPTION, text);
				}
			}
		}

		values.put(Publishers.UPDATED, System.currentTimeMillis());
		resolver.update(uri, values, null, null);
	}

	private interface Tags {
		// String COMPANIES = "companies";
		String COMPANY = "company";
		String NAME = "name";
		String DESCRIPTION = "description";
	}
}
