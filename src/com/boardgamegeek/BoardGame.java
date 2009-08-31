package com.boardgamegeek;

import java.util.Collection;
import java.util.HashMap;

import android.graphics.drawable.Drawable;

public class BoardGame {

	private String gameId = null;
	private String name = "";
	private int yearPublished = 0;
	private int minPlayers = 0;
	private int maxPlayers = 0;
	private int playingTime = 0;
	private int age = 0;
	private String description = "";
	private String thumbnailUrl = null;
	private Drawable thumbnail;
	private int ratingCount = 0;
	private double average = 0.0;
	private double bayesAverage;
	private int rank = 0;
	private double standardDeviation;
	private double median;
	private int ownedCount;
	private int tradingCount;
	private int wantingCount;
	private int wishingCount;
	private int commentCount;
	private int weightCount;
	private double averageWeight;
	private HashMap<String, String> designers = new HashMap<String, String>();
	private HashMap<String, String> artists = new HashMap<String, String>();
	private HashMap<String, String> publishers = new HashMap<String, String>();
	private HashMap<String, String> categories = new HashMap<String, String>();
	private HashMap<String, String> mechanics = new HashMap<String, String>();
	private HashMap<String, String> expansions = new HashMap<String, String>();

	// TODO: polls

	// game ID
	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	// game name
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNameForUrl() {
		return EncodeAsUrl(name);
	}

	// year published
	public int getYearPublished() {
		return yearPublished;
	}

	public void setYearPublished(int year) {
		this.yearPublished = year;
	}

	// min number of players
	public int getMinPlayers() {
		return minPlayers;
	}

	public void setMinPlayers(int minplayers) {
		this.minPlayers = minplayers;
	}

	// max number of players
	public int getMaxPlayers() {
		return maxPlayers;
	}

	public void setMaxPlayers(int maxplayers) {
		this.maxPlayers = maxplayers;
	}

	// number of players
	public String getPlayers() {
		if (minPlayers == 0 && maxPlayers == 0) {
			return "?";
		} else if (minPlayers >= maxPlayers) {
			return "" + minPlayers;
		} else {
			return "" + minPlayers + " - " + maxPlayers;
		}
	}

	// game playing time
	public int getPlayingTime() {
		return playingTime;
	}

	public void setPlayingTime(int playingTime) {
		this.playingTime = playingTime;
	}

	// player age (minimum)
	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	// number of ratings
	public int getRatingCount() {
		return ratingCount;
	}

	public void setRatingCount(int ratingCount) {
		this.ratingCount = ratingCount;
	}

	// average
	public double getAverage() {
		return average;
	}

	public void setAverage(double average) {
		this.average = average;
	}

	// bayes average
	public double getBayesAverage() {
		return bayesAverage;
	}

	public void setBayesAverage(double bayesAverage) {
		this.bayesAverage = bayesAverage;
	}

	// game rank
	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	// thumbnail image
	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public Drawable getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(Drawable thumbnail) {
		this.thumbnail = thumbnail;
	}

