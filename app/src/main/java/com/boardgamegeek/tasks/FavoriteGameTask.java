package com.boardgamegeek.tasks;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.boardgamegeek.provider.BggContract.Games;

public class FavoriteGameTask extends AsyncTask<Void, Void, Boolean> {
	@SuppressLint("StaticFieldLeak") @Nullable private final Context context;
	private final int gameId;
	private final boolean isFavorite;

	public FavoriteGameTask(@Nullable Context context, int gameId, boolean isFavorite) {
		this.context = context == null ? null : context.getApplicationContext();
		this.gameId = gameId;
		this.isFavorite = isFavorite;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (context == null) return false;
		final ContentResolver resolver = context.getContentResolver();
		ContentValues values = new ContentValues();
		values.put(Games.STARRED, isFavorite ? 1 : 0);
		return resolver.update(Games.buildGameUri(gameId), values, null, null) > 0;
	}
}
