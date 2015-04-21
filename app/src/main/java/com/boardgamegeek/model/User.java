package com.boardgamegeek.model;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.StringUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;

import java.util.ArrayList;

public class User {
	@Attribute
	private String id;

	@Attribute
	public String name;

	@Path(value = "firstname")
	@Attribute(name = "value")
	public String firstName;

	@Path(value = "lastname")
	@Attribute(name = "value")
	public String lastName;

	@Path(value = "avatarlink")
	@Attribute(name = "value")
	public String avatarUrl;

	@Path(value = "yearregistered")
	@Attribute(name = "value")
	private String yearRegistered;

	@Path(value = "lastlogin")
	@Attribute(name = "value")
	private String lastLogin;

	@Path(value = "stateorprovince")
	@Attribute(name = "value")
	private String state;

	@Path(value = "country")
	@Attribute(name = "value")
	private String country;

	@Path(value = "webaddress")
	@Attribute(name = "value")
	private String webAddress;

	@Path(value = "xboxaccount")
	@Attribute(name = "value")
	private String xboxAccount;

	@Path(value = "wiiaccount")
	@Attribute(name = "value")
	private String wiiAccount;

	@Path(value = "psnaccount")
	@Attribute(name = "value")
	private String psnAccount;

	@Path(value = "battlenetaccount")
	@Attribute(name = "value")
	private String battlenetAccount;

	@Path(value = "steamaccount")
	@Attribute(name = "value")
	private String steamAccount;

	@Path(value = "traderating")
	@Attribute(name = "value")
	private String tradeRating;

	@Element(required = false)
	private Buddies buddies;

	public int getId() {
		return StringUtils.parseInt(id, BggContract.INVALID_ID);
	}

	public ArrayList<Buddy> getBuddies() {
		if (buddies == null || buddies.buddies == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(buddies.buddies);
	}
}
