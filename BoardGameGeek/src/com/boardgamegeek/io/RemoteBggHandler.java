package com.boardgamegeek.io;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.StringUtils;

public abstract class RemoteBggHandler {
	private static final String TAG = makeLogTag(RemoteBggHandler.class);
	private static final DateFormat FORMATER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

	private final String mAuthority;
	private Context mContext;
	protected XmlPullParser mParser;
	protected ContentResolver mResolver;
	protected ArrayList<ContentProviderOperation> mBatch;
	private String mErrorMessage;
	private int mCount;
	private int mPageNumber;
	private boolean mDebug = false;
	private ArrayList<Insert> mInserts;

	protected Context getContext() {
		return mContext;
	}

	public RemoteBggHandler() {
		mAuthority = BggContract.CONTENT_AUTHORITY;
	}

	public boolean hasError() {
		return !TextUtils.isEmpty(mErrorMessage);
	}

	public String getErrorMessage() {
		return mErrorMessage;
	}

	public void setErrorMessage(String message) {
		mErrorMessage = message;
	}

	public int getCount() {
		return mCount;
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

	public boolean parseAndHandle(XmlPullParser parser, Context context) throws IOException, XmlPullParserException {
		mContext = context;
		if (mContext != null) {
			mDebug = PreferencesUtils.getDebugInserts(mContext);
		}
		return parse(parser, mContext == null ? null : mContext.getContentResolver(), mAuthority);
	}

	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
		throws XmlPullParserException, IOException {

		mErrorMessage = "";
		mParser = parser;
		mResolver = resolver;
		mBatch = new ArrayList<ContentProviderOperation>();
		mInserts = new ArrayList<Insert>();

		clearResults();

		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG) {
				String name = mParser.getName();
				if (getRootNodeName().equals(name)) {
					mCount = StringUtils.parseInt(parser.getAttributeValue(null, getTotalCountAttributeName()));
					mPageNumber = StringUtils.parseInt(parser.getAttributeValue(null, getPageNumberAttributeName()));
					if (mCount > 0) {
						LOGI(TAG, "Expecting " + mCount + " items");
					}
					if (mPageNumber > 0) {
						LOGI(TAG, "Handling page " + mPageNumber);
					}
					parseItems();
				} else if (Tags.ANCHOR.equals(name)) {
					String href = mParser.getAttributeValue(null, Tags.HREF);
					if (Tags.DOWN_LINK.equals(href)) {
						setBggDown();
					}
				} else if (Tags.HTML.equals(name)) {
					setBggDown();
				}
			}
		}
		processBatch();
		return mCount > (mPageNumber * getPageSize());
	}

	public void setBggDown() throws IOException {
		clearResults();
		String message = "";
		if (mContext != null) {
			message = mContext.getString(R.string.bgg_down);
		}
		throw new IOException(message);
	}

	protected abstract void parseItems() throws XmlPullParserException, IOException;

	protected String parseStringAttribute(String tag) {
		String s = mParser.getAttributeValue(null, tag);
		if (s != null) {
			s = s.trim();
		}
		return s;
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

	public long parseDateAttribute(String tag) {
		String dateText = parseStringAttribute(tag);
		try {
			final Date date = FORMATER.parse(dateText);
			return date.getTime();
		} catch (ParseException e) {
			LOGE(TAG, "Couldn't parse date", e);
			return 0;
		}
	}

	protected boolean parseBooleanAttribute(String tag) {
		return "1".equals(parseStringAttribute(tag));
	}

	protected int parseBooleanAttributeAsInteger(String tag) {
		return parseBooleanAttribute(tag) ? 1 : 0;
	}

	protected void addDelete(Uri uri) {
		mBatch.add(ContentProviderOperation.newDelete(uri).build());
	}

	protected void addUpdate(Uri uri, ContentValues values) {
		mBatch.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
	}

	protected void addUpdate(Uri uri, ContentValues values, boolean yieldAllowed) {
		mBatch.add(ContentProviderOperation.newUpdate(uri).withValues(values).withYieldAllowed(yieldAllowed).build());
	}

	protected void addUpdateToTop(Uri uri, ContentValues values) {
		mBatch.add(0, ContentProviderOperation.newUpdate(uri).withValues(values).withYieldAllowed(true).build());
	}

	protected void addInsertToTop(Uri uri, ContentValues values) {
		if (mDebug) {
			mInserts.add(0, new Insert(uri, values));
		} else {
			mBatch.add(0, ContentProviderOperation.newInsert(uri).withValues(values).withYieldAllowed(true).build());
		}
	}

	protected void addInsert(Uri uri, ContentValues values) {
		if (mDebug) {
			mInserts.add(new Insert(uri, values));
		} else {
			mBatch.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
		}
	}

	protected void addInsert(Uri uri, ContentValues values, boolean yieldAllowed) {
		if (mDebug) {
			mInserts.add(new Insert(uri, values));
		} else {
			mBatch.add(ContentProviderOperation.newInsert(uri).withValues(values).withYieldAllowed(yieldAllowed)
				.build());
		}
	}

	protected void addInsert(Uri uri, String key, Object value) {
		if (mDebug) {
			mInserts.add(new Insert(uri, key, value));
		} else {
			mBatch.add(ContentProviderOperation.newInsert(uri).withValue(key, value).build());
		}
	}

	protected void processBatch() {
		if (mDebug) {
			Uri uri;
			for (Insert insert : mInserts) {
				uri = mResolver.insert(insert.uri, insert.values);
				if (uri == null) {
					throw new RuntimeException("Insert failed for uri: [" + insert.uri + "], values: ["
						+ insert.values.toString() + "]");
				}
			}
			mInserts.clear();
		}
		ResolverUtils.applyBatch(mResolver, mBatch);
		mBatch.clear();
	}

	interface Tags {
		String ANCHOR = "a";
		String HREF = "href";
		String DOWN_LINK = "http://groups.google.com/group/bgg_down";
		String HTML = "html";
		String TOTAL_ITEMS = "totalitems";
		String PAGE = "page";
	}

	class Insert {
		Insert(Uri uri, ContentValues values) {
			this.uri = uri;
			this.values = new ContentValues(values);
		}

		Insert(Uri uri, String key, Object value) {
			this.uri = uri;
			values = new ContentValues(1);
			if (value == null) {
				values.putNull(key);
			} else if (value instanceof String) {
				values.put(key, (String) value);
			} else if (value instanceof Byte) {
				values.put(key, (Byte) value);
			} else if (value instanceof Short) {
				values.put(key, (Short) value);
			} else if (value instanceof Integer) {
				values.put(key, (Integer) value);
			} else if (value instanceof Long) {
				values.put(key, (Long) value);
			} else if (value instanceof Float) {
				values.put(key, (Float) value);
			} else if (value instanceof Double) {
				values.put(key, (Double) value);
			} else if (value instanceof Boolean) {
				values.put(key, (Boolean) value);
			} else if (value instanceof byte[]) {
				values.put(key, (byte[]) value);
			} else {
				throw new IllegalArgumentException("bad value type: " + value.getClass().getName());
			}
		}

		Uri uri;
		ContentValues values;
	}
}