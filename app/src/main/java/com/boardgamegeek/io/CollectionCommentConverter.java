package com.boardgamegeek.io;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.model.CollectionCommentPostResponse;

import java.lang.reflect.Type;

public class CollectionCommentConverter extends PostConverter {
	private static final String CLASS_NAME = "class com.boardgamegeek.model.CollectionCommentPostResponse";

	public CollectionCommentConverter() {
	}

	@NonNull
	@Override
	protected Object convertContent(String content) {
		String errorMessage = extractErrorMessage(content);
		if (!TextUtils.isEmpty(errorMessage)) {
			return new CollectionCommentPostResponse(new RuntimeException(errorMessage));
		}
		return new CollectionCommentPostResponse(extractComment(content));
	}

	@Override
	protected boolean typeIsExpected(Type type) {
		return CLASS_NAME.equals(type.toString());
	}

	private String extractComment(String content) {
		return content;
	}
}

// value for comment is a string (how long?)

// SUCCESS
//Comment text!

// INVALID COMMENT, INVALID COLLID
//

// UNAUTHORIZED
//<div class='messagebox error'>
//	You must
//<a href="/login">login</a> to use the collection utilities.
//
//</div>
