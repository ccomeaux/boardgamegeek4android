package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.util.Log;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.StringUtils;

public abstract class RemoteBggHandler extends XmlHandler {
	private static final String TAG = "RemoteBggHandler";

	protected XmlPullParser mParser;
	protected ContentResolver mResolver;
	private boolean mIsBggDown;
	private int mTotalCount;

	public RemoteBggHandler() {
		super(BggContract.CONTENT_AUTHORITY);
	}

	public boolean isBggDown() {
		return mIsBggDown;
	}

	public abstract int getCount();

	public int getTotalCount() {
		return mTotalCount;
	}

	protected abstract void clearResults();

	protected abstract String getRootNodeName();

	protected String getTotalCountAttributeName() {
		return Tags.TOTAL_ITEMS;
	}

	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
			throws XmlPullParserException, IOException {

		mParser = parser;
		mResolver = resolver;

		clearResults();

		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG) {
				String name = mParser.getName();
				if (getRootNodeName().equals(name)) {
					mTotalCount = StringUtils.parseInt(parser.getAttributeValue(null, getTotalCountAttributeName()));
					Log.i(TAG, "Expecting " + mTotalCount + " items");
					parseItems();
				} else if (Tags.ANCHOR.equals(name)) {
					String href = mParser.getAttributeValue(null, Tags.HREF);
					if (Tags.DOWN_LINK.equals(href)) {
						clearResults();
						mIsBggDown = true;
						break;
					}
				} else if (Tags.HTML.equals(name)) {
					clearResults();
					mIsBggDown = true;
					break;
				}
			}
		}

		return false;
	}

	protected abstract void parseItems() throws XmlPullParserException, IOException;

	interface Tags {
		String ANCHOR = "a";
		String HREF = "href";
		String DOWN_LINK = "http://groups.google.com/group/bgg_down";
		String HTML = "html";
		String TOTAL_ITEMS = "totalitems";
	}
}