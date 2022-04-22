package com.boardgamegeek.io.model;

import org.simpleframework.xml.ElementList;

import java.util.List;

public class PersonResponse {
	@ElementList(inline = true, required = false) public List<PersonItem> items;
}
