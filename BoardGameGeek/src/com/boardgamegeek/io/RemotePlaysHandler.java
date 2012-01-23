package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.database.PlayHelper;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.StringUtils;

public class RemotePlaysHandler extends XmlHandler {
	private static final String TAG = "RemotePlaysHandler";

	private static final int PAGE_SIZE = 100;

	private XmlPullParser mParser;
	private ContentResolver mResolver;
	private Play mPlay;

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
		final int depth = mParser.getDepth();

		int updateCount = 0;
		int insertCount = 0;

		Cursor cursor = null;
		try {
			String date = "";
			boolean isComments = false;

			int type;
			while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
				if (type == START_TAG) {
					String tag = mParser.getName();

					if (Tags.PLAY.equals(tag)) {
						date = mParser.getAttributeValue(null, Tags.DATE);

						mPlay = new Play();
						mPlay.PlayId = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.ID));
						mPlay.setDate(date);
						mPlay.Quantity = Integer.valueOf(mParser.getAttributeValue(null, Tags.QUANTITY));
						mPlay.Length = Integer.valueOf(mParser.getAttributeValue(null, Tags.LENGTH));
						mPlay.Incomplete = !"0".equals(mParser.getAttributeValue(null, Tags.INCOMPLETE));
						mPlay.NoWinStats = !"0".equals(mParser.getAttributeValue(null, Tags.NO_WIN_STATS));
						mPlay.Location = mParser.getAttributeValue(null, Tags.LOCATION);
					} else if (Tags.ITEM.equals(tag)) {
						mPlay.GameId = Integer.valueOf(mParser.getAttributeValue(null, Tags.OBJECT_ID));
						mPlay.GameName = mParser.getAttributeValue(null, Tags.NAME);
					} else if (Tags.COMMENTS.equals(tag)) {
						isComments = true;
					} else if (Tags.PLAYER.equals(tag)) {
						Player player = new Player();
						player.UserId = Integer.valueOf(mParser.getAttributeValue(null, Tags.USERID));
						player.Username = mParser.getAttributeValue(null, Tags.USERNAME);
						player.Name = mParser.getAttributeValue(null, Tags.NAME);
						player.StartingPosition = mParser.getAttributeValue(null, Tags.STARTPOSITION);
						player.TeamColor = mParser.getAttributeValue(null, Tags.COLOR);
						player.Score = mParser.getAttributeValue(null, Tags.SCORE);
						player.New = "1".equals(mParser.getAttributeValue(null, Tags.NEW));
						player.Rating = Double.valueOf(mParser.getAttributeValue(null, Tags.RATING));
						player.Win = "1".equals(mParser.getAttributeValue(null, Tags.WIN));
						mPlay.addPlayer(player);
					}
				} else if (type == TEXT) {
					if (isComments) {
						mPlay.Comments = mParser.getText();
					}
				} else if (type == END_TAG) {
					String tag = mParser.getName();
					if (Tags.PLAY.equals(tag)) {
						PlayHelper helper = new PlayHelper(mResolver, mPlay);
						helper.save();
						if (helper.getIsUpdate()) {
							updateCount++;
						} else {
							insertCount++;
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