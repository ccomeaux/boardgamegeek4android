package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import android.text.TextUtils;

import com.boardgamegeek.util.StringUtils;

@Root(name = "item")
public class SearchResult {
	public static final int NAME_TYPE_PRIMARY = 0;
	public static final int NAME_TYPE_ALTERNATE = 1;
	public static final int NAME_TYPE_UNKNOWN = -1;

	/**
	 * Includes boardgame, boardgameexpansion, videogame, rpg, rpgitem
	 */
	@Attribute private String type;

	@Attribute public int id;

	@Path("name") @Attribute(name = "value") public String name;

	@Path("name") @Attribute(name = "type") private String nameType;

	@Path("yearpublished") @Attribute(name = "value") private String yearPublished;

	public int getYearPublished() {
		if (TextUtils.isEmpty(yearPublished)) {
			return 0;
		}
		long l = Long.parseLong(yearPublished);
		int year = 0;
		if (l > Integer.MAX_VALUE) {
			try {
				year = (int) (l - Long.MAX_VALUE) - 1;
			} catch (Exception e) {
				year = 0;
			}
		} else {
			year = StringUtils.parseInt(yearPublished);
		}
		return year;
	}

	public int getNameType() {
		if ("primary".equals(nameType)) {
			return NAME_TYPE_PRIMARY;
		} else if ("alternate".equals(nameType)) {
			return NAME_TYPE_ALTERNATE;
		}
		return NAME_TYPE_UNKNOWN;
	}
}
