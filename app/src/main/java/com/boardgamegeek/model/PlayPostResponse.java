package com.boardgamegeek.model;

import android.text.TextUtils;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.StringUtils;

public class PlayPostResponse {
	@SuppressWarnings("unused") private String html;
	@SuppressWarnings("unused") private String numplays;
	@SuppressWarnings("unused") private String playid;
	@SuppressWarnings("unused") private String error;

	private Exception mException;

	public PlayPostResponse(String errorMessage) {
		this.error = errorMessage;
	}

	public PlayPostResponse(Exception e) {
		mException = e;
	}

	public boolean hasError() {
		return !TextUtils.isEmpty(getErrorMessage());
	}

	/**
	 * Indicates the user attempted to modify a play without being authenticated.
	 */
	public boolean hasAuthError() {
		return "You must login to save plays".equals(error);
	}

	/**
	 * Indicates the user attempted to modify a play that doesn't exist.
	 */
	public boolean hasInvalidIdError() {
		if ("You are not permitted to edit this play.".equals(error)) {
			return true;
		} else if ("Play does not exist.".equals(error)) {
			return true;
		}
		return false;
	}

	public String getErrorMessage() {
		if (mException != null) {
			return mException.getMessage();
		}
		return error;
	}

	public int getPlayCount() {
		if (hasError()) {
			return 0;
		} else {
			return StringUtils.parseInt(numplays);
		}
	}

	public int getPlayId() {
		return StringUtils.parseInt(playid, BggContract.INVALID_ID);
	}
}
