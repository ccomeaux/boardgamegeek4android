package com.boardgamegeek.model;

import android.text.TextUtils;

import com.boardgamegeek.util.StringUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Root(name = "item")
public class Game {
	@Root(name = "name")
	static class Name {
		@Attribute private String type;

		@Attribute private int sortindex;

		@Attribute private String value;
	}

	@Root(name = "poll")
	public static class Poll {
		@Attribute public String name;

		@Attribute public String title;

		@Attribute public int totalvotes;

		@ElementList(required = false, inline = true, empty = false) public List<Results> results;
	}

	@Root(name = "results")
	public static class Results {
		@Attribute(required = false) private String numplayers;

		@ElementList(required = false, inline = true, empty = false) public List<Result> result;

		public String getKey() {
			if (TextUtils.isEmpty(numplayers)) {
				return "X";
			}
			return numplayers;
		}
	}

	@Root(name = "result")
	public static class Result {
		@Attribute(required = false) public int level;

		@Attribute public String value;

		@Attribute public int numvotes;
	}

	public static class Link {
		@Attribute private String type;

		@Attribute public int id;

		@Attribute public String value;

		@Attribute(required = false) private String inbound;

		public boolean getInbound() {
			return "true".equals(inbound);
		}
	}

	@Root(name = "rank")
	public static class Rank {
		@Attribute public String type;

		@Attribute public int id;

		@Attribute public String name;

		@Attribute(name = "friendlyname") public String friendlyName;

		@Attribute private String value;

		@Attribute private String bayesaverage;

		public int getValue() {
			return StringUtils.parseInt(value, Integer.MAX_VALUE);
		}

		public double getBayesAverage() {
			return StringUtils.parseDouble(bayesaverage);
		}
	}

	public static class Statistics {
		@Attribute private int page;

		@Path("ratings/usersrated") @Attribute(name = "value") private String usersRated;

		public int usersRated() {
			return StringUtils.parseInt(usersRated);
		}

		@Path("ratings/average") @Attribute(name = "value") private String average;

		public double average() {
			return StringUtils.parseDouble(average);
		}

		@Path("ratings/bayesaverage") @Attribute(name = "value") private String bayesAverage;

		public double bayesAverage() {
			return StringUtils.parseDouble(bayesAverage);
		}

		@Path("ratings") @ElementList public List<Rank> ranks;

		@Path("ratings/stddev") @Attribute(name = "value") private String standardDeviation;

		public double standardDeviation() {
			return StringUtils.parseDouble(standardDeviation);
		}

		@Path("ratings/median") @Attribute(name = "value") private String median;

		public double median() {
			return StringUtils.parseDouble(median);
		}

		@Path("ratings/owned") @Attribute(name = "value") private String owned;

		public int owned() {
			return StringUtils.parseInt(owned);
		}

		@Path("ratings/trading") @Attribute(name = "value") private String trading;

		public int trading() {
			return StringUtils.parseInt(trading);
		}

		@Path("ratings/wanting") @Attribute(name = "value") private String wanting;

		public int wanting() {
			return StringUtils.parseInt(wanting);
		}

		@Path("ratings/wishing") @Attribute(name = "value") private String wishing;

		public int wishing() {
			return StringUtils.parseInt(wishing);
		}

		@Path("ratings/numcomments") @Attribute(name = "value") private String commenting;

		public int commenting() {
			return StringUtils.parseInt(commenting);
		}

		@Path("ratings/numweights") @Attribute(name = "value") private String weighting;

		public int weighting() {
			return StringUtils.parseInt(weighting);
		}

		@Path("ratings/averageweight") @Attribute(name = "value") private String averageWeight;

		public double averageWeight() {
			return StringUtils.parseDouble(averageWeight);
		}
	}

	@Root(name = "comment")
	public static class Comment {
		private static final DecimalFormat RATING_FORMAT = new DecimalFormat("#0.00");

		@Attribute public String username;

		@Attribute private String rating;

		public double getRating() {
			return StringUtils.parseDouble(rating, 0.0);
		}

		public String getRatingText() {
			double rating = getRating();
			if (rating < 1.0) {
				return "N/A";
			}
			return RATING_FORMAT.format(rating);
		}

		@Attribute public String value;
	}

	@Attribute private String type;

	public String subtype() {
		return type;
	}

	@Attribute public int id;

	@Element(required = false) public String thumbnail;

	@Element(required = false) public String image;

	@ElementList(inline = true) private List<Name> names;

	@Element(required = false) private String description;

	@Path("yearpublished") @Attribute(name = "value") private String yearpublished;

	public int getYearPublished() {
		return StringUtils.parseInt(yearpublished, Constants.YEAR_UNKNOWN);
	}

	@Path("minplayers") @Attribute(name = "value") private String minplayers;

	public int getMinPlayers() {
		return StringUtils.parseInt(minplayers, 0);
	}

	@Path("maxplayers") @Attribute(name = "value") private String maxplayers;

	public int getMaxPlayers() {
		return StringUtils.parseInt(maxplayers, 0);
	}

	@ElementList(inline = true, required = false) public List<Poll> polls;

	@Path("playingtime") @Attribute(name = "value") private String playingtime;

	public int getPlayingTime() {
		return StringUtils.parseInt(playingtime, 0);
	}

	@Path("minage") @Attribute(name = "value") private String minage;

	public int getMinAge() {
		return StringUtils.parseInt(minage, 0);
	}

	@ElementList(inline = true, required = false) private List<Link> links;

	@Element(name = "statistics", required = false) public Statistics statistics;

	@Element(required = false) public Comments comments;

	public static class Comments {
		@Attribute public int page;

		@Attribute public int totalitems;

		@ElementList(inline = true) public List<Comment> comments;
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
		if (TextUtils.isEmpty(description)) {
			return "";
		}
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

	public int getRank() {
		for (Rank rank : statistics.ranks) {
			if ("subtype".equals(rank.type)) {
				return rank.getValue();
			}
		}
		return Integer.MAX_VALUE;
	}

	private List<Link> getLinks(String type) {
		List<Link> list = new ArrayList<>();
		if (!TextUtils.isEmpty(type) && links != null && links.size() > 0) {
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