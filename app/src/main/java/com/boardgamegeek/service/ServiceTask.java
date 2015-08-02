package com.boardgamegeek.service;

public class ServiceTask {
	public static final int NO_NOTIFICATION = 0;

	public ServiceTask() {
		super();
	}

	protected int getNotification() {
		return NO_NOTIFICATION;
	}
}