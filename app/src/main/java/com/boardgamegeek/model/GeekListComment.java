package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.text.SimpleDateFormat;
import java.util.Locale;

@Root(name = "comment")
public class GeekListComment {
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

	@SuppressWarnings("unused")
	public GeekListComment() {
	}

	@Attribute public String date;
	@Attribute public String editdate;
	@Attribute public String postdate;
	@Attribute public String thumbs;
	@Attribute public String username;
	@Text public String content;
}
