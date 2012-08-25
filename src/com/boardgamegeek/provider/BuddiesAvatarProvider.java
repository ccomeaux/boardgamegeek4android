package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Buddies;

public class BuddiesAvatarProvider extends BaseFileProvider {
	@Override
	protected String getPath() {
		return "buddies/#/avatars";
	}

	@Override
	protected Uri getFileUri(Uri uri) {
		return Buddies.buildBuddyUri(Buddies.getBuddyId(uri));
	}

	@Override
	protected String getColumnName() {
		return Buddies.AVATAR_URL;
	}

	@Override
	protected String getContentPath() {
		return BggContract.PATH_AVATARS;
	}
}
