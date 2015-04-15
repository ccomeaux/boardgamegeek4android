package com.boardgamegeek.ui.loader;

import android.text.TextUtils;

import java.util.List;

import retrofit.RetrofitError;

public abstract class Data<T> {
	private String mErrorMessage;

	public Data() {
	}

	public Data(Exception e) {
		if (e instanceof RetrofitError) {
			RetrofitError re = (RetrofitError) e;
			if (re.getKind() == RetrofitError.Kind.NETWORK && re.getResponse() == null) {
				mErrorMessage = getOfflineMessage();
			} else {
				mErrorMessage = re.getMessage();
			}
		} else {
			mErrorMessage = e.getMessage();
		}
	}

	protected String getOfflineMessage() {
		return "Looks like you're offline.";
	}

	protected abstract List<T> list();

	public boolean hasError() {
		return !TextUtils.isEmpty(mErrorMessage);
	}

	public String getErrorMessage() {
		return mErrorMessage;
	}
}
