package com.boardgamegeek.io;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.model.CollectionRatingPostResponse;
import com.boardgamegeek.util.StringUtils;

import java.lang.reflect.Type;

public class CollectionRatingConverter extends PostConverter {
	private static final String CLASS_NAME = "class com.boardgamegeek.model.CollectionRatingPostResponse";

	public CollectionRatingConverter() {
	}

	@NonNull
	@Override
	protected Object convertContent(String content) {
		String errorMessage = extractErrorMessage(content);
		if (!TextUtils.isEmpty(errorMessage)) {
			return new CollectionRatingPostResponse(errorMessage);
		}
		return new CollectionRatingPostResponse(extractRating(content));
	}

	@Override
	protected boolean typeIsExpected(Type type) {
		return CLASS_NAME.equals(type.toString());
	}

	private static final String RATING_DIV = "<div class='ratingtext'>";
	private static final String N_A_SPAN = "<span>N/A</span>";

	private double extractRating(String content) {
		if (content.contains(N_A_SPAN)) {
			return CollectionRatingPostResponse.INVALID_RATING;
		}
		if (content.contains(RATING_DIV)) {
			int index = content.indexOf(RATING_DIV) + RATING_DIV.length();
			String message = content.substring(index);
			index = message.indexOf("<");
			if (index > 0) {
				message = message.substring(0, index);
			}
			return StringUtils.parseDouble(message.trim(), CollectionRatingPostResponse.INVALID_RATING);
		}
		return CollectionRatingPostResponse.INVALID_RATING;
	}
}

// value for rating is between 1 - 10
// supports 5 digits to the right of the decimal
// entering from 0.00001 - 0.99999 rounds up to 1
// all non-valid ratings result in clearing the rating

// SUCCESS - ratingtext is the interpreted rating
//<div style='background:#ff66cc;' class='rating'>
//<div class='ratingtext'>4.4</div>
//</div>
//<div class='sf darkgray' >Aug&nbsp;2015</div>

// INVALID RATING, INVALID COLLID
//<div style='background:white;' class='rating'>
//<span>N/A</span>
//</div>

// UNAUTHORIZED
//<div class='messagebox error'>
//	You must
//<a href="/login">login</a> to use the collection utilities.
//
//</div>
