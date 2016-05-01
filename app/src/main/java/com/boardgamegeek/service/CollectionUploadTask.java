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
import okhttp3.Response;

public abstract class CollectionUploadTask {
	private static final String GEEK_COLLECTION_URL = "https://www.boardgamegeek.com/geekcollection.php";
	private static final String ERROR_DIV = "<div class='messagebox error'>";
	private static final String AUTH_ERROR_TEXT = "You must login to use the collection utilities.";
	private final OkHttpClient client;
	protected CollectionItem collectionItem;
	protected String error;
	protected Exception exception;

	public CollectionUploadTask(OkHttpClient client) {
		this.client = client;
	}

	public abstract String getTimestampColumn();

	public void addCollectionItem(CollectionItem collectionItem) {
		this.collectionItem = collectionItem;
	}

	public CollectionUploadTask post() {
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
		return this;
	}

	public abstract void appendContentValues(ContentValues contentValues);

	public abstract boolean isDirty();

	public boolean hasError() {
		return !TextUtils.isEmpty(getErrorMessage());
	}

	public boolean hasAuthError() {
		return AUTH_ERROR_TEXT.equals(error);
	}

	public String getErrorMessage() {
		if (exception != null) {
			return getExceptionMessage();
		}
		return error;
	}

	protected abstract FormBody createForm(CollectionItem collectionItem);

	protected abstract void saveContent(String content);

	@NonNull
	protected FormBody.Builder createFormBuilder() {
		return new FormBody.Builder()
			.add("B1", "Cancel")
			.add("ajax", "1")
			.add("action", "savedata")
			.add("objecttype", "thing")
			.add("objectid", String.valueOf(collectionItem.getGameId()))
			.add("collid", String.valueOf(collectionItem.getCollectionId()));
	}

	private String getExceptionMessage() {
		String message = exception.getMessage();
		if (!TextUtils.isEmpty(message)) {
			return message;
		}
		return exception.toString();
	}
}
