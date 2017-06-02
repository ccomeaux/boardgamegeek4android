package com.boardgamegeek.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;

import com.boardgamegeek.provider.BggContract.Games;

public class FavoriteGameTask extends AsyncTask<Void, Void, Boolean> {
	private final Context context;
	private final int gameId;
	private final boolean isFavorite;

	public FavoriteGameTask(Context context, int gameId, boolean isFavorite) {
		this.context = context;
		this.gameId = gameId;
		this.isFavorite = isFavorite;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		final ContentResolver resolver = context.getContentResolver();
		ContentValues values = new ContentValues();
		values.put(Games.STARRED, isFavorite ? 1 : 0);
		return resolver.update(Games.buildGameUri(gameId), values, null, null) > 0;
	}
}
