package com.boardgamegeek.io;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParserException;

import android.database.Cursor;
import android.text.TextUtils;

import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.model.persister.PlayPersister;

public class RemotePlaysHandler extends RemoteBggHandler {
	private static final String TAG = makeLogTag(RemotePlaysHandler.class);
	private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

	private int mTotalCount = 0;
	private Play mPlay;
	private long mNewestDate;
	private long mOldestDate;

	public RemotePlaysHandler() {
		super();
		mNewestDate = 0;
		mOldestDate = Long.MAX_VALUE;
	}

	public long getNewestDate() {
		return mNewestDate;
	}

	public long getOldestDate() {
		return mOldestDate;
	}

	public void setDatesMaybe(String date) {
		if (TextUtils.isEmpty(date)) {
			return;
		}

		try {
			Date parsedDate = formatter.parse(date);
			long time = parsedDate.getTime();
			if (time > mNewestDate) {
				mNewestDate = time;
			}
			if (time < mOldestDate) {
				mOldestDate = time;
			}
		} catch (ParseException e) {
			LOGE(TAG, "Bad date: " + date);
		}
	}

	@Override
	public int getCount() {
		return mTotalCount;
	}

	@Override
	protected String getRootNodeName() {
		return Tags.PLAYS;
	}

	@Override
	protected String getTotalCountAttributeName() {
		return Tags.TOTAL;
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {
		final int depth = mParser.getDepth();

		int updateCount = 0;
		int insertCount = 0;
		int pendingUpdateCount = 0;
		int pendingDeleteCount = 0;
		int inProgressCount = 0;
		int errorCount = 0;

		Cursor cursor = null;
		try {
			String date = "";
			boolean isComments = false;

			int type;
			while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
				if (type == START_TAG) {
					String tag = mParser.getName();

					if (Tags.PLAY.equals(tag)) {
						mPlay = new Play();
						mPlay.PlayId = parseIntegerAttribute(Tags.ID);
						mPlay.setDate(parseStringAttribute(Tags.DATE));
						mPlay.Quantity = parseIntegerAttribute(Tags.QUANTITY);
						mPlay.Length = parseIntegerAttribute(Tags.LENGTH);
						mPlay.Incomplete = parseBooleanAttribute(Tags.INCOMPLETE);
						mPlay.NoWinStats = parseBooleanAttribute(Tags.NO_WIN_STATS);
						mPlay.Location = parseStringAttribute(Tags.LOCATION);
						mPlay.Updated = System.currentTimeMillis();
					} else if (Tags.ITEM.equals(tag)) {
						mPlay.GameId = parseIntegerAttribute(Tags.OBJECT_ID);
						mPlay.GameName = parseStringAttribute(Tags.NAME);
					} else if (Tags.COMMENTS.equals(tag)) {
						isComments = true;
					} else if (Tags.PLAYER.equals(tag)) {
						Player player = new Player();
						player.UserId = parseIntegerAttribute(Tags.USERID);
						player.Username = parseStringAttribute(Tags.USERNAME);
						player.Name = parseStringAttribute(Tags.NAME);
						player.setStartingPosition(parseStringAttribute(Tags.STARTPOSITION));
						player.TeamColor = parseStringAttribute(Tags.COLOR);
						player.Score = parseStringAttribute(Tags.SCORE);
						player.New = parseBooleanAttribute(Tags.NEW);
						player.Rating = parseDoubleAttribute(Tags.RATING);
						player.Win = parseBooleanAttribute(Tags.WIN);
						mPlay.addPlayer(player);
					}
				} else if (type == TEXT) {
					if (isComments) {
						mPlay.Comments = mParser.getText();
					}
				} else if (type == END_TAG) {
					String tag = mParser.getName();
					if (Tags.PLAY.equals(tag)) {
						mTotalCount++;
						int status = PlayPersister.save(mResolver, mPlay, true);
						switch (status) {
							case PlayPersister.STATUS_IN_PROGRESS:
								inProgressCount++;
								break;
							case PlayPersister.STATUS_INSERT:
								insertCount++;
								break;
							case PlayPersister.STATUS_PENDING_UPDATE:
								pendingUpdateCount++;
								break;
							case PlayPersister.STATUS_PENDING_DELETE:
								pendingDeleteCount++;
								break;
							case PlayPersister.STATUS_UPDATE:
								updateCount++;
								break;
							case PlayPersister.STATUS_ERROR:
							case PlayPersister.STATUS_UNKNOWN:
								errorCount++;
								break;
							default:
								break;
						}
						setDatesMaybe(date);
					} else if (Tags.COMMENTS.equals(tag)) {
						isComments = false;
					}
				}
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			String msg = String
				.format(
					"Updated %1$s, inserted %2$s, skipped %3$s (%4$s pending update, %5$s pending delete, %6$s draft, %7$s errors)",
					updateCount, insertCount, (pendingUpdateCount + pendingDeleteCount + inProgressCount + errorCount),
					pendingUpdateCount, pendingDeleteCount, inProgressCount, errorCount);
			LOGI(TAG, msg);
		}
	}

	private interface Tags {
		String PLAYS = "plays";
		String TOTAL = "total";

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