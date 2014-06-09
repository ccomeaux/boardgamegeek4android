package com.boardgamegeek.model.persister;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.model.Buddy;
import com.boardgamegeek.model.User;
import com.boardgamegeek.provider.BggContract.Avatars;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.ResolverUtils;

public class BuddyPersister {
	public static int save(Context context, User buddy) {
		return save(context, buddy, System.currentTimeMillis());
	}

	public static int save(Context context, User buddy, long updateTime) {
		List<User> buddies = new ArrayList<User>(1);
		buddies.add(buddy);
		return save(context, buddies, updateTime);
	}

	public static int save(Context context, List<User> buddies, long updateTime) {
		ContentResolver resolver = context.getContentResolver();
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		if (buddies != null) {
			for (User buddy : buddies) {
				ContentValues values = toValues(buddy, updateTime);
				addToBatch(resolver, values, batch);
			}
		}
		ContentProviderResult[] result = ResolverUtils.applyBatch(resolver, batch);
		if (result == null) {
			return 0;
		} else {
			return result.length;
		}
	}

	public static int saveList(Context context, Buddy buddy, long updateTime) {
		List<Buddy> buddies = new ArrayList<Buddy>(1);
		buddies.add(buddy);
		return saveList(context, buddies, updateTime);
	}

	public static int saveList(Context context, List<Buddy> buddies, long updateTime) {
		ContentResolver resolver = context.getContentResolver();
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		if (buddies != null) {
			for (Buddy buddy : buddies) {
				ContentValues values = toValues(buddy, updateTime);
				addToBatch(resolver, values, batch);
			}
		}
		ContentProviderResult[] result = ResolverUtils.applyBatch(resolver, batch);
		if (result == null) {
			return 0;
		} else {
			return result.length;
		}
	}

	private static void addToBatch(ContentResolver resolver, ContentValues values,
		ArrayList<ContentProviderOperation> batch) {
		int id = values.getAsInteger(Buddies.BUDDY_ID);
		Uri uri = Buddies.buildBuddyUri(id);
		if (!ResolverUtils.rowExists(resolver, uri)) {
			batch.add(ContentProviderOperation.newInsert(Buddies.CONTENT_URI).withValues(values).build());
		} else {
			maybeDeleteAvatar(values, uri, resolver);
			batch.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
		}
	}

	private static ContentValues toValues(User buddy, long updateTime) {
		ContentValues values = new ContentValues();
		values.put(Buddies.BUDDY_ID, buddy.id);
		values.put(Buddies.BUDDY_NAME, buddy.name);
		values.put(Buddies.BUDDY_FIRSTNAME, buddy.firstName);
		values.put(Buddies.BUDDY_LASTNAME, buddy.lastName);
		values.put(Buddies.AVATAR_URL, buddy.avatarUrl);
		values.put(Buddies.UPDATED, updateTime);
		return values;
	}

	private static ContentValues toValues(Buddy buddy, long updateTime) {
		ContentValues values = new ContentValues();
		values.put(Buddies.BUDDY_ID, buddy.id);
		values.put(Buddies.BUDDY_NAME, buddy.name);
		values.put(Buddies.UPDATED_LIST, updateTime);
		return values;
	}

	private static void maybeDeleteAvatar(ContentValues values, Uri uri, ContentResolver resolver) {
		if (!values.containsKey(Buddies.AVATAR_URL)) {
			// nothing to do - no avatar
			return;
		}

		String newAvatarUrl = values.getAsString(Buddies.AVATAR_URL);
		if (newAvatarUrl == null) {
			newAvatarUrl = "";
		}

		String oldAvatarUrl = ResolverUtils.queryString(resolver, uri, Buddies.AVATAR_URL);
		if (newAvatarUrl.equals(oldAvatarUrl)) {
			// nothing to do - avatar hasn't changed
			return;
		}

		String avatarFileName = FileUtils.getFileNameFromUrl(oldAvatarUrl);
		if (!TextUtils.isEmpty(avatarFileName)) {
			// TODO: use batch
			resolver.delete(Avatars.buildUri(avatarFileName), null, null);
		}
	}
}
