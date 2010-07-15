package com.boardgamegeek.model;

public abstract class DescriptiveEntity extends Entity {

	public String Description;

	public DescriptiveEntity(int id, String name) {
		super(id, name);
	}

	public DescriptiveEntity(int id, String name, String description) {
		super(id, name);
		Description = description;
	}
}
