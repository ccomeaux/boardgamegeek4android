package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.boardgamegeek.provider.BggContract.Publishers;

public class RemotePublisherHandler extends RemoteBggHandler {
	private static final String TAG = "RemotePublisherHandler";

	private int mPublisherId;
	private int mCount;

	public RemotePublisherHandler() {
		super();
	}

	public RemotePublisherHandler(int publisherId) {
		super();
		mPublisherId = publisherId;
	}

	@Override
	public int getCount() {
		return mCount;
	}

	@Override
	protected String getRootNodeName() {
		return Tags.COMPANIES;
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {
		if (mPublisherId == 0) {
			return;
		}
		Uri uri = Publishers.buildPublisherUri(mPublisherId);

		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.COMPANY.equals(mParser.getName())) {

				Cursor cursor = mResolver.query(uri, new String[] { Publishers.PUBLISHER_ID }, null, null, null);
				try {
					if (cursor.getCount() > 0) {
						ContentValues values = parsePublisher(uri);
						mCount = mResolver.update(uri, values, null, null);
					} else {
						Log.w(TAG, "Tried to parse publisher, but ID not in database: " + mPublisherId);
					}
				} finally {
					if (cursor != null) {
						cursor.close();
					}
				}
			}
		}
	}

	private ContentValues parsePublisher(Uri uri) throws XmlPullParserException, IOException {
		final int depth = mParser.getDepth();
		ContentValues values = new ContentValues();
		String tag = null;

		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG) {
				tag = mParser.getName();
			} else if (type == END_TAG) {
				tag = null;
			} else if (type == TEXT) {
				String text = mParser.getText();

				if (Tags.NAME.equals(tag)) {
					values.put(Publishers.PUBLISHER_NAME, text);
				} else if (Tags.DESCRIPTION.equals(tag)) {
					values.put(Publishers.PUBLISHER_DESCRIPTION, text);
				}
			}
		}
		values.put(Publishers.UPDATED, System.currentTimeMillis());
		return values;
	}

	private interface Tags {
		String COMPANIES = "companies";
		String COMPANY = "company";
		String NAME = "name";
		String DESCRIPTION = "description";
	}
}
