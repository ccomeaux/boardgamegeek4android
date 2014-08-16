package com.boardgamegeek.io;

public class InvalidIdException extends Exception {
	private static final long serialVersionUID = 1L;

	public InvalidIdException(String message) {
		super(message);
	}

	public InvalidIdException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public InvalidIdException(Throwable throwable) {
		super(throwable);
	}
}