	// game description
	public String getDescription() {
		return Utility.unescapeText(description);
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getGameInfo() {
		StringBuilder info = new StringBuilder();
		if (gameId == null) {
			info.append("Game Not Found");
		} else {
			if (yearPublished != 0) {
				info.append("Year Published: ").append(yearPublished).append(
						"\n");
			}
			info.append("Players: ").append(getPlayers()).append("\n");
			if (playingTime != 0) {
				info.append("Playing Time: ").append(playingTime).append(
						" minutes\n");
			}
			if (age != 0) {
				info.append("Ages: ").append(age).append(" and up\n");
			}
			info.append("Game ID: ").append(gameId);
		}
		return info.toString();
	}

	public String toString() {
		if (yearPublished != 0) {
			return name + " (" + yearPublished + ")";
		} else {
			return name + " (ID# " + gameId + ")";
		}
	}

	public void setStandardDeviation(double standardDeviation) {
		this.standardDeviation = standardDeviation;
	}

	public double getStandardDeviation() {
		return standardDeviation;
	}

	public void setMedian(double median) {
		this.median = median;
	}

	public double getMedian() {
		return median;
	}

	public void setOwnedCount(int ownedCount) {
		this.ownedCount = ownedCount;
	}

	public int getOwnedCount() {
		return ownedCount;
	}

	public void setTradingCount(int tradingCount) {
		this.tradingCount = tradingCount;
	}

	public int getTradingCount() {
		return tradingCount;
	}

	public void setWantingCount(int wantingCount) {
		this.wantingCount = wantingCount;
	}

	public int getWantingCount() {
		return wantingCount;
	}

	public void setWishingCount(int wishingCount) {
		this.wishingCount = wishingCount;
	}

	public int getWishingCount() {
		return wishingCount;
	}

	public void setCommentCount(int commentCount) {
		this.commentCount = commentCount;
	}

	public int getCommentCount() {
		return commentCount;
	}

	public void setWeightCount(int weightCount) {
		this.weightCount = weightCount;
	}

	public int getWeightCount() {
		return weightCount;
	}

	public void setAverageWeight(double averageWeight) {
		this.averageWeight = averageWeight;
	}

	public double getAverageWeight() {
		return averageWeight;
	}

	public void addDesigner(String id, String name) {
		designers.put(id, name);
	}

	public Collection<String> getDesignerNames() {
		return designers.values();
	}

	public String getDesignerId(int position) {
		if (designers.size() > position) {
			return (String) designers.keySet().toArray()[position];
		} else {
			return null;
		}
	}

	public void addArtist(String id, String name) {
		artists.put(id, name);
	}

	public Collection<String> getArtistNames() {
		return artists.values();
	}

	public String getArtistId(int position) {
		if (artists.size() > position) {
			return (String) artists.keySet().toArray()[position];
		} else {
			return null;
		}
	}

	public void addPublisher(String id, String name) {
		publishers.put(id, name);
	}

	public Collection<String> getPublisherNames() {
		return publishers.values();
	}

	public String getPublisherId(int position) {
		if (publishers.size() > position) {
			return (String) publishers.keySet().toArray()[position];
		} else {
			return null;
		}
	}

	public void addCategory(String id, String name) {
		categories.put(id, name);
	}

	public Collection<String> getCategoryNames() {
		return categories.values();
	}

	public void addMechanic(String id, String name) {
		mechanics.put(id, name);
	}

	public Collection<String> getMechanicNames() {
		return mechanics.values();
	}

	public void addExpansion(String id, String name) {
		expansions.put(id, name);
	}

	public Collection<String> getExpansionNames() {
		return expansions.values();
	}

	public String getExpansionId(int position) {
		if (expansions.size() > position) {
			return (String) expansions.keySet().toArray()[position];
		} else {
			return null;
		}
	}

	public static String EncodeAsUrl(String s) {
		// converts any accented characters into standard equivalents
		// and replaces spaces with +

		if (s == null) {
			return null;
		}

		final String PLAIN_ASCII = "AaEeIiOoUu" // grave
				+ "AaEeIiOoUuYy" // acute
				+ "AaEeIiOoUuYy" // circumflex
				+ "AaOoNn" // tilde
				+ "AaEeIiOoUuYy" // umlaut
				+ "Aa" // ring
				+ "Cc" // cedilla
				+ "OoUu" // double acute
				+ "+" // space
		;

		final String UNICODE = "\u00C0\u00E0\u00C8\u00E8\u00CC\u00EC\u00D2\u00F2\u00D9\u00F9"
				+ "\u00C1\u00E1\u00C9\u00E9\u00CD\u00ED\u00D3\u00F3\u00DA\u00FA\u00DD\u00FD"
				+ "\u00C2\u00E2\u00CA\u00EA\u00CE\u00EE\u00D4\u00F4\u00DB\u00FB\u0176\u0177"
				+ "\u00C3\u00E3\u00D5\u00F5\u00D1\u00F1"
				+ "\u00C4\u00E4\u00CB\u00EB\u00CF\u00EF\u00D6\u00F6\u00DC\u00FC\u0178\u00FF"
				+ "\u00C5\u00E5"
				+ "\u00C7\u00E7"
				+ "\u0150\u0151\u0170\u0171"
				+ " ";

		StringBuilder sb = new StringBuilder();
		int n = s.length();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			int pos = UNICODE.indexOf(c);
			if (pos > -1) {
				sb.append(PLAIN_ASCII.charAt(pos));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}