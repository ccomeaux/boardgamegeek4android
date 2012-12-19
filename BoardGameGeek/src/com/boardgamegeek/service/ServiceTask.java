package com.boardgamegeek.service;

public class ServiceTask {

	public static final int NO_NOTIFICATION = 0;
	private boolean mIsBggDown;

	public ServiceTask() {
		super();
	}

	public int getNotification() {
		return NO_NOTIFICATION;
	}

	public boolean isBggDown() {
		return mIsBggDown;
	}

	protected void setIsBggDown(boolean value) {
		mIsBggDown = value;
	}

}