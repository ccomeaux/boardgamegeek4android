package com.boardgamegeek.io.model;

import org.simpleframework.xml.ElementList;

import java.util.List;

/** @noinspection unused*/
public class CompanyResponse {
	@ElementList(inline = true, required = false) public List<CompanyItem> items;
}
