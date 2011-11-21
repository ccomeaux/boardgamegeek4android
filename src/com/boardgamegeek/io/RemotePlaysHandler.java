package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.StringUtils;

public class RemotePlaysHandler extends XmlHandler {
	private static final String TAG = "RemotePlaysHandler";

	private static final int PAGE_SIZE = 100;

	private XmlPullParser mParser;

	public RemotePlaysHandler() {
		super(BggContract.CONTENT_AUTHORITY);
	}

	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
			throws XmlPullParserException, IOException {

		mParser = parser;

		int playCount = 0;
		int page = 0;

		int type;
		while ((type = parser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.PLAYS.equals(parser.getName())) {
				playCount = StringUtils.parseInt(parser.getAttributeValue(null, Tags.TOTAL));
				page = StringUtils.parseInt(parser.getAttributeValue(null, Tags.PAGE));

				parsePlays(resolver);
			}
		}

		return playCount > (page * PAGE_SIZE);
	}

	private void parsePlays(ContentResolver resolver) throws XmlPullParserException, IOException {

		String[] projection = { BaseColumns._ID, };
		final int depth = mParser.getDepth();
		ContentValues values = new ContentValues();

		int updateCount = 0;
		int insertCount = 0;

		Cursor cursor = null;
		try {
			int id = 0;
			int type;
			while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
				if (type == START_TAG) {
					String tag = mParser.getName();

					if (Tags.PLAY.equals(tag)) {
						id = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.ID));

						if (id > 0) {
							values.clear();
							values.put(Plays.DATE, mParser.getAttributeValue(null, Tags.DATE));
							values.put(Plays.QUANTITY, Integer.valueOf(mParser.getAttributeValue(null, Tags.QUANTITY)));
							values.put(Plays.LENGTH, Integer.valueOf(mParser.getAttributeValue(null, Tags.LENGTH)));
							values.put(Plays.INCOMPLETE, !"0".equals(mParser.getAttributeValue(null, Tags.INCOMPLETE)));
							values.put(Plays.NO_WIN_STATS,
									!"0".equals(mParser.getAttributeValue(null, Tags.NO_WIN_STATS)));
							values.put(Plays.LOCATION, mParser.getAttributeValue(null, Tags.LOCATION));
							values.put(Plays.UPDATED_LIST, System.currentTimeMillis());

							Uri uri = Plays.buildPlayUri(id);
							cursor = resolver.query(uri, projection, null, null, null);
							if (cursor.moveToFirst()) {
								resolver.delete(Plays.buildItemUri(id), null, null);
								resolver.delete(Plays.buildPlayerUri(id), null, null);
								resolver.update(uri, values, null, null);
								updateCount++;
							} else {
								values.put(Plays.PLAY_ID, id);
								resolver.insert(Plays.CONTENT_URI, values);
								insertCount++;
							}
							cursor.deactivate();
						}
					} else if (Tags.ITEM.equals(tag)) {
						values.clear();
						values.put(PlayItems.OBJECT_ID,
								Integer.valueOf(mParser.getAttributeValue(null, Tags.OBJECT_ID)));
						values.put(PlayItems.NAME, mParser.getAttributeValue(null, Tags.NAME));
						resolver.insert(Plays.buildItemUri(id), values);
					} else if (Tags.PLAYER.equals(tag)) {
						// TODO: delete all users with ID=0 or null, update
						// users with matching IDs, insert others, delete
						// missing

						values.clear();
						values.put(PlayPlayers.USER_NAME, mParser.getAttributeValue(null, Tags.USERNAME));
						values.put(PlayPlayers.USER_ID, Integer.valueOf(mParser.getAttributeValue(null, Tags.USERID)));
						values.put(PlayPlayers.NAME, mParser.getAttributeValue(null, Tags.NAME));
						values.put(PlayPlayers.START_POSITION, mParser.getAttributeValue(null, Tags.STARTPOSITION));
						values.put(PlayPlayers.COLOR, mParser.getAttributeValue(null, Tags.COLOR));
						values.put(PlayPlayers.SCORE, mParser.getAttributeValue(null, Tags.SCORE));
						values.put(PlayPlayers.NEW, Integer.valueOf(mParser.getAttributeValue(null, Tags.NEW)));
						values.put(PlayPlayers.RATING, Double.valueOf(mParser.getAttributeValue(null, Tags.RATING)));
						values.put(PlayPlayers.WIN, Integer.valueOf(mParser.getAttributeValue(null, Tags.WIN)));
						resolver.insert(Plays.buildPlayerUri(id), values);
					}
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
			Log.i(TAG, "Updated " + updateCount + ", inserted " + insertCount + " plays");
		}
	}

	private interface Tags {
		String PLAYS = "plays";
		String TOTAL = "total";
		String PAGE = "page";
		String PLAY = "play";
		String ID = "id";
		String DATE = "date";
		String QUANTITY = "quantity";
		String LENGTH = "length";
		String INCOMPLETE = "incomplete";
		String NO_WIN_STATS = "nowinstats";
		String LOCATION = "location";

		String ITEM = "item";
		String NAME = "name";
		String OBJECT_ID = "objectid";

		String PLAYER = "player";
		String USERNAME = "username";
		String USERID = "userid";
		String STARTPOSITION = "startposition";
		String COLOR = "color";
		String SCORE = "score";
		String NEW = "new";
		String RATING = "rating";
		String WIN = "win";
	}
}
