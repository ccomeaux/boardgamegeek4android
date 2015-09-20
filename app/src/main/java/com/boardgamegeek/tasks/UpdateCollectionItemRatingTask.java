package com.boardgamegeek.tasks;


import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.CollectionConverter;
import com.boardgamegeek.model.CollectionPostResponse;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class UpdateCollectionItemRatingTask extends AsyncTask<Void, Void, CollectionPostResponse> {
	private final Context context;
	private final int gameId;
	private final int collectionId;
	private final double rating;

	public UpdateCollectionItemRatingTask(Context context, int gameId, int collectionId, double rating) {
		this.context = context;
		this.gameId = gameId;
		this.collectionId = collectionId;
		this.rating = rating;
	}

	@Override
	protected CollectionPostResponse doInBackground(Void... params) {
		BggService service = Adapter.createForPost(context, new CollectionConverter());

		Map<String, String> form = new HashMap<>();
		form.put("ajax", "1");
		form.put("action", "savedata");
		form.put("objecttype", "thing");
		form.put("objectid", String.valueOf(gameId));
		form.put("collid", String.valueOf(collectionId));
		form.put("fieldname", "rating");
		form.put("rating", String.valueOf(rating));

		try {
			return service.geekCollection(form);
		} catch (Exception e) {
			return new CollectionPostResponse(e);
		}
	}

	@Override
	protected void onPostExecute(CollectionPostResponse response) {
		// TEMP
		String message;
		if (response.hasAuthError()) {
			message = "Sign in again!";
		} else if (response.hasError()) {
			message = response.getErrorMessage();
		} else {
			message = "Rated " + String.valueOf(response.getRating());
		}
		Timber.i(message);
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}
}
