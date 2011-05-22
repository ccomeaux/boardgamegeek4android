package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;

import com.boardgamegeek.provider.BggContract;

public abstract class RemoteBggHandler extends XmlHandler {

	protected XmlPullParser mParser;
	private boolean mIsBggDown;

	public RemoteBggHandler() {
		super(BggContract.CONTENT_AUTHORITY);
	}

	public boolean isBggDown() {
		return mIsBggDown;
	}

	public abstract int getCount();

	protected abstract void clearResults();

	protected abstract String getRootNodeName();

	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
			throws XmlPullParserException, IOException {

		mParser = parser;
		clearResults();

		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG) {
				String name = mParser.getName();
				if (getRootNodeName().equals(name)) {
					parseItems();
				} else if (Tags.ANCHOR.equals(name)) {
					// This method is currently broken since the meta element is
					// unclosed
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
	}
}