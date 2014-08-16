package com.boardgamegeek.io;

public class AuthException extends Exception {
	private static final long serialVersionUID = 1L;

	public AuthException(String message) {
		super(message);
	}

	public AuthException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public AuthException(Throwable throwable) {
		super(throwable);
	}
}
