package com.boardgamegeek.model;

import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.StringUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

@Root(name = "item")
public class GeekListItem {
	public static final int INVALID_OBJECT_TYPE_RES_ID = 0;
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

	private long postDateTime = DateTimeUtils.UNPARSED_DATE;
	private long editDateTime = DateTimeUtils.UNPARSED_DATE;

	@Attribute private String id;
	@Attribute private String imageid;
	@Attribute private String objectid;
	@Attribute private String objectname;
	@Attribute private String objecttype;
	@Attribute private String postdate;
	@Attribute private String editdate;
	@Attribute private String subtype;
	@Attribute private String thumbs;
	@Attribute private String username;
	@Element(required = false) private String body;
	@ElementList(name = "comment", inline = true, required = false) private ArrayList<GeekListComment> comments;

	public int imageId() {
		return Integer.valueOf(imageid);
	}

	public int getObjectId() {
		return StringUtils.parseInt(objectid, BggContract.INVALID_ID);
	}

	public String getObjectName() {
		return objectname;
	}

	public int getObjectTypeResId() {
		if (objecttype == null) return INVALID_OBJECT_TYPE_RES_ID;

		switch (objecttype) {
			case "thing":
				if ("boardgame".equals(subtype)) {
					return R.string.title_board_game;
				} else if ("boardgameaccessory".equals(subtype)) {
					return R.string.title_board_game_accessory;
				}
				return R.string.title_thing;
			case "company":
				if ("boardgamepublisher".equals(subtype)) {
					return R.string.title_board_game_publisher;
				}
				return R.string.title_company;
			case "person":
				if ("boardgamedesigner".equals(subtype)) {
					return R.string.title_board_game_designer;
				}
				return R.string.title_person;
			case "family":
				if ("boardgamefamily".equals(subtype)) {
					return R.string.title_board_game_family;
				}
				return R.string.title_family;
			case "filepage":
				return R.string.title_file;
			case "geeklist":
				return R.string.title_geeklist;
		}
		return INVALID_OBJECT_TYPE_RES_ID;
	}

	public boolean isBoardGame() {
		return "thing".equals(objecttype);
	}

	public String getObjectUrl() {
		return "https://www.boardgamegeek.com/" + (TextUtils.isEmpty(subtype) ? objecttype : subtype) + "/" + objectid;
	}

	public long getPostDate() {
		postDateTime = DateTimeUtils.tryParseDate(postDateTime, postdate, FORMAT);
		return postDateTime;
	}

	public long getEditDate() {
		editDateTime = DateTimeUtils.tryParseDate(editDateTime, editdate, FORMAT);
		return editDateTime;
	}

	public int getThumbCount() {
		return StringUtils.parseInt(thumbs, 0);
	}

	public ArrayList<GeekListComment> getComments() {
		return comments;
	}

	public String getUsername() {
		return username;
	}

	public String getBody() {
		return body;
	}
}
