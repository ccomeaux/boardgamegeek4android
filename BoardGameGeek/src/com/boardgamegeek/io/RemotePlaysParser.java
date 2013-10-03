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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParserException;

import android.text.TextUtils;

import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;

public class RemotePlaysParser extends RemoteBggParser {
	private static final String TAG = makeLogTag(RemotePlaysHandler.class);
	private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

	private List<Play> mPlays = new ArrayList<Play>();
	private Play mPlay;
	private long mNewestDate;
	private long mOldestDate;

	public RemotePlaysParser() {
		super();
		mNewestDate = 0;
		mOldestDate = Long.MAX_VALUE;
	}

	public List<Play> getPlays() {
		return mPlays;
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
		return mPlays.size();
	}

	@Override
	protected void clearResults() {
		mPlays.clear();
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
						mPlays.add(mPlay);
						setDatesMaybe(date);
					} else if (Tags.COMMENTS.equals(tag)) {
						isComments = false;
					}
				}
			}
		} finally {
			LOGI(TAG, String.format("Parsed %1$s plays", mPlays.size()));
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
