package com.boardgamegeek.ui.loader;

import android.text.TextUtils;

import java.util.List;

import retrofit.RetrofitError;

public abstract class Data<T> {
	private String errorMessage;

	public Data() {
	}

	public Data(Exception e) {
		if (e instanceof RetrofitError) {
			RetrofitError re = (RetrofitError) e;
			if (re.getKind() == RetrofitError.Kind.NETWORK && re.getResponse() == null) {
				errorMessage = getOfflineMessage();
			} else {
				errorMessage = re.getMessage();
			}
		} else {
			errorMessage = e.getMessage();
		}
	}

	protected String getOfflineMessage() {
		//TODO: externalize
		return "Looks like you're offline.";
	}

	protected abstract List<T> list();

	public boolean hasError() {
		return !TextUtils.isEmpty(errorMessage);
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
