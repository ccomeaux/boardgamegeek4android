package com.boardgamegeek.model;

public class CollectionCommentPostResponse extends CollectionPostResponse {
	private String comment;

	public CollectionCommentPostResponse(String comment) {
		this.comment = comment;
	}

	public CollectionCommentPostResponse(Exception e) {
		exception = e;
	}

	public String getComment() {
		return this.comment;
	}
}
