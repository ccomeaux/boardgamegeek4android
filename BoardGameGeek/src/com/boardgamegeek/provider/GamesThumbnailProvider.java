package com.boardgamegeek.provider;

import com.boardgamegeek.provider.BggContract.Games;

import android.net.Uri;

public class GamesThumbnailProvider extends BaseFileProvider {

	@Override
	protected Uri getFileUri(Uri uri) {
		return Games.buildGameUri(Games.getGameId(uri));
	}

	@Override
	protected String getColumnName() {
		return Games.THUMBNAIL_URL;
	}

	@Override
	protected String getContentPath() {
		return BggContract.PATH_THUMBNAILS;
	}

	@Override
	protected String getPath() {
		return "games/#/thumbnails";
	}
}
