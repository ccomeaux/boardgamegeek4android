package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;

import com.boardgamegeek.Utility;
import com.boardgamegeek.provider.BggContract;

public class RemoteSearchHandler extends XmlHandler {
	// private static final String TAG = "RemoteSearchHandler";

	private XmlPullParser mParser;
	private List<SearchResult> mSearchResults = new ArrayList<SearchResult>();
	private boolean mIsBggDown;

	public int getCount() {
		return mSearchResults.size();
	}

	public List<SearchResult> getResults() {
		return mSearchResults;
	}

	public boolean isBggDown() {
		return mIsBggDown;
	}

	public RemoteSearchHandler() {
		super(BggContract.CONTENT_AUTHORITY);
	}

	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
			throws XmlPullParserException, IOException {

		mParser = parser;
		mSearchResults.clear();

		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG) {
				String name = mParser.getName();
				if (Tags.BOARDGAMES.equals(name)) {
					parseItems();
				} else if (Tags.ANCHOR.equals(name)) {
					// This method is currently broken since the meta element is
					// unclosed
					String href = mParser.getAttributeValue(null, Tags.HREF);
					if (Tags.DOWN_LINK.equals(href)) {
						mSearchResults.clear();
						mIsBggDown = true;
						break;
					}
				} else if (Tags.HTML.equals(name)) {
					mSearchResults.clear();
					mIsBggDown = true;
					break;
				}
			}
		}

		return false;
	}

	private void parseItems() throws XmlPullParserException, IOException {

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.BOARDGAME.equals(mParser.getName())) {

				int id = Utility.parseInt(mParser.getAttributeValue(null, Tags.OBJECT_ID));

				SearchResult result = parseItem();
				result.Id = id;
				mSearchResults.add(result);
			}
		}
	}

	private SearchResult parseItem() throws XmlPullParserException, IOException {

		String tag = null;
		SearchResult sr = new SearchResult();

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {

			if (type == START_TAG) {
				tag = mParser.getName();
				if (Tags.NAME.equals(tag)) {
					sr.IsNamePrimary = Tags.TRUE.equals(mParser.getAttributeValue(null, Tags.PRIMARY));
				}
			} else if (type == END_TAG) {
				tag = null;
			} else if (type == TEXT) {
				String text = mParser.getText();
				if (Tags.NAME.equals(tag)) {
					sr.Name = text;
				} else if (Tags.YEAR_PUBLISHED.equals(tag)) {
					sr.YearPublished = Utility.parseInt(text);
				}
			}
		}

		return sr;
	}

	private interface Tags {
		String BOARDGAMES = "boardgames";
		String BOARDGAME = "boardgame";
		String OBJECT_ID = "objectid";
		String NAME = "name";
		String PRIMARY = "primary";
		String TRUE = "true";
		String YEAR_PUBLISHED = "yearpublished";
		String ANCHOR = "a";
		String HREF = "href";
		String DOWN_LINK = "http://groups.google.com/group/bgg_down";
		String HTML = "html";
	}

	// Example:
	// <boardgames>
	// <boardgame objectid="31260">
	// <name primary="true">Agricola</name>
	// <yearpublished>2007</yearpublished>
	// </boardgame>
	// <boardgame objectid="59158">
	// <name primary="true">Agricola CZ-Deck</name>
	// <yearpublished>2009</yearpublished>
	// </boardgame>
	// </boardgames>
}
