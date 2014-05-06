package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.User;
import com.boardgamegeek.provider.BggContract.Avatars;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.ResolverUtils;

public class SyncBuddy extends UpdateTask {
	private static final String TAG = makeLogTag(SyncBuddy.class);
	private String mName;

	public SyncBuddy(String name) {
		mName = name;
	}

	@Override
	public void execute(Context context) {
		BggService service = Adapter.create();
		User user = service.user(mName);

		if (user == null || user.id == 0) {
			LOGI(TAG, "Invalid user: " + mName);
			return;
		}

		ContentResolver resolver = context.getContentResolver();

		Uri uri = Buddies.buildBuddyUri(user.id);
		if (!ResolverUtils.rowExists(resolver, uri)) {
			Account account = Authenticator.getAccount(context);
			if (account != null && user.name.equals(account.name)) {
				resolver.update(Buddies.CONTENT_URI, toValues(user), Buddies.BUDDY_NAME + "=?",
					new String[] { user.name });
			} else {
				resolver.insert(uri, toValues(user));
			}
		} else {
			maybeDeleteAvatar(user.avatarUrl, uri, resolver);
			resolver.update(uri, toValues(user), null, null);
		}

		LOGI(TAG, "Synced Buddy " + mName);
	}

	private static ContentValues toValues(User buddy) {
		ContentValues values = new ContentValues();
		values.put(Buddies.BUDDY_ID, buddy.id);
		values.put(Buddies.BUDDY_NAME, buddy.name);
		values.put(Buddies.BUDDY_FIRSTNAME, buddy.firstName);
		values.put(Buddies.BUDDY_LASTNAME, buddy.lastName);
		values.put(Buddies.AVATAR_URL, buddy.avatarUrl);
		values.put(Buddies.UPDATED, System.currentTimeMillis());
		return values;
	}
	
	private void maybeDeleteAvatar(String newAvatarUrl, Uri uri, ContentResolver resolver) {
		String oldAvatarUrl = ResolverUtils.queryString(resolver, uri, Buddies.AVATAR_URL);
		if (newAvatarUrl.equals(oldAvatarUrl)) {
			// nothing to do - avatar hasn't changed
			return;
		}

		String avatarFileName = FileUtils.getFileNameFromUrl(oldAvatarUrl);
		if (!TextUtils.isEmpty(avatarFileName)) {
			resolver.delete(Avatars.buildUri(avatarFileName), null, null);
		}
	}
}
