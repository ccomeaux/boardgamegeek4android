package com.boardgamegeek.model;

import android.text.TextUtils;

import com.boardgamegeek.io.AuthException;
import com.boardgamegeek.io.InvalidIdException;
import com.boardgamegeek.io.PossibleSuccessException;
import com.boardgamegeek.util.StringUtils;

import retrofit.RetrofitError;
import retrofit.converter.ConversionException;

public class PlayPostResponse {
	private String html;

	private Exception mException;

	public PlayPostResponse(Exception e) {
		mException = e;
	}

	public boolean hasError() {
		return getErrorMessage() != null;
	}

	public boolean hasAuthError() {
		if (mException != null) {
			if (mException instanceof RetrofitError && mException.getCause() instanceof ConversionException
				&& mException.getCause().getCause() instanceof AuthException) {
				return true;
			}
		}
		return false;
	}

	public boolean hasInvalidIdError() {
		if (mException != null) {
			if (mException instanceof RetrofitError && mException.getCause() instanceof ConversionException
				&& mException.getCause().getCause() instanceof InvalidIdException) {
				return true;
			}
		}
		return false;
	}

	public boolean hasPossibleSuccessError() {
		if (mException != null) {
			if (mException instanceof RetrofitError && mException.getCause() instanceof ConversionException
				&& mException.getCause().getCause() instanceof PossibleSuccessException) {
				return true;
			}
		}
		return false;
	}

	public String getErrorMessage() {
		if (mException != null) {
			if (hasPossibleSuccessError()) {
				return null;
			}
			return mException.getMessage();
		}
		if (TextUtils.isEmpty(html)) {
			return "Missing response";
		}
		if (html.startsWith("Plays: <a") || html.startsWith("{\"html\":\"Plays:")) {
			return null;
		} else {
			return "Bad response:\n" + html;
		}
	}

	public int getPlayCount() {
		if (hasError()) {
			return 0;
		} else {
			int start = html.indexOf(">");
			int end = html.indexOf("<", start);
			return StringUtils.parseInt(html.substring(start + 1, end), 1);
		}
	}
}
