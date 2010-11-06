package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.Utility;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Buddies;

public class RemoteBuddiesHandler extends XmlHandler {
	// private static final String TAG = "RemoteBuddiesHandler";

	private static final int PAGE_SIZE = 100;

	public RemoteBuddiesHandler() {
		super(BggContract.CONTENT_AUTHORITY);
	}

	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
		throws XmlPullParserException, IOException {

		int buddyCount = 0;
		int page = 0;

		int type;
		while ((type = parser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.BUDDIES.equals(parser.getName())) {
				buddyCount = Utility.parseInt(parser.getAttributeValue(null, Tags.TOTAL));
				page = Utility.parseInt(parser.getAttributeValue(null, Tags.PAGE));

				parseBuddies(parser, resolver);
			}
		}

		return buddyCount > (page * PAGE_SIZE);
	}

	private void parseBuddies(XmlPullParser parser, ContentResolver resolver) throws XmlPullParserException,
		IOException {

		final int depth = parser.getDepth();
		ContentValues values = new ContentValues();
		
		Uri uri;
		Cursor cursor;

		int type;
		while (((type = parser.next()) != END_TAG || parser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.BUDDY.equals(parser.getName())) {

				int id = Utility.parseInt(parser.getAttributeValue(null, Tags.ID));

				if (id > 0) {

					values.clear();

					uri = Buddies.buildBuddyUri(id);
					cursor = resolver.query(uri, BuddiesQuery.PROJECTION, null, null, null);
					if (cursor.moveToFirst()) {
						values.put(Buddies.UPDATED, System.currentTimeMillis());
						resolver.update(uri, values, null, null);
					} else {
						values.put(Buddies.BUDDY_ID, id);
						values.put(Buddies.BUDDY_NAME, parser.getAttributeValue(null, Tags.NAME));
						values.put(Buddies.UPDATED, System.currentTimeMillis());

						resolver.insert(Buddies.CONTENT_URI, values);
					}
					cursor.close();
				}
			}
		}
	}

	private interface Tags {
		String BUDDIES = "buddies";
		String TOTAL = "total";
		String PAGE = "page";
		String BUDDY = "buddy";
		String ID = "id";
		String NAME = "name";
	}

	private interface BuddiesQuery {
		String[] PROJECTION = { BaseColumns._ID, };

		//int _ID = 0;
	}
}
