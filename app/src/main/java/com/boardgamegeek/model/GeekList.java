package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Root(name = "geeklist")
public class GeekList {
	public static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

	@SuppressWarnings("unused")
	public GeekList() {
	}

	@Attribute public int id;
	@Element public String postdate;
	@Element public String editdate;
	@Element(name = "postdate_timestamp") private String postdateTimestamp;
	@Element(name = "editdate_timestamp") private String editdateTimestamp;
	@Element public String thumbs;
	@Element public String numitems;
	@Element public String username;
	@Element public String title;
	@Element public String description;
	@ElementList(name = "comment", inline = true, required = false) private ArrayList<GeekListComment> comments;
	@ElementList(name = "item", inline = true, required = false) private List<GeekListItem> items;

	private ArrayList<GeekListComment> getComments() {
		return comments;
	}

	public List<GeekListItem> getItems() {
		return items;
	}
}
