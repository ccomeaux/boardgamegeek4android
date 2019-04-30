package com.boardgamegeek.io.model;

import org.simpleframework.xml.ElementList;

import java.util.List;

public class CompanyResponse2 {
	@ElementList(inline = true, required = false) public List<CompanyItem> items;
}
