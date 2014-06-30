package com.boardgamegeek.io;

public class PossibleSuccessException extends Exception {
	private static final long serialVersionUID = 1L;

	public PossibleSuccessException(String message) {
		super(message);
	}

	public PossibleSuccessException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public PossibleSuccessException(Throwable throwable) {
		super(throwable);
	}
}
