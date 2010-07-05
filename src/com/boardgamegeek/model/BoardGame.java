package com.boardgamegeek.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.boardgamegeek.BoardGameGeekData.*;
import com.boardgamegeek.DataHelper;
import com.boardgamegeek.Utility;

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
	private int rankFamily;
	private int rankKids = 0;
	private int rankParty = 0;
	private int rankStrategy = 0;
	private int rankTheme = 0;
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
	private HashMap<Integer, Designer> designers = new HashMap<Integer, Designer>();
	private HashMap<Integer, Artist> artists = new HashMap<Integer, Artist>();
	private HashMap<Integer, Publisher> publishers = new HashMap<Integer, Publisher>();
	private HashMap<Integer, Category> categories = new HashMap<Integer, Category>();
	private HashMap<Integer, Mechanic> mechanics = new HashMap<Integer, Mechanic>();
	private HashMap<Integer, Expansion> expansions = new HashMap<Integer, Expansion>();
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

	public int getRankFamily() {
		return rankFamily;
	}

	public void setRankFamily(int rank) {
		this.rankFamily = rank;
	}

	public int getRankKids() {
		return rankKids;
	}

	public void setRankKids(int rank) {
		this.rankKids = rank;
	}

	public int getRankParty() {
		return rankParty;
	}

	public void setRankParty(int rank) {
		this.rankParty = rank;
	}

	public int getRankStrategy() {
		return rankStrategy;
	}

	public void setRankStrategy(int rank) {
		this.rankStrategy = rank;
	}

	public int getRankTheme() {
		return rankTheme;
	}

	public void setRankTheme(int rank) {
		this.rankTheme = rank;
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

	public int getThumbnailId() {
		return DataHelper.getThumbnailId(thumbnailUrl);
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

	public int getDesignerKey(int id) {
		for (Entry<Integer, Designer> designer : designers.entrySet()) {
			if (designer.getValue().Id == id) {
				return designer.getKey();
			}
		}
		return 0;
	}

	public void addDesigner(int id, String name) {
		addDesigner(-1 * publishers.size(), id, name);
	}

	public void addDesigner(int key, int id, String name) {
		designers.put(key, new Designer(id, name));
	}

	public Collection<Integer> getDesignerIds() {
		List<Integer> ids = new ArrayList<Integer>(designers.size());
		for (Designer designer : designers.values()) {
			ids.add(designer.Id);
		}
		return ids;
	}

	public Collection<String> getDesignerNames() {
		List<String> names = new ArrayList<String>(designers.size());
		for (Designer designer : designers.values()) {
			names.add(designer.Name);
		}
		return names;
	}

	public Designer getDesignerByPosition(int position) {
		if (designers.size() > position) {
			return (Designer) designers.values().toArray()[position];
		} else {
			return null;
		}
	}

	public void createDesigners(Cursor cursor) {
		designers.clear();
		if (cursor == null) {
			return;
		}
		if (cursor.moveToFirst()) {
			do {
				designers.put(cursor.getInt(cursor.getColumnIndex(BoardGameDesigners._ID)), new Designer(
					cursor.getInt(cursor.getColumnIndex(BoardGameDesigners.DESIGNER_ID)), cursor
						.getString(cursor.getColumnIndex(BoardGameDesigners.DESIGNER_NAME))));
			} while (cursor.moveToNext());
		}
	}

	public int getArtistCount() {
		return artists.size();
	}

	public int getArtistKey(int id) {
		for (Entry<Integer, Artist> artist : artists.entrySet()) {
			if (artist.getValue().Id == id) {
				return artist.getKey();
			}
		}
		return 0;
	}

	public void addArtist(int id, String name) {
		addArtist(-1 * artists.size(), id, name);
	}

	public void addArtist(int key, int id, String name) {
		artists.put(key, new Artist(id, name));
	}

	public Collection<Integer> getArtistIds() {
		List<Integer> ids = new ArrayList<Integer>(artists.size());
		for (Artist artist : artists.values()) {
			ids.add(artist.Id);
		}
		return ids;
	}

	public Collection<String> getArtistNames() {
		List<String> names = new ArrayList<String>(artists.size());
		for (Artist artist : artists.values()) {
			names.add(artist.Name);
		}
		return names;
	}

	public Artist getArtistByPosition(int position) {
		if (artists.size() > position) {
			return (Artist) artists.values().toArray()[position];
		} else {
			return null;
		}
	}

	public void createArtists(Cursor cursor) {
		artists.clear();
		if (cursor == null) {
			return;
		}
		if (cursor.moveToFirst()) {
			do {
				artists.put(cursor.getInt(cursor.getColumnIndex(BoardGameArtists._ID)), new Artist(cursor
					.getInt(cursor.getColumnIndex(BoardGameArtists.ARTIST_ID)), cursor.getString(cursor
					.getColumnIndex(BoardGameArtists.ARTIST_NAME))));
			} while (cursor.moveToNext());
		}
	}

	public int getPublisherCount() {
		return publishers.size();
	}

	public int getPublisherKey(int id) {
		for (Entry<Integer, Publisher> publisher : publishers.entrySet()) {
			if (publisher.getValue().Id == id) {
				return publisher.getKey();
			}
		}
		return 0;
	}

	public void addPublisher(int id, String name) {
		addPublisher(-1 * publishers.size(), id, name);
	}

	public void addPublisher(int key, int id, String name) {
		publishers.put(key, new Publisher(id, name));
	}

	public Collection<Integer> getPublisherIds() {
		List<Integer> ids = new ArrayList<Integer>(publishers.size());
		for (Publisher publisher : publishers.values()) {
			ids.add(publisher.Id);
		}
		return ids;
	}

	public Collection<String> getPublisherNames() {
		List<String> names = new ArrayList<String>(publishers.size());
		for (Publisher publisher : publishers.values()) {
			names.add(publisher.Name);
		}
		return names;
	}

	public Publisher getPublisherByPosition(int position) {
		if (publishers.size() > position) {
			return (Publisher) publishers.values().toArray()[position];
		} else {
			return null;
		}
	}

	public void createPublishers(Cursor cursor) {
		publishers.clear();
		if (cursor == null) {
			return;
		}
		if (cursor.moveToFirst()) {
			do {
				publishers.put(cursor.getInt(cursor.getColumnIndex(BoardGamePublishers._ID)), new Publisher(
					cursor.getInt(cursor.getColumnIndex(BoardGamePublishers.PUBLISHER_ID)), cursor
						.getString(cursor.getColumnIndex(BoardGamePublishers.PUBLISHER_NAME))));
			} while (cursor.moveToNext());
		}
	}

	public int getCategoryCount() {
		return categories.size();
	}

	public int getCategoryKey(int id) {
		for (Entry<Integer, Category> category : categories.entrySet()) {
			if (category.getValue().Id == id) {
				return category.getKey();
			}
		}
		return 0;
	}

	public Category getCategoryByPosition(int position) {
		if (categories.size() > position) {
			return (Category) categories.values().toArray()[position];
		} else {
			return null;
		}
	}

	public void addCategory(int id, String name) {
		categories.put(-1 * categories.size(), new Category(id, name));
	}

	public Collection<Integer> getCategoryIds() {
		List<Integer> ids = new ArrayList<Integer>(categories.size());
		for (Category category : categories.values()) {
			ids.add(category.Id);
		}
		return ids;
	}

	public Collection<String> getCategoryNames() {
		List<String> names = new ArrayList<String>(categories.size());
		for (Category category : categories.values()) {
			names.add(category.Name);
		}
		return names;
	}

	public void createCategories(Cursor cursor) {
		categories.clear();
		if (cursor == null) {
			return;
		}
		if (cursor.moveToFirst()) {
			do {
				categories.put(cursor.getInt(cursor.getColumnIndex(BoardGameCategories._ID)), new Category(
					cursor.getInt(cursor.getColumnIndex(BoardGameCategories.CATEGORY_ID)), cursor
						.getString(cursor.getColumnIndex(BoardGameCategories.CATEGORY_NAME))));
			} while (cursor.moveToNext());
		}
	}

	public int getMechanicCount() {
		return mechanics.size();
	}

	public int getMechanicKey(int id) {
		for (Entry<Integer, Mechanic> mechanic : mechanics.entrySet()) {
			if (mechanic.getValue().Id == id) {
				return mechanic.getKey();
			}
		}
		return 0;
	}

	public Mechanic getMechanicByPosition(int position) {
		if (mechanics.size() > position) {
			return (Mechanic) mechanics.values().toArray()[position];
		} else {
			return null;
		}
	}

	public void addMechanic(int id, String name) {
		mechanics.put(-1 * mechanics.size(), new Mechanic(id, name));
	}

	public Collection<Integer> getMechanicIds() {
		List<Integer> ids = new ArrayList<Integer>(mechanics.size());
		for (Mechanic mechanic : mechanics.values()) {
			ids.add(mechanic.Id);
		}
		return ids;
	}

	public Collection<String> getMechanicNames() {
		List<String> names = new ArrayList<String>(mechanics.size());
		for (Mechanic mechanic : mechanics.values()) {
			names.add(mechanic.Name);
		}
		return names;
	}

	public void createMechanics(Cursor cursor) {
		mechanics.clear();
		if (cursor == null) {
			return;
		}
		if (cursor.moveToFirst()) {
			do {
				mechanics.put(cursor.getInt(cursor.getColumnIndex(BoardGameMechanics._ID)), new Mechanic(
					cursor.getInt(cursor.getColumnIndex(BoardGameMechanics.MECHANIC_ID)), cursor
						.getString(cursor.getColumnIndex(BoardGameMechanics.MECHANIC_NAME))));
			} while (cursor.moveToNext());
		}
	}

	public int getExpansionCount() {
		return expansions.size();
	}

	public int getExpansionKey(int id) {
		for (Entry<Integer, Expansion> expansion : expansions.entrySet()) {
			if (expansion.getValue().Id == id) {
				return expansion.getKey();
			}
		}
		return 0;
	}

	public Expansion getExpansionByPosition(int position) {
		if (expansions.size() > position) {
			return (Expansion) expansions.values().toArray()[position];
		} else {
			return null;
		}
	}

	public void addExpansion(int id, String name) {
		addExpansion(-1 * expansions.size(), id, name);
	}

	public void addExpansion(int key, int id, String name) {
		expansions.put(key, new Expansion(id, name));
	}

	public Collection<Integer> getExpansionIds() {
		List<Integer> ids = new ArrayList<Integer>(expansions.size());
		for (Expansion expansion : expansions.values()) {
			ids.add(expansion.Id);
		}
		return ids;
	}

	public Collection<String> getExpansionNames() {
		List<String> names = new ArrayList<String>(expansions.size());
		for (Expansion expansion : expansions.values()) {
			names.add(expansion.Name);
		}
		return names;
	}

	public void createExpanions(Cursor cursor) {
		expansions.clear();
		if (cursor == null) {
			return;
		}
		if (cursor.moveToFirst()) {
			do {
				expansions.put(cursor.getInt(cursor.getColumnIndex(BoardGameExpansions._ID)), new Expansion(
					cursor.getInt(cursor.getColumnIndex(BoardGameExpansions.EXPANSION_ID)), cursor
						.getString(cursor.getColumnIndex(BoardGameExpansions.EXPANSION_NAME))));
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

	public Poll getPollByName(String name) {
		for (Poll poll : polls) {
			if (poll.getName().equals(name)) {
				return poll;
			}
		}
		return null;
	}

	public Collection<Integer> getPollIds() {
		if (polls == null) {
			return null;
		}
		List<Integer> ids = new ArrayList<Integer>(polls.size());
		for (Poll poll : polls) {
			ids.add(poll.getId());
		}
		return ids;
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
				int id = cursor.getInt(cursor.getColumnIndex(BoardGamePollResult._ID));
				results.addResult(new PollResult(value, votes, level, id));
			} while (cursor.moveToNext());
		}
	}
}