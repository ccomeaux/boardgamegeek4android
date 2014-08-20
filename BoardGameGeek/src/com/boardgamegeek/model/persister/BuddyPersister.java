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
	private Context mContext;
	private long mUpdateTime;

	public BuddyPersister(Context context) {
		mContext = context;
		mUpdateTime = System.currentTimeMillis();
	}

	public long getTimestamp() {
		return mUpdateTime;
	}

	public int save(User buddy) {
		List<User> buddies = new ArrayList<User>(1);
		buddies.add(buddy);
		return save(buddies);
	}

	public int save(List<User> buddies) {
		ContentResolver resolver = mContext.getContentResolver();
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		if (buddies != null) {
			for (User buddy : buddies) {
				ContentValues values = toValues(buddy);
				addToBatch(resolver, values, batch);
			}
		}
		ContentProviderResult[] result = ResolverUtils.applyBatch(mContext, batch);
		if (result == null) {
			return 0;
		} else {
			return result.length;
		}
	}

	public int saveList(Buddy buddy) {
		List<Buddy> buddies = new ArrayList<Buddy>(1);
		buddies.add(buddy);
		return saveList(buddies);
	}

	public int saveList(List<Buddy> buddies) {
		ContentResolver resolver = mContext.getContentResolver();
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		if (buddies != null) {
			for (Buddy buddy : buddies) {
				ContentValues values = toValues(buddy);
				addToBatch(resolver, values, batch);
			}
		}
		ContentProviderResult[] result = ResolverUtils.applyBatch(mContext, batch);
		if (result == null) {
			return 0;
		} else {
			return result.length;
		}
	}

	private void addToBatch(ContentResolver resolver, ContentValues values, ArrayList<ContentProviderOperation> batch) {
		String name = values.getAsString(Buddies.BUDDY_NAME);
		Uri uri = Buddies.buildBuddyUri(name);
		if (!ResolverUtils.rowExists(resolver, uri)) {
			values.put(Buddies.UPDATED_LIST, mUpdateTime);
			batch.add(ContentProviderOperation.newInsert(Buddies.CONTENT_URI).withValues(values).build());
		} else {
			maybeDeleteAvatar(values, uri, resolver);
			values.remove(Buddies.BUDDY_NAME);
			batch.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
		}
	}

	private ContentValues toValues(User buddy) {
		ContentValues values = new ContentValues();
		values.put(Buddies.BUDDY_ID, buddy.id);
		values.put(Buddies.BUDDY_NAME, buddy.name);
		values.put(Buddies.BUDDY_FIRSTNAME, buddy.firstName);
		values.put(Buddies.BUDDY_LASTNAME, buddy.lastName);
		values.put(Buddies.AVATAR_URL, buddy.avatarUrl);
		values.put(Buddies.UPDATED, mUpdateTime);
		return values;
	}

	private ContentValues toValues(Buddy buddy) {
		ContentValues values = new ContentValues();
		values.put(Buddies.BUDDY_ID, buddy.id);
		values.put(Buddies.BUDDY_NAME, buddy.name);
		values.put(Buddies.UPDATED_LIST, mUpdateTime);
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
