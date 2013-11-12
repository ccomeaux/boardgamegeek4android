package com.boardgamegeek.io;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.provider.BggContract.Avatars;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.StringUtils;

public class RemoteBuddyUserHandler extends RemoteBggHandler {
	private static final String TAG = makeLogTag(RemoteBuddyUserHandler.class);

	private int mCount;
	private long mStartTime;

	public RemoteBuddyUserHandler(long startTime) {
		super();
		mStartTime = startTime;
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
		if (id == 0) {
			// No ID indicates the was an invalid user
			return;
		}

		String name = mParser.getAttributeValue(null, Tags.NAME);
		ContentValues values = parseUser();

		Uri uri = Buddies.buildBuddyUri(id);
		if (!ResolverUtils.rowExists(mResolver, uri)) {
			Account account = Authenticator.getAccount(getContext());
			if (account != null && name.equals(account.name)) {
				mBatch.add(ContentProviderOperation.newUpdate(Buddies.CONTENT_URI)
					.withSelection(Buddies.BUDDY_NAME + "=?", new String[] { name }).withValues(values).build());
				mCount++;
			} else {
				LOGW(TAG, "Tried to parse user, but ID not in database: " + id);
			}
		} else {
			maybeDeleteAvatar(values, uri);
			addUpdate(uri, values);
			mCount++;
		}
	}

	private void maybeDeleteAvatar(ContentValues values, Uri uri) {
		if (!values.containsKey(Buddies.AVATAR_URL)) {
			return;
		}

		String oldAvatarUrl = ResolverUtils.queryString(mResolver, uri, Buddies.AVATAR_URL);
		String newAvatarUrl = values.getAsString(Buddies.AVATAR_URL);
		if (newAvatarUrl.equals(oldAvatarUrl)) {
			return;
		}

		String avatarFileName = FileUtils.getFileNameFromUrl(oldAvatarUrl);
		if (!TextUtils.isEmpty(avatarFileName)) {
			addDelete(Avatars.buildUri(avatarFileName));
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

		values.put(Buddies.UPDATED, mStartTime);
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
