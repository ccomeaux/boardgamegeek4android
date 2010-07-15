package com.boardgamegeek.model;

public class Publisher extends DescriptiveEntity {
	public Publisher(int id, String name) {
		super(id, name);
	}

	public Publisher(int id, String name, String description) {
		super(id, name, description);
	}
}
