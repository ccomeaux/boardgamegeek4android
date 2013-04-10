package com.boardgamegeek.provider;

import android.content.Context;
import android.net.Uri;

public class ThumbnailsIdProvider extends BaseFileProvider {
	@Override
	protected String getContentPath() {
		return BggContract.PATH_THUMBNAILS;
	}

	@Override
	protected String generateFileName(Context context, Uri uri) {
		return uri.getLastPathSegment();
	}

	@Override
	protected String getPath() {
		return addWildCardToPath(BggContract.PATH_THUMBNAILS);
	}

}
