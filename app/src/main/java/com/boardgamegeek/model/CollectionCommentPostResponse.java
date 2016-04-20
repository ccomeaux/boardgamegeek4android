package com.boardgamegeek.model;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class CollectionCommentPostResponse extends CollectionPostResponse {
	private String comment;

	public CollectionCommentPostResponse(OkHttpClient client, Request request) {
		super(client, request);
	}

	protected void saveContent(String content) {
		comment = content;
	}

	public String getComment() {
		return this.comment;
	}
}
