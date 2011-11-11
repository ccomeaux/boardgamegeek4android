package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.SyncListColumns;
import com.boardgamegeek.util.StringUtils;

public class RemoteCollectionDeleteHandler extends XmlHandler {
	private static final String TAG = "RemoteCollectionDeleteHandler";

	private long mStartTime;
	private XmlPullParser mParser;
	private ContentResolver mResolver;

	public RemoteCollectionDeleteHandler(long startTime) {
		super(BggContract.CONTENT_AUTHORITY);
		mStartTime = startTime;
	}

	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
			throws XmlPullParserException, IOException {

		mParser = parser;
		mResolver = resolver;

		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.ITEMS.equals(mParser.getName())) {

				int itemCount = StringUtils.parseInt(parser.getAttributeValue(null, Tags.TOTAL_ITEMS));
				Log.i(TAG, "Expecting " + itemCount + " items");

				parseItems();
			}
		}

		return false;
	}

	private void parseItems() throws XmlPullParserException, IOException {

		ContentValues values = new ContentValues();
		values.put(SyncListColumns.UPDATED_LIST, mStartTime);

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.ITEM.equals(mParser.getName())) {

				int collectionId = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.COLLECTION_ID));

				Uri uri = Collection.buildItemUri(collectionId);
				mResolver.update(uri, values, null, null);
			}
		}
	}

	private interface Tags {
		String ITEMS = "items";
		String TOTAL_ITEMS = "totalitems";
		String ITEM = "item";
		String COLLECTION_ID = "collid";
	}
}
