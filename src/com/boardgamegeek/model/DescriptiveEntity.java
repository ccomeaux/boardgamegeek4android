package com.boardgamegeek.model;

public abstract class DescriptiveEntity extends Entity {

	public final String Description;

	protected DescriptiveEntity(int id, String name) {
		super(id, name);
		Description = "";
	}

	protected DescriptiveEntity(int id, String name, String description) {
		super(id, name);
		Description = description;
	}
}
