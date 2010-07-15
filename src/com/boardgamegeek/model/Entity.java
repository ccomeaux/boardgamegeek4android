package com.boardgamegeek.model;

public abstract class Entity {
	public int Id;
	public String Name;

	public Entity(int id, String name) {
		Id = id;
		Name = name;
	}
}
