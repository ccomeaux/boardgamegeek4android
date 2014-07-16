package com.boardgamegeek.model;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import android.text.TextUtils;

import com.boardgamegeek.util.StringUtils;

@Root(name = "item")
public class Game {
	@Root(name = "name")
	static class Name {
		@Attribute
		private String type;

		@Attribute
		private int sortindex;

		@Attribute
		private String value;
	}

	@Root(name = "poll")
	public static class Poll {
		@Attribute
		public String name;

		@Attribute
		public String title;

		@Attribute
		public int totalvotes;

		@ElementList(required = false, inline = true, empty = false)
		public List<Results> results;
	}

	@Root(name = "results")
	public static class Results {
		@Attribute(required = false)
		private String numplayers;

		@ElementList(required = false, inline = true, empty = false)
		public List<Result> result;

		public String getKey() {
			if (TextUtils.isEmpty(numplayers)) {
				return "X";
			}
			return numplayers;
		}
	}

	@Root(name = "result")
	public static class Result {
		@Attribute(required = false)
		public int level;

		@Attribute
		public String value;

		@Attribute
		public int numvotes;
	}

	public static class Link {
		@Attribute
		private String type;

		@Attribute
		public int id;

		@Attribute
		public String value;

		@Attribute(required = false)
		private String inbound;

		public boolean getInbound() {
			return "true".equals(inbound);
		}
	}

	@Root(name = "rank")
	public static class Rank {
		@Attribute
		public String type;

		@Attribute
		public int id;

		@Attribute
		public String name;

		@Attribute(name = "friendlyname")
		public String friendlyName;

		@Attribute
		private String value;

		@Attribute
		private String bayesaverage;

		public int getValue() {
			return StringUtils.parseInt(value);
		}

		public double getBayesAverage() {
			return StringUtils.parseDouble(bayesaverage);
		}
	}

	public static class Statistics {
		@Attribute
		private int page;

		@Path("ratings/usersrated")
		@Attribute(name = "value")
		public int usersRated;

		@Path("ratings/average")
		@Attribute(name = "value")
		public double average;

		@Path("ratings/bayesaverage")
		@Attribute(name = "value")
		public double bayesAverage;

		@Path("ratings")
		@ElementList
		public List<Rank> ranks;

		@Path("ratings/stddev")
		@Attribute(name = "value")
		public double standardDeviation;

		@Path("ratings/median")
		@Attribute(name = "value")
		public double median;

		@Path("ratings/owned")
		@Attribute(name = "value")
		public int owned;

		@Path("ratings/trading")
		@Attribute(name = "value")
		public int trading;

		@Path("ratings/wanting")
		@Attribute(name = "value")
		public int wanting;

		@Path("ratings/wishing")
		@Attribute(name = "value")
		public int wishing;

		@Path("ratings/numcomments")
		@Attribute(name = "value")
		public int commenting;

		@Path("ratings/numweights")
		@Attribute(name = "value")
		public int weighting;

		@Path("ratings/averageweight")
		@Attribute(name = "value")
		public double averageWeight;
	}

	@Root(name = "comment")
	public static class Comment {
		@Attribute
		public String username;

		@Attribute
		private String rating;

		public double getRating() {
			return StringUtils.parseDouble(rating, 0.0);
		}

		@Attribute
		public String value;
	}

	@Attribute
	private String type;

	public String subtype() {
		return type;
	}

	@Attribute
	public int id;

	@Element(required = false)
	public String thumbnail;

	@Element(required = false)
	public String image;

	@ElementList(inline = true)
	private List<Name> names;

	@Element
	private String description;

	@Path("yearpublished")
	@Attribute(name = "value")
	public int yearPublished;

	@Path("minplayers")
	@Attribute(name = "value")
	public int minPlayers;

	@Path("maxplayers")
	@Attribute(name = "value")
	public int maxPlayers;

	@ElementList(inline = true)
	public List<Poll> polls;

	@Path("playingtime")
	@Attribute(name = "value")
	public int playingTime;

	@Path("minage")
	@Attribute(name = "value")
	public int minAge;

	@ElementList(inline = true)
	private List<Link> links;

	@Element(name = "statistics", required = false)
	public Statistics statistics;

	@Element(required = false)
	public Comments comments;

	public static class Comments {
		@Attribute
		public int page;

		@Attribute
		public int totalitems;

		@ElementList(inline = true)
		public List<Comment> comments;
	}

	public String getName() {
		for (Name name : names) {
			if ("primary".equals(name.type)) {
				return name.value;
			}
		}
		return "";
	}

	public String getSortName() {
		for (Name name : names) {
			if ("primary".equals(name.type)) {
				return StringUtils.createSortName(name.value, name.sortindex);
			}
		}
		return "";
	}

	public String getDescription() {
		String d = description.replace("&#10;", "\n");
		return d.trim();
	}

	public List<Link> getDesigners() {
		return getLinks("boardgamedesigner");
	}

	public List<Link> getArtists() {
		return getLinks("boardgameartist");
	}

	public List<Link> getPublishers() {
		return getLinks("boardgamepublisher");
	}

	public List<Link> getCategories() {
		return getLinks("boardgamecategory");
	}

	public List<Link> getMechanics() {
		return getLinks("boardgamemechanic");
	}

	public List<Link> getExpansions() {
		return getLinks("boardgameexpansion");
	}

	public List<Link> getFamilies() {
		return getLinks("boardgamefamily");
	}

	public List<Link> getImplementations() {
		return getLinks("boardgameimplementation");
	}

	public Rank getRank() {
		return null;
	}

	private List<Link> getLinks(String type) {
		List<Link> list = new ArrayList<Link>();
		if (!TextUtils.isEmpty(type)) {
			for (Link link : links) {
				if (type.equals(link.type)) {
					list.add(link);
				}
			}
		}
		return list;
	}

	@Override
	public String toString() {
		return id + ": " + getName();
	}
}