package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import com.boardgamegeek.util.StringUtils;

public class RemoteSearchHandler extends RemoteBggHandler {
	// private static final String TAG = "RemoteSearchHandler";

	List<SearchResult> mSearchResults = new ArrayList<SearchResult>();

	public List<SearchResult> getResults() {
		return mSearchResults;
	}

	@Override
	public int getCount() {
		return mSearchResults.size();
	}

	@Override
	protected void clearResults() {
		mSearchResults.clear();
	}
	
	@Override
	protected String getRootNodeName() {
		return "boardgames";
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.BOARDGAME.equals(mParser.getName())) {

				int id = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.OBJECT_ID));

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
					sr.YearPublished = StringUtils.parseInt(text);
				}
			}
		}

		return sr;
	}

	interface Tags {
		String BOARDGAMES = "boardgames";
		String BOARDGAME = "boardgame";
		String OBJECT_ID = "objectid";
		String NAME = "name";
		String PRIMARY = "primary";
		String TRUE = "true";
		String YEAR_PUBLISHED = "yearpublished";
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
