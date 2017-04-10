package com.boardgamegeek.io;

public interface BackOff {
	long STOP = -1L;

	long nextBackOffMillis();

	void reset();
}
