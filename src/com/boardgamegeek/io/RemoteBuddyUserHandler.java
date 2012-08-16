package com.boardgamegeek.io;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.net.Uri;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.database.ResolverUtils;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.StringUtils;

public class RemoteBuddyUserHandler extends RemoteBggHandler {
	private static final String TAG = makeLogTag(RemoteBuddyUserHandler.class);

	private int mCount;

	public RemoteBuddyUserHandler() {
		super();
	}

	@Override
	public int getCount() {
		return mCount;
	}

	@Override
	protected String getRootNodeName() {
		return Tags.USER;
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {
		int id = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.ID));
		String name = mParser.getAttributeValue(null, Tags.NAME);
		ContentValues values = parseUser();

		Uri uri = Buddies.buildBuddyUri(id);
		if (ResolverUtils.rowExists(mResolver, uri)) {
			if (name.equals(BggApplication.getInstance().getUserName())) {
				mBatch.add(ContentProviderOperation.newUpdate(Buddies.CONTENT_URI)
						.withSelection(Buddies.BUDDY_NAME + "=?", new String[] { name }).withValues(values).build());
				mCount++;
			} else {
				LOGW(TAG, "Tried to parse user, but ID not in database: " + id);
			}
		} else {
			mBatch.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
			mCount++;
		}
	}

	private ContentValues parseUser() throws XmlPullParserException, IOException {
		final int depth = mParser.getDepth();
		ContentValues values = new ContentValues();

		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG) {
				final String tag = mParser.getName();
				final String attribute = mParser.getAttributeValue(null, Tags.VALUE);
				if (Tags.FIRSTNAME.equals(tag)) {
					values.put(Buddies.BUDDY_FIRSTNAME, attribute);
				} else if (Tags.LASTNAME.equals(tag)) {
					values.put(Buddies.BUDDY_LASTNAME, attribute);
				} else if (Tags.AVATAR.equals(tag)) {
					values.put(Buddies.AVATAR_URL, attribute);
				}
			}
		}

		values.put(Buddies.UPDATED, System.currentTimeMillis());
		return values;
	}

	private interface Tags {
		String USER = "user";
		String ID = "id";
		String NAME = "name";
		String FIRSTNAME = "firstname";
		String LASTNAME = "lastname";
		String AVATAR = "avatarlink";
		String VALUE = "value";
	}
}
