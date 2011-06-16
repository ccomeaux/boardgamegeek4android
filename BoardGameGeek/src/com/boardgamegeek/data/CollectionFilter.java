package com.boardgamegeek.data;


public class CollectionFilter {

	private String displayText;
	private String selection;
	private String[] selectionArgs;
	private int id;
	
	public String getSelection() {
		return selection;
	}
	public String[] getSelectionArgs() {
		return selectionArgs;
	}
	public String getDisplayText() {
		return displayText;
	}
	public int getId() {
		return id;
	}
	
	public CollectionFilter name(String name) {
		this.displayText = name;
		return this;
	}
	public CollectionFilter selection(String selection) {
		this.selection = selection;
		return this;
	}
	public CollectionFilter selectionargs(String... selectionArgs) {
		this.selectionArgs = selectionArgs;
		return this;
	}		
	public CollectionFilter id(int id) {
		this.id = id;
		return this;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null || !(o instanceof CollectionFilter)) 
			return false;

		CollectionFilter other = (CollectionFilter)o;
		
		return other.getId() == this.getId();
	}
	
	@Override
	public int hashCode() {
		return this.getId();
	}
}
