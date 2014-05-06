package com.boardgamegeek.model.persister;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.model.User;
import com.boardgamegeek.provider.BggContract.Avatars;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.ResolverUtils;

public class BuddyPersister {
	public static void save(Context context, User buddy) {
		ContentResolver resolver = context.getContentResolver();
		Uri uri = Buddies.buildBuddyUri(buddy.id);
		if (!ResolverUtils.rowExists(resolver, uri)) {
			Account account = Authenticator.getAccount(context);
			if (account != null && buddy.name.equals(account.name)) {
				resolver.update(Buddies.CONTENT_URI, toValues(buddy), Buddies.BUDDY_NAME + "=?",
					new String[] { buddy.name });
			} else {
				resolver.insert(uri, toValues(buddy));
			}
		} else {
			maybeDeleteAvatar(buddy.avatarUrl, uri, resolver);
			resolver.update(uri, toValues(buddy), null, null);
		}
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

	private static void maybeDeleteAvatar(String newAvatarUrl, Uri uri, ContentResolver resolver) {
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
