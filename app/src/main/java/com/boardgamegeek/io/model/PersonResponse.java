package com.boardgamegeek.io.model;

import org.simpleframework.xml.ElementList;

import java.util.List;

/** @noinspection unused*/
public class PersonResponse {
	@ElementList(inline = true, required = false) public List<PersonItem> items;
}
