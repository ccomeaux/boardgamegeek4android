package com.boardgamegeek.io;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public abstract class RemoteProducerHandler extends RemoteBggHandler {
	private static final String TAG = makeLogTag(RemoteProducerHandler.class);

	protected int mProducerId;
	private int mCount;

	RemoteProducerHandler(int producerId) {
		super();
		mProducerId = producerId;
	}

	@Override
	public int getCount() {
		return mCount;
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {
		if (mProducerId == 0) {
			return;
		}
		Uri uri = createUri();
		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG && ItemTag().equals(mParser.getName())) {
				Cursor cursor = mResolver.query(uri, new String[] { idColumn() }, null, null, null);
				try {
					if (cursor.getCount() > 0) {
						mCount++;
						mBatch.add(ContentProviderOperation.newUpdate(uri).withValues(parseProducer()).build());
					} else {
						LOGW(TAG, "Tried to parse " + type() + ", but ID not in database: " + mProducerId);
					}
				} finally {
					if (cursor != null && !cursor.isClosed()) {
						cursor.close();
					}
				}
			}
		}
	}

	private ContentValues parseProducer() throws XmlPullParserException, IOException {
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
				if (nameTag().equals(tag)) {
					values.put(nameColumn(), text);
				} else if (descriptionTag().equals(tag)) {
					values.put(descriptionColumn(), text);
				}
			}
		}
		values.put(updatedColumn(), System.currentTimeMillis());
		return values;
	}

	protected abstract String type();

	protected abstract Uri createUri();

	protected abstract String ItemTag();

	protected abstract String nameTag();

	protected abstract String descriptionTag();

	protected abstract String idColumn();

	protected abstract String nameColumn();

	protected abstract String descriptionColumn();

	protected abstract String updatedColumn();
}
