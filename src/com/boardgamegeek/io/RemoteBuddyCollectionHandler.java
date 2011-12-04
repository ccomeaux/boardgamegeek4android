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

import com.boardgamegeek.model.BuddyGame;

public class RemoteBuddyCollectionHandler extends RemoteBggHandler {

	private XmlPullParser mParser;
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
		return "items";
	}

	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
			throws XmlPullParserException, IOException {
		mParser = parser;

		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.ITEMS.equals(mParser.getName())) {
				parseItems();
			}
		}
		
		return false;
	}
	
	@Override
	protected void parseItems() throws XmlPullParserException, IOException {
		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.ITEM.equals(mParser.getName())) {
				final BuddyGame game = new BuddyGame();
				game.Id = mParser.getAttributeValue(null, Tags.OBJECTID);
				parseGame(game);
				mBuddyGames.add(game);
			}
		}
	}
	
	protected void parseGame(final BuddyGame game) throws XmlPullParserException, IOException {
		String tag = null;
		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG) {
				tag = mParser.getName();
			} else if (type == END_TAG) {
				tag = null;
			}
			if (type == TEXT) {
				if (Tags.NAME.equals(tag)) {
					game.Name = mParser.getText();
				}
				if (Tags.YEAR.equals(tag)) {
					game.Year = mParser.getText();
				}
			}
		}
	}
	
	private interface Tags {
		String ITEMS = "items";
		String ITEM = "item";
		String OBJECTID = "objectid";
		String NAME = "name";
		String YEAR = "yearpublished";
	}
}
