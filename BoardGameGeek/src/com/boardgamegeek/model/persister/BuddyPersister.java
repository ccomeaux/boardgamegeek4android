package com.boardgamegeek.model.persister;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
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
		save(context, buddy, System.currentTimeMillis());
	}

	public static int save(Context context, User buddy, long updateTime) {
		ContentResolver resolver = context.getContentResolver();
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		addToBatch(context, resolver, buddy, updateTime, batch);
		ContentProviderResult[] result = ResolverUtils.applyBatch(resolver, batch);
		if (result == null) {
			return 0;
		} else {
			return result.length;
		}
	}

	public static int save(Context context, List<User> buddies, long updateTime) {
		ContentResolver resolver = context.getContentResolver();
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		if (buddies != null) {
			for (User buddy : buddies) {
				addToBatch(context, resolver, buddy, updateTime, batch);
			}
		}
		ContentProviderResult[] result = ResolverUtils.applyBatch(resolver, batch);
		if (result == null) {
			return 0;
		} else {
			return result.length;
		}
	}

	private static void addToBatch(Context context, ContentResolver resolver, User buddy, long updateTime,
		ArrayList<ContentProviderOperation> batch) {
		ContentValues values = toValues(buddy, updateTime);
		Uri uri = Buddies.buildBuddyUri(buddy.id);
		if (!ResolverUtils.rowExists(resolver, uri)) {
			Account account = Authenticator.getAccount(context);
			if (account != null && buddy.name.equals(account.name)) {
				batch.add(ContentProviderOperation.newUpdate(Buddies.CONTENT_URI)
					.withSelection(Buddies.BUDDY_NAME + "=?", new String[] { buddy.name }).withValues(values).build());
			} else {
				batch.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
			}
		} else {
			maybeDeleteAvatar(buddy.avatarUrl, uri, resolver);
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
