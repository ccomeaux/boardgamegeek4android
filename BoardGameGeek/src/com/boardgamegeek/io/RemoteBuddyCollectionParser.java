package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import com.boardgamegeek.model.BuddyGame;
import com.boardgamegeek.util.StringUtils;

public class RemoteBuddyCollectionParser extends RemoteBggHandler {

	private List<BuddyGame> mBuddyGames = new ArrayList<BuddyGame>();

	public List<BuddyGame> getResults() {
		return mBuddyGames;
	}

	@Override
	public int getCount() {
		return mBuddyGames.size();
	}

	@Override
	protected void clearResults() {
		mBuddyGames.clear();
	}

	@Override
	protected String getRootNodeName() {
		return Tags.ITEMS;
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {
		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.ITEM.equals(mParser.getName())) {
				mBuddyGames.add(parseGame());
			}
		}
	}

	protected BuddyGame parseGame() throws XmlPullParserException, IOException {
		BuddyGame game = new BuddyGame();
		game.Id = mParser.getAttributeValue(null, Tags.OBJECTID);
		int sortIndex = 1;
		String tag = null;
		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG) {
				tag = mParser.getName();
				if (Tags.NAME.equals(tag)) {
					sortIndex = parseIntegerAttribute(Tags.SORT_INDEX, 1);
				}
			} else if (type == END_TAG) {
				tag = null;
				sortIndex = 1;
			}
			if (type == TEXT) {
				String text = mParser.getText();
				if (Tags.NAME.equals(tag)) {
					game.Name = text;
					game.SortName = StringUtils.createSortName(text, sortIndex);
				}
				if (Tags.YEAR.equals(tag)) {
					game.Year = text;
				}
			}
		}
		return game;
	}

	private interface Tags {
		String ITEMS = "items";
		String ITEM = "item";
		String OBJECTID = "objectid";
		String NAME = "name";
		String YEAR = "yearpublished";
		String SORT_INDEX = "sortindex";
	}
}
