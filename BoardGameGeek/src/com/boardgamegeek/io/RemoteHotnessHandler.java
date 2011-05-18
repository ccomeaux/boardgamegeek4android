package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.StringUtils;

public class RemoteHotnessHandler extends XmlHandler {
	// private static final String TAG = "RemoteHotnessHandler";

	private XmlPullParser mParser;
	private List<HotGame> mHotGames = new ArrayList<HotGame>();
	private boolean mIsBggDown;

	public int getCount() {
		return mHotGames.size();
	}

	public List<HotGame> getResults() {
		return mHotGames;
	}

	public boolean isBggDown() {
		return mIsBggDown;
	}

	public RemoteHotnessHandler() {
		super(BggContract.CONTENT_AUTHORITY);
	}

	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
			throws XmlPullParserException, IOException {

		mParser = parser;
		mHotGames.clear();

		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG) {
				String name = mParser.getName();
				if (Tags.ITEMS.equals(name)) {
					parseItems();
				} else if (Tags.ANCHOR.equals(name)) {
					// This method is currently broken since the meta element is
					// unclosed
					String href = mParser.getAttributeValue(null, Tags.HREF);
					if (Tags.DOWN_LINK.equals(href)) {
						mHotGames.clear();
						mIsBggDown = true;
						break;
					}
				} else if (Tags.HTML.equals(name)) {
					mHotGames.clear();
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
			if (type == START_TAG && Tags.ITEM.equals(mParser.getName())) {

				int id = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.ID));
				int rank = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.RANK));

				HotGame game = parseItem();
				game.Id = id;
				game.Rank = rank;
				mHotGames.add(game);
			}
		}
	}

	private HotGame parseItem() throws XmlPullParserException, IOException {

		String tag = null;
		HotGame game = new HotGame();

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {

			if (type == START_TAG) {
				tag = mParser.getName();
				if (Tags.THUMBNAIL.equals(tag)) {
					game.ThumbnailUrl = mParser.getAttributeValue(null, Tags.VALUE);
				} else if (Tags.NAME.equals(tag)) {
					game.Name = mParser.getAttributeValue(null, Tags.VALUE);
				} else if (Tags.YEAR_PUBLISHED.equals(tag))
					game.YearPublished = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.VALUE));
			} else if (type == END_TAG) {
				tag = null;
			}
		}

		return game;
	}

	private interface Tags {
		String ITEMS = "items";
		String ITEM = "item";
		String ID = "id";
		String NAME = "name";
		String RANK = "rank";
		String THUMBNAIL = "thumbnail";
		String VALUE = "value";
		String YEAR_PUBLISHED = "yearpublished";

		String ANCHOR = "a";
		String HREF = "href";
		String DOWN_LINK = "http://groups.google.com/group/bgg_down";
		String HTML = "html";
	}

	// Example from XMLAPI2:
	// <items termsofuse="http://boardgamegeek.com/xmlapi/termsofuse">
	// <item id="98351" rank="1">
	// <thumbnail value="http://cf.geekdo-images.com/images/pic988097_t.jpg"/>
	// <name value="Core Worlds"/>
	// <yearpublished value="2011"/>
	// </item>
	// </items>
}
