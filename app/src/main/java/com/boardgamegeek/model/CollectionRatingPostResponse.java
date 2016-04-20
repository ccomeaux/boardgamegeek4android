package com.boardgamegeek.model;

import com.boardgamegeek.util.StringUtils;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class CollectionRatingPostResponse extends CollectionPostResponse {
	private static final String N_A_SPAN = "<span>N/A</span>";
	private static final String RATING_DIV = "<div class='ratingtext'>";
	public static final double INVALID_RATING = -1.0;
	private double rating;

	public CollectionRatingPostResponse(OkHttpClient client, Request request) {
		super(client, request);
	}

	@Override
	protected void saveContent(String content) {
		if (content.contains(N_A_SPAN)) {
			rating = CollectionRatingPostResponse.INVALID_RATING;
		} else if (content.contains(RATING_DIV)) {
			int index = content.indexOf(RATING_DIV) + RATING_DIV.length();
			String message = content.substring(index);
			index = message.indexOf("<");
			if (index > 0) {
				message = message.substring(0, index);
			}
			rating = StringUtils.parseDouble(message.trim(), CollectionRatingPostResponse.INVALID_RATING);
		} else {
			rating = CollectionRatingPostResponse.INVALID_RATING;
		}
	}

	public double getRating() {
		return this.rating;
	}
}
