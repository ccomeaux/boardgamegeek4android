package com.boardgamegeek.service;

import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.TextUtils;

import com.boardgamegeek.service.model.CollectionItem;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;

public abstract class CollectionTask {
	private static final String GEEK_COLLECTION_URL = "https://www.boardgamegeek.com/geekcollection.php";
	private static final String ERROR_DIV = "<div class='messagebox error'>";
	private static final String AUTH_ERROR_TEXT = "login";
	protected final OkHttpClient client;
	protected CollectionItem collectionItem;
	protected String error;
	protected Exception exception;

	public CollectionTask(OkHttpClient client) {
		this.client = client;
	}

	public void addCollectionItem(CollectionItem collectionItem) {
		this.collectionItem = collectionItem;
	}

	public void post() {
		Request request = new Builder()
			.url(GEEK_COLLECTION_URL)
			.post(createForm(collectionItem))
			.build();
		try {
			Response response = client.newCall(request).execute();
			if (response.isSuccessful()) {
				final String content = response.body().string();
				if (content.startsWith(ERROR_DIV)) {
					error = Html.fromHtml(content).toString().trim();
				}
				saveContent(content);
			} else {
				error = "Unsuccessful post: " + response.code();
			}
		} catch (IOException e) {
			exception = e;
		}
	}

	public boolean hasError() {
		return !TextUtils.isEmpty(getErrorMessage());
	}

	public boolean hasAuthError() {
		return error != null && error.contains(AUTH_ERROR_TEXT);
	}

	public String getErrorMessage() {
		if (exception != null) {
			return getExceptionMessage();
		}
		return error;
	}

	protected abstract RequestBody createForm(CollectionItem collectionItem);

	protected void saveContent(String content) {
	}

	public abstract void appendContentValues(ContentValues contentValues);

	@NonNull
	protected FormBody.Builder createFormBuilder() {
		return new FormBody.Builder()
			.add("ajax", "1")
			.add("objecttype", "thing")
			.add("objectid", String.valueOf(collectionItem.getGameId()));
	}

	private String getExceptionMessage() {
		String message = exception.getMessage();
		if (!TextUtils.isEmpty(message)) {
			return message;
		}
		return exception.toString();
	}
}
