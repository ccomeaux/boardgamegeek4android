package com.boardgamegeek.io;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.database.ResolverUtils;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.StringUtils;

public abstract class RemoteBggHandler extends XmlHandler {
	private static final String TAG = makeLogTag(RemoteBggHandler.class);

	protected XmlPullParser mParser;
	protected ContentResolver mResolver;
	protected ArrayList<ContentProviderOperation> mBatch;
	private boolean mIsBggDown;
	private String mErrorMessage;
	private int mTotalCount;
	private int mPageNumber;

	public RemoteBggHandler() {
		super(BggContract.CONTENT_AUTHORITY);
	}

	public boolean isBggDown() {
		return mIsBggDown;
	}

	public boolean hasError() {
		return !TextUtils.isEmpty(mErrorMessage);
	}

	public String getErrorMessage() {
		if (mIsBggDown) {
			return getContext().getString(R.string.bgg_down);
		}
		return mErrorMessage;
	}

	public void setErrorMessage(String message) {
		mErrorMessage = message;
	}

	public abstract int getCount();

	public int getTotalCount() {
		return mTotalCount;
	}

	protected void clearResults() {
	}

	protected abstract String getRootNodeName();

	protected String getTotalCountAttributeName() {
		return Tags.TOTAL_ITEMS;
	}

	protected String getPageNumberAttributeName() {
		return Tags.PAGE;
	}

	protected int getPageSize() {
		return 100;
	}

	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
		throws XmlPullParserException, IOException {

		mErrorMessage = "";
		mIsBggDown = false;
		mParser = parser;
		mResolver = resolver;
		mBatch = new ArrayList<ContentProviderOperation>();

		clearResults();

		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG) {
				String name = mParser.getName();
				if (getRootNodeName().equals(name)) {
					mTotalCount = StringUtils.parseInt(parser.getAttributeValue(null, getTotalCountAttributeName()));
					mPageNumber = StringUtils.parseInt(parser.getAttributeValue(null, getPageNumberAttributeName()));
					if (mTotalCount > 0) {
						LOGI(TAG, "Expecting " + mTotalCount + " items");
					}
					if (mPageNumber > 0) {
						LOGI(TAG, "Handling page " + mPageNumber);
					}
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
		ResolverUtils.applyBatch(mResolver, mBatch);
		return mTotalCount > (mPageNumber * getPageSize());
	}

	protected abstract void parseItems() throws XmlPullParserException, IOException;

	protected String parseStringAttribute(String tag) {
		return mParser.getAttributeValue(null, tag);
	}

	protected double parseDoubleAttribute(String tag) {
		return StringUtils.parseDouble(parseStringAttribute(tag));
	}

	protected int parseIntegerAttribute(String tag) {
		return StringUtils.parseInt(parseStringAttribute(tag));
	}

	protected int parseIntegerAttribute(String tag, int defaultValue) {
		return StringUtils.parseInt(parseStringAttribute(tag), defaultValue);
	}

	protected void addDelete(Uri uri) {
		mBatch.add(ContentProviderOperation.newDelete(uri).build());
	}

	protected void addUpdate(Uri uri, ContentValues values) {
		mBatch.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
	}

	protected void addInsert(Uri uri, ContentValues values) {
		mBatch.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
	}

	protected void addInsert(Uri uri, String key, Object value) {
		mBatch.add(ContentProviderOperation.newInsert(uri).withValue(key, value).build());
	}

	interface Tags {
		String ANCHOR = "a";
		String HREF = "href";
		String DOWN_LINK = "http://groups.google.com/group/bgg_down";
		String HTML = "html";
		String TOTAL_ITEMS = "totalitems";
		String PAGE = "page";
	}
}