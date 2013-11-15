package com.boardgamegeek.io;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.util.StringUtils;

public abstract class RemoteBggParser {
	private static final String TAG = makeLogTag(RemoteBggParser.class);

	private Context mContext;
	protected XmlPullParser mParser;
	private String mErrorMessage;
	private int mCount;
	private int mPageNumber;

	public RemoteBggParser() {
	}

	public abstract String getUrl();

	public void setParser(XmlPullParser parser) {
		mParser = parser;
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

	protected Context getContext() {
		return mContext;
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

	public boolean parse(XmlPullParser parser, Context context) throws IOException, XmlPullParserException {
		mParser = parser;
		mContext = context;

		mErrorMessage = "";
		clearResults();

		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG) {
				String name = mParser.getName();
				if (getRootNodeName().equals(name)) {
					mCount = parseIntegerAttribute(getTotalCountAttributeName());
					mPageNumber = parseIntegerAttribute(getPageNumberAttributeName());
					if (mCount > 0) {
						LOGI(TAG, "Expecting " + mCount + " items");
					}
					if (mPageNumber > 0) {
						LOGI(TAG, "Handling page " + mPageNumber);
					}
					parseItems();
				} else if (Tags.ANCHOR.equals(name)) {
					String href = parseStringAttribute(Tags.HREF);
					if (Tags.DOWN_LINK.equals(href)) {
						setBggDown();
					}
				} else if (Tags.HTML.equals(name)) {
					setBggDown();
				}
			}
		}
		return mCount > (mPageNumber * getPageSize());
	}

	private void setBggDown() throws IOException {
		clearResults();
		String message = "BGG is down!";
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

	protected long parseDateAttribute(String tag, String format, boolean includesTimeZone) {
		String dateText = parseStringAttribute(tag);
		if (TextUtils.isEmpty(dateText)) {
			LOGW(TAG, "Missing date");
			return 0;
		}
		if (includesTimeZone) {
			dateText = fixupTimeZone(dateText);
		}
		DateFormat sdf = new SimpleDateFormat(format, Locale.US);
		try {
			final Date date = sdf.parse(dateText);
			return date.getTime();
		} catch (ParseException e) {
			LOGE(TAG, "Couldn't parse date", e);
			return 0;
		}
	}

	private static String fixupTimeZone(String dateText) {
		int index = dateText.lastIndexOf("-");

		if (index > 0) {
			dateText = dateText.substring(0, index).concat("GMT").concat(dateText.substring(index));
		}
		return dateText;
	}

	protected boolean parseBooleanAttribute(String tag) {
		String attribute = parseStringAttribute(tag);
		return "1".equals(attribute) || "true".equals(attribute);
	}

	protected int parseBooleanAttributeAsInteger(String tag) {
		return parseBooleanAttribute(tag) ? 1 : 0;
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