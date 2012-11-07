package com.boardgamegeek.provider;

import com.boardgamegeek.provider.BggContract.Collection;

import android.net.Uri;

public class CollectionIdThumbnailProvider extends IndirectFileProvider {

	@Override
	protected Uri getFileUri(Uri uri) {
		return Collection.buildItemUri(Collection.getItemId(uri));
	}

	@Override
	protected String getColumnName() {
		return Collection.THUMBNAIL_URL;
	}

	@Override
	protected String getContentPath() {
		return BggContract.PATH_THUMBNAILS;
	}

	@Override
	protected String getPath() {
		return "collection/#/thumbnails";
	}
}
