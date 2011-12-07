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
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.StringUtils;

public class RemotePlaysHandler extends XmlHandler {
	private static final String TAG = "RemotePlaysHandler";

	private static final int PAGE_SIZE = 100;

	private XmlPullParser mParser;
	private ContentResolver mResolver;

	public RemotePlaysHandler() {
		super(BggContract.CONTENT_AUTHORITY);
	}

	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
			throws XmlPullParserException, IOException {

		mParser = parser;
		mResolver = resolver;

		int playCount = 0;
		int page = 0;

		int type;
		while ((type = parser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.PLAYS.equals(parser.getName())) {
				playCount = StringUtils.parseInt(parser.getAttributeValue(null, Tags.TOTAL));
				page = StringUtils.parseInt(parser.getAttributeValue(null, Tags.PAGE));

				parsePlays();
			}
		}

		return playCount > (page * PAGE_SIZE);
	}

	private void parsePlays() throws XmlPullParserException, IOException {

		String[] projection = { BaseColumns._ID, };
		final int depth = mParser.getDepth();
		ContentValues values = new ContentValues();

		int updateCount = 0;
		int insertCount = 0;

		Cursor cursor = null;
		try {
			int playId = 0;
			String date = "";
			boolean isComments = false;
			List<Integer> itemObjectIds = new ArrayList<Integer>();
			List<Integer> playerUserIds = new ArrayList<Integer>();

			int type;
			while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
				if (type == START_TAG) {
					String tag = mParser.getName();

					if (Tags.PLAY.equals(tag)) {
						playId = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.ID));

						if (playId > 0) {
							date = mParser.getAttributeValue(null, Tags.DATE);
							values.clear();
							values.put(Plays.DATE, date);
							values.put(Plays.QUANTITY, Integer.valueOf(mParser.getAttributeValue(null, Tags.QUANTITY)));
							values.put(Plays.LENGTH, Integer.valueOf(mParser.getAttributeValue(null, Tags.LENGTH)));
							values.put(Plays.INCOMPLETE, !"0".equals(mParser.getAttributeValue(null, Tags.INCOMPLETE)));
							values.put(Plays.NO_WIN_STATS,
									!"0".equals(mParser.getAttributeValue(null, Tags.NO_WIN_STATS)));
							values.put(Plays.LOCATION, mParser.getAttributeValue(null, Tags.LOCATION));
							values.put(Plays.UPDATED_LIST, System.currentTimeMillis());

							Uri uri = Plays.buildPlayUri(playId);
							cursor = mResolver.query(uri, projection, null, null, null);
							if (cursor.moveToFirst()) {
								itemObjectIds = getIds(Plays.buildItemUri(playId), PlayItems.OBJECT_ID);

								mResolver.delete(Plays.buildPlayerUri(playId), PlayPlayers.USER_ID + " IS NULL", null);
								playerUserIds = getIds(Plays.buildPlayerUri(playId), PlayPlayers.USER_ID);
								playerUserIds = removeDuplicateIds(playId, playerUserIds);

								mResolver.update(uri, values, null, null);
								updateCount++;
							} else {
								values.put(Plays.PLAY_ID, playId);
								mResolver.insert(Plays.CONTENT_URI, values);
								insertCount++;
							}
							cursor.deactivate();
						}
					} else if (Tags.ITEM.equals(tag)) {
						int objectId = Integer.valueOf(mParser.getAttributeValue(null, Tags.OBJECT_ID));
						values.clear();
						values.put(PlayItems.NAME, mParser.getAttributeValue(null, Tags.NAME));

						if (itemObjectIds != null && itemObjectIds.remove(new Integer(objectId))) {
							mResolver.update(Plays.buildItemUri(playId, objectId), values, null, null);
						} else {
							values.put(PlayItems.OBJECT_ID, objectId);
							mResolver.insert(Plays.buildItemUri(playId), values);
						}
					} else if (Tags.COMMENTS.equals(tag)) {
						isComments = true;
					} else if (Tags.PLAYER.equals(tag)) {
						int userId = Integer.valueOf(mParser.getAttributeValue(null, Tags.USERID));
						values.clear();
						values.put(PlayPlayers.USER_NAME, mParser.getAttributeValue(null, Tags.USERNAME));
						values.put(PlayPlayers.USER_ID, userId);
						values.put(PlayPlayers.NAME, mParser.getAttributeValue(null, Tags.NAME));
						values.put(PlayPlayers.START_POSITION, mParser.getAttributeValue(null, Tags.STARTPOSITION));
						values.put(PlayPlayers.COLOR, mParser.getAttributeValue(null, Tags.COLOR));
						values.put(PlayPlayers.SCORE, mParser.getAttributeValue(null, Tags.SCORE));
						values.put(PlayPlayers.NEW, Integer.valueOf(mParser.getAttributeValue(null, Tags.NEW)));
						values.put(PlayPlayers.RATING, Double.valueOf(mParser.getAttributeValue(null, Tags.RATING)));
						values.put(PlayPlayers.WIN, Integer.valueOf(mParser.getAttributeValue(null, Tags.WIN)));

						if (playerUserIds != null && playerUserIds.remove(new Integer(userId))) {
							mResolver.update(Plays.buildPlayerUri(playId), values, PlayPlayers.USER_ID + "=?",
									new String[] { String.valueOf(userId) });
						} else {
							values.put(PlayPlayers.USER_ID, userId);
							mResolver.insert(Plays.buildPlayerUri(playId), values);
						}
					}
				} else if (type == TEXT) {
					if (isComments) {
						values.clear();
						values.put(Plays.COMMENTS, mParser.getText());
						Uri uri = Plays.buildPlayUri(playId);
						mResolver.update(uri, values, null, null);
					}
				} else if (type == END_TAG) {
					String tag = mParser.getName();
					if (Tags.PLAY.equals(tag)) {
						if (itemObjectIds != null) {
							for (Integer itemObjectId : itemObjectIds) {
								mResolver.delete(Plays.buildItemUri(playId, itemObjectId), null, null);
							}
							itemObjectIds.clear();
						}

						if (playerUserIds != null) {
							for (Integer playerUserId : playerUserIds) {
								mResolver.delete(Plays.buildPlayerUri(playId), PlayPlayers.USER_ID + "=?",
										new String[] { String.valueOf(playerUserId) });
							}
							playerUserIds.clear();
						}

						if (!TextUtils.isEmpty(date)) {
							String maxDate = BggApplication.getInstance().getMaxPlayDate();
							if ((date.compareTo(maxDate)) < 0) {
								BggApplication.getInstance().putMaxPlayDate(date);
							}
							String minDate = BggApplication.getInstance().getMinPlayDate();
							if ((date.compareTo(minDate)) > 0) {
								BggApplication.getInstance().putMinPlayDate(date);
							}
						}
					} else if (Tags.COMMENTS.equals(tag)) {
						isComments = false;
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

	private List<Integer> getIds(Uri uri, String columnName) {
		List<Integer> list = new ArrayList<Integer>();
		Cursor c = mResolver.query(uri, new String[] { columnName }, null, null, null);
		try {
			while (c.moveToNext()) {
				list.add(c.getInt(0));
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return list;
	}

	private List<Integer> removeDuplicateIds(int playId, List<Integer> ids) {
		if (ids == null || ids.size() == 0) {
			return new ArrayList<Integer>();
		}

		List<Integer> uniqueIds = new ArrayList<Integer>();
		List<Integer> idsToDelete = new ArrayList<Integer>();

		for (int i = 0; i < ids.size(); i++) {
			Integer id = ids.get(i);
			if (uniqueIds.contains(id)) {
				idsToDelete.add(id);
			} else {
				uniqueIds.add(id);
			}
		}

		for (Integer id : idsToDelete) {
			mResolver.delete(Plays.buildPlayerUri(playId), PlayPlayers.USER_ID + "=?",
					new String[] { String.valueOf(id) });
			uniqueIds.remove(id);
		}

		return uniqueIds;
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

		String COMMENTS = "comments";

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
