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
import com.boardgamegeek.provider.BggContract.Designers;

public class RemoteDesignerHandler extends XmlHandler {
	private static final String TAG = "RemoteDesignerHandler";

	private int mDesignerId;

	public RemoteDesignerHandler() {
		super(BggContract.CONTENT_AUTHORITY);
	}

	public RemoteDesignerHandler(int designerId) {
		super(BggContract.CONTENT_AUTHORITY);
		mDesignerId = designerId;
	}

	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
			throws XmlPullParserException, IOException {

		if (mDesignerId == 0) {
			return false;
		}

		String[] projection = { Designers.DESIGNER_ID };

		int type;
		while ((type = parser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.PERSON.equals(parser.getName())) {

				Uri uri = Designers.buildDesignerUri(mDesignerId);
				Cursor cursor = resolver.query(uri, projection, null, null, null);

				if (!cursor.moveToFirst()) {
					Log.w(TAG, "Tried to parse designer, but ID not in database: " + mDesignerId);
				} else {
					parseDesigner(parser, resolver, uri);
				}

				cursor.close();
			}
		}

		return false;
	}

	private void parseDesigner(XmlPullParser parser, ContentResolver resolver, Uri uri) throws XmlPullParserException,
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
					values.put(Designers.DESIGNER_NAME, text);
				} else if (Tags.DESCRIPTION.equals(tag)) {
					values.put(Designers.DESIGNER_DESCRIPTION, text);
				}
			}
		}

		values.put(Designers.UPDATED, System.currentTimeMillis());
		resolver.update(uri, values, null, null);
	}

	private interface Tags {
		// String PEOPLE = "people";
		String PERSON = "person";
		String NAME = "name";
		String DESCRIPTION = "description";
	}
}
