package com.boardgamegeek.model;

public abstract class Entity {
	
	public final int Id;
	public final String Name;

	protected Entity(int id, String name) {
		Id = id;
		Name = name;
	}

	@Override
	public String toString() {
		return String.format("{0} [{1}]", Name, Id);
	}
}
