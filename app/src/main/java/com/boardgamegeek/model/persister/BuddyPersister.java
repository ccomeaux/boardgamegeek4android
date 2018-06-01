package com.boardgamegeek.model.persister;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.model.User;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Avatars;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.ResolverUtils;

import timber.log.Timber;

public class BuddyPersister {
	private final Context context;
	private long updateTime;

	public BuddyPersister(Context context) {
		this.context = context;
		updateTime = System.currentTimeMillis();
	}

	public long getTimestamp() {
		return updateTime;
	}

	public void resetTimestamp() {
		updateTime = System.currentTimeMillis();
	}

	public int saveUser(User buddy) {
		if (buddy != null && !TextUtils.isEmpty(buddy.name)) {
			ContentValues values = new ContentValues();
			values.put(Buddies.UPDATED, updateTime);
			values.put(Buddies.UPDATED_LIST, updateTime);
			int oldSyncHashCode = ResolverUtils.queryInt(context.getContentResolver(), Buddies.buildBuddyUri(buddy.name), Buddies.SYNC_HASH_CODE);
			int newSyncHashCode = generateSyncHashCode(buddy);
			if (oldSyncHashCode != newSyncHashCode) {
				values.put(Buddies.BUDDY_ID, buddy.getId());
				values.put(Buddies.BUDDY_NAME, buddy.name);
				values.put(Buddies.BUDDY_FIRSTNAME, buddy.firstName);
				values.put(Buddies.BUDDY_LASTNAME, buddy.lastName);
				values.put(Buddies.AVATAR_URL, buddy.avatarUrl);
				values.put(Buddies.SYNC_HASH_CODE, newSyncHashCode);
			}
			return upsert(values, buddy.name, buddy.getId());
		}
		return 0;
	}

	public int saveBuddy(int userId, String username, boolean isBuddy) {
		if (userId != BggContract.INVALID_ID && !TextUtils.isEmpty(username)) {
			ContentValues values = new ContentValues();
			values.put(Buddies.BUDDY_ID, userId);
			values.put(Buddies.BUDDY_NAME, username);
			values.put(Buddies.BUDDY_FLAG, isBuddy ? 1 : 0);
			values.put(Buddies.UPDATED_LIST, updateTime);

			return upsert(values, username, userId);
		} else {
			Timber.i("Un-savable buddy %s (%d)", username, userId);
		}
		return 0;
	}

	private int upsert(ContentValues values, String username, int userId) {
		ContentResolver resolver = context.getContentResolver();
		Uri uri = Buddies.buildBuddyUri(username);
		if (ResolverUtils.rowExists(resolver, uri)) {
			values.remove(Buddies.BUDDY_NAME);
			int count = resolver.update(uri, values, null, null);
			Timber.d("Updated %,d buddy rows at %s", count, uri);
			maybeDeleteAvatar(values, uri);
			return count;
		} else {
			values.put(Buddies.BUDDY_NAME, username);
			values.put(Buddies.BUDDY_ID, userId);
			Uri insertedUri = resolver.insert(Buddies.CONTENT_URI, values);
			Timber.d("Inserted buddy at %s", insertedUri);
			return 1;
		}
	}

	private static int generateSyncHashCode(User buddy) {
		return (buddy.firstName + "\n" + buddy.lastName + "\n" + buddy.avatarUrl + "\n").hashCode();
	}

	private void maybeDeleteAvatar(ContentValues values, Uri uri) {
		if (!values.containsKey(Buddies.AVATAR_URL)) {
			// nothing to do - no avatar
			return;
		}

		String newAvatarUrl = values.getAsString(Buddies.AVATAR_URL);
		if (newAvatarUrl == null) {
			newAvatarUrl = "";
		}

		String oldAvatarUrl = ResolverUtils.queryString(context.getContentResolver(), uri, Buddies.AVATAR_URL);
		if (newAvatarUrl.equals(oldAvatarUrl)) {
			// nothing to do - avatar hasn't changed
			return;
		}

		String avatarFileName = FileUtils.getFileNameFromUrl(oldAvatarUrl);
		if (!TextUtils.isEmpty(avatarFileName)) {
			context.getContentResolver().delete(Avatars.buildUri(avatarFileName), null, null);
		}
	}
}
