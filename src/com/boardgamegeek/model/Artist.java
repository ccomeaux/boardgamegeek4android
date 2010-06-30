package com.boardgamegeek.model;

public class Artist extends DescriptiveEntity {

	public Artist(int id, String name) {
		super(id, name);
	}

	public Artist(int id, String name, String description) {
		super(id, name, description);
	}

}
