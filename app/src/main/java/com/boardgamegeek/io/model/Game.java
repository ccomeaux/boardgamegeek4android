package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "item")
public class Game {
	public static final int PAGE_SIZE = 100;

	@Attribute public String type;
	@Attribute public int id;
	@Element(required = false) public String thumbnail;
	@Element(required = false) public String image;
	@Element(required = false) public String description;
	@Path("yearpublished") @Attribute(name = "value") public String yearpublished;
	@Path("minplayers") @Attribute(name = "value") public String minplayers;
	@Path("maxplayers") @Attribute(name = "value") public String maxplayers;
	@Path("playingtime") @Attribute(name = "value") public String playingtime;
	@Path("minplaytime") @Attribute(name = "value") public String minplaytime;
	@Path("maxplaytime") @Attribute(name = "value") public String maxplaytime;
	@Path("minage") @Attribute(name = "value") public String minage;

	@ElementList(inline = true, required = false) public List<Name> names;

	@Root(name = "name")
	public static class Name {
		@Attribute public String type;
		@Attribute public int sortindex;
		@Attribute public String value;
	}

	@ElementList(inline = true, required = false)
	public List<Poll> polls;

	@Root(name = "poll")
	public static class Poll {
		@Attribute public String name;
		@Attribute public String title;
		@Attribute public int totalvotes;
		@ElementList(required = false, inline = true, empty = false) public List<Results> results;
	}

	@Root(name = "results")
	public static class Results {
		@Attribute(required = false) public String numplayers;
		@ElementList(required = false, inline = true, empty = false) public List<Result> result;
	}

	@Root(name = "result")
	public static class Result {
		@Attribute(required = false) public int level;
		@Attribute public String value;
		@Attribute public int numvotes;
	}

	@ElementList(inline = true, required = false)
	public List<Link> links;

	public static class Link {
		@Attribute public String type;
		@Attribute public int id;
		@Attribute public String value;
		@Attribute(required = false) public String inbound;
	}

	@Element(name = "statistics", required = false)
	public Statistics statistics;

	public static class Statistics {
		@Attribute private int page;
		@Path("ratings/usersrated") @Attribute(name = "value") public String usersrated;
		@Path("ratings/average") @Attribute(name = "value") public String average;
		@Path("ratings/bayesaverage") @Attribute(name = "value") public String bayesaverage;
		@Path("ratings/stddev") @Attribute(name = "value") public String stddev;
		@Path("ratings/median") @Attribute(name = "value") public String median;
		@Path("ratings/owned") @Attribute(name = "value") public String owned;
		@Path("ratings/trading") @Attribute(name = "value") public String trading;
		@Path("ratings/wanting") @Attribute(name = "value") public String wanting;
		@Path("ratings/wishing") @Attribute(name = "value") public String wishing;
		@Path("ratings/numcomments") @Attribute(name = "value") public String numcomments;
		@Path("ratings/numweights") @Attribute(name = "value") public String numweights;
		@Path("ratings/averageweight") @Attribute(name = "value") public String averageweight;
		@Path("ratings") @ElementList public List<Rank> ranks;
	}

	@Root(name = "rank")
	public static class Rank {
		@Attribute public String type;
		@Attribute public int id;
		@Attribute public String name;
		@Attribute public String friendlyname;
		@Attribute public String value;
		@Attribute public String bayesaverage;
	}

	@Element(required = false)
	public Comments comments;

	public static class Comments {
		@Attribute public int page;
		@Attribute public int totalitems;
		@ElementList(inline = true) public List<Comment> comments;
	}

	@Root(name = "comment")
	public static class Comment {
		@Attribute public String username;
		@Attribute public String rating;
		@Attribute public String value;
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}
}