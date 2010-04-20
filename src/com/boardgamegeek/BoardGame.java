package com.boardgamegeek;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.boardgamegeek.BoardGameGeekData.*;

import android.database.Cursor;
import android.graphics.drawable.Drawable;

public class BoardGame {

	private int gameId = 0;
	private String name = "";
	private int sortIndex = 1;
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
	private int rankAbstract = 0;
	private int rankCcg = 0;
	private int rankKids = 0;
	private int rankWar = 0;
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
	private List<Poll> polls = new ArrayList<Poll>();

	public BoardGame() {}

	// game ID
	public int getGameId() {
		return gameId;
	}

	public void setGameId(int gameId) {
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
		return Utility.EncodeAsUrl(name);
	}

	public int getSortIndex() {
		return sortIndex;
	}

	public void setSortIndex(int sortIndex) {
		this.sortIndex = sortIndex;
	}

	public String getSortName() {
		if (sortIndex > 1) {
			return name.substring(sortIndex - 1) + ", " + name.substring(0, sortIndex - 1).trim();
		} else {
			return name;
		}
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

	public int getRankAbstract() {
		return rankAbstract;
	}

	public void setRankAbstract(int rank) {
		this.rankAbstract = rank;
	}

	public int getRankCcg() {
		return rankCcg;
	}

	public void setRankCcg(int rank) {
		this.rankCcg = rank;
	}

	public int getRankKids() {
		return rankKids;
	}

	public void setRankKids(int rank) {
		this.rankKids = rank;
	}

	public int getRankWar() {
		return rankWar;
	}

	public void setRankWar(int rank) {
		this.rankWar = rank;
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
		if (gameId == 0) {
			info.append("Game not found");
		} else {
			if (yearPublished != 0) {
				info.append("Year Published: ").append(yearPublished).append("\n");
			}
			info.append("Players: ").append(getPlayers()).append("\n");
			if (playingTime != 0) {
				info.append("Playing Time: ").append(playingTime).append(" minutes\n");
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

	public int getDesignerCount() {
		return designers.size();
	}

	public void addDesigner(String id, String name) {
		designers.put(id, name);
	}

	public Collection<String> getDesignerNames() {
		return designers.values();
	}

	public String getDesignerIdByPosition(int position) {
		if (designers.size() > position) {
			return (String) designers.keySet().toArray()[position];
		} else {
			return null;
		}
	}

	public String getDesignerNameById(String designerId) {
		return designers.get(designerId);
	}

	public HashMap<String, String> getDesigners() {
		return designers;
	}

	public void CreateDesigners(Cursor cursor) {
		while (cursor.moveToNext()) {
			designers.put(cursor.getString(cursor.getColumnIndex(BoardGameDesigners.DESIGNER_ID)), cursor
				.getString(cursor.getColumnIndex(BoardGameDesigners.DESIGNER_NAME)));
		}
	}

	public int getArtistCount() {
		return artists.size();
	}

	public void addArtist(String id, String name) {
		artists.put(id, name);
	}

	public Collection<String> getArtistNames() {
		return artists.values();
	}

	public String getArtistIdByPosition(int position) {
		if (artists.size() > position) {
			return (String) artists.keySet().toArray()[position];
		} else {
			return null;
		}
	}

	public String getArtistNameById(String artistId) {
		return artists.get(artistId);
	}

	public HashMap<String, String> getArtists() {
		return artists;
	}

	public void CreateArtists(Cursor cursor) {
		while (cursor.moveToNext()) {
			artists.put(cursor.getString(cursor.getColumnIndex(BoardGameArtists.ARTIST_ID)), cursor
				.getString(cursor.getColumnIndex(BoardGameArtists.ARTIST_NAME)));
		}
	}

	public int getPublisherCount() {
		return publishers.size();
	}

	public void addPublisher(String id, String name) {
		publishers.put(id, name);
	}

	public Collection<String> getPublisherNames() {
		return publishers.values();
	}

	public String getPublisherIdByPosition(int position) {
		if (publishers.size() > position) {
			return (String) publishers.keySet().toArray()[position];
		} else {
			return null;
		}
	}

	public String getPublisherNameById(String publisherId) {
		return publishers.get(publisherId);
	}

	public void CreatePublishers(Cursor cursor) {
		while (cursor.moveToNext()) {
			publishers.put(cursor.getString(cursor.getColumnIndex(BoardGamePublishers.PUBLISHER_ID)), cursor
				.getString(cursor.getColumnIndex(BoardGamePublishers.PUBLISHER_NAME)));
		}
	}

	public int getCategoryCount() {
		return categories.size();
	}

	public String getCategoryIdByPosition(int position) {
		if (categories.size() > position) {
			return (String) categories.keySet().toArray()[position];
		} else {
			return null;
		}
	}

	public String getCategoryNameById(String categoryId) {
		return categories.get(categoryId);
	}

	public void addCategory(String id, String name) {
		categories.put(id, name);
	}

	public Collection<String> getCategoryNames() {
		return categories.values();
	}

	public void CreateCategories(Cursor cursor) {
		while (cursor.moveToNext()) {
			categories.put(cursor.getString(cursor.getColumnIndex(BoardGameCategories.CATEGORY_ID)), cursor
				.getString(cursor.getColumnIndex(BoardGameCategories.CATEGORY_NAME)));
		}
	}

	public int getMechanicCount() {
		return mechanics.size();
	}

	public String getMechanicIdByPosition(int position) {
		if (mechanics.size() > position) {
			return (String) mechanics.keySet().toArray()[position];
		} else {
			return null;
		}
	}

	public String getMechanicNameById(String mechanicId) {
		return mechanics.get(mechanicId);
	}

	public void addMechanic(String id, String name) {
		mechanics.put(id, name);
	}

	public Collection<String> getMechanicNames() {
		return mechanics.values();
	}

	public void CreateMechanics(Cursor cursor) {
		while (cursor.moveToNext()) {
			mechanics.put(cursor.getString(cursor.getColumnIndex(BoardGameMechanics.MECHANIC_ID)), cursor
				.getString(cursor.getColumnIndex(BoardGameMechanics.MECHANIC_NAME)));
		}
	}

	public int getExpansionCount() {
		return expansions.size();
	}

	public String getExpansionIdByPosition(int position) {
		if (expansions.size() > position) {
			return (String) expansions.keySet().toArray()[position];
		} else {
			return null;
		}
	}

	public String getExpansionNameById(String expansionId) {
		return expansions.get(expansionId);
	}

	public void addExpansion(String id, String name) {
		expansions.put(id, name);
	}

	public Collection<String> getExpansionNames() {
		return expansions.values();
	}

	public void CreateExpanions(Cursor cursor) {
		expansions.clear();
		if (cursor == null) {
			return;
		}
		if (cursor.moveToFirst()) {
			do {
				expansions.put(cursor.getString(cursor.getColumnIndex(BoardGameExpansions.EXPANSION_ID)),
					cursor.getString(cursor.getColumnIndex(BoardGameExpansions.EXPANSION_NAME)));
			} while (cursor.moveToNext());
		}
	}

	public List<Poll> getPolls() {
		return polls;
	}

	public void addPoll(Poll poll) {
		polls.add(poll);
	}

	public int getPollCount() {
		return polls.size();
	}

	public Poll getPollByPosition(int position) {
		if (polls.size() > position) {
			return polls.get(position);
		} else {
			return null;
		}
	}

	public void createPolls(Cursor cursor) {
		polls.clear();
		if (cursor == null) {
			return;
		}

		if (cursor.moveToFirst()) {
			do {
				int id = cursor.getInt(cursor.getColumnIndex(BoardGamePolls._ID));
				String name = cursor.getString(cursor.getColumnIndex(BoardGamePolls.NAME));
				String title = cursor.getString(cursor.getColumnIndex(BoardGamePolls.TITLE));
				int votes = cursor.getInt(cursor.getColumnIndex(BoardGamePolls.VOTES));
				polls.add(new Poll(name, title, votes, id));
			} while (cursor.moveToNext());
		}
	}

	public void createPollResults(Cursor cursor) {
		if (cursor == null) {
			return;
		}

		if (cursor.moveToFirst()) {

			int pollId = cursor.getInt(cursor.getColumnIndex(BoardGamePollResults.POLL_ID));
			Poll poll = null;
			for (Poll p : polls) {
				if (p.getId() == pollId) {
					poll = p;
					break;
				}
			}
			if (poll == null) {
				return;
			}

			do {
				String players = cursor.getString(cursor.getColumnIndex(BoardGamePollResults.PLAYERS));
				int id = cursor.getInt(cursor.getColumnIndex(BoardGamePollResults._ID));
				poll.addResults(new PollResults(players, id));
			} while (cursor.moveToNext());
		}
	}

	public void createPollResult(Cursor cursor) {
		if (cursor == null) {
			return;
		}

		if (cursor.moveToFirst()) {
			int resultsId = cursor.getInt(cursor.getColumnIndex(BoardGamePollResult.POLLRESULTS_ID));
			PollResults results = null;
			for (Poll p : polls) {
				for (PollResults r : p.getResultsList()) {
					if (r.getId() == resultsId) {
						results = r;
						break;
					}
				}
				if (results != null) {
					break;
				}
			}
			if (results == null) {
				return;
			}

			do {
				String value = cursor.getString(cursor.getColumnIndex(BoardGamePollResult.VALUE));
				int level = cursor.getInt(cursor.getColumnIndex(BoardGamePollResult.LEVEL));
				int votes = cursor.getInt(cursor.getColumnIndex(BoardGamePollResult.VOTES));
				results.addResult(new PollResult(value, votes, level));
			} while (cursor.moveToNext());
		}
	}
}