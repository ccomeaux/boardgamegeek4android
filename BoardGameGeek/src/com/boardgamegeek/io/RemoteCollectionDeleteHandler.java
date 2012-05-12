package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentValues;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.SyncListColumns;
import com.boardgamegeek.util.StringUtils;

public class RemoteCollectionDeleteHandler extends RemoteBggHandler {
	private long mStartTime;
	private int mCount;

	public RemoteCollectionDeleteHandler(long startTime) {
		super();
		mStartTime = startTime;
	}

	@Override
	public int getCount() {
		return mCount;
	}

	@Override
	protected String getRootNodeName() {
		return Tags.ITEMS;
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {

		ContentValues values = new ContentValues();
		values.put(SyncListColumns.UPDATED_LIST, mStartTime);

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.ITEM.equals(mParser.getName())) {

				int collectionId = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.COLLECTION_ID));

				Uri uri = Collection.buildItemUri(collectionId);
				mCount = mResolver.update(uri, values, null, null);
			}
		}
	}

	private interface Tags {
		String ITEMS = "items";
		String ITEM = "item";
		String COLLECTION_ID = "collid";
	}
}
