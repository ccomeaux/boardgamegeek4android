package com.boardgamegeek.io;

public class RetryableException extends Exception {
	private static final long serialVersionUID = 1L;

	public RetryableException(String message) {
		super(message);
	}

	public RetryableException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public RetryableException(Throwable throwable) {
		super(throwable);
	}
}
