package com.boardgamegeek;

public class BoardGame {
	private String users_wanting = null;
	private String users_trading = null;

	private String gameId = null;
	private String name = "";
	private int yearPublished = 0;
	private int minPlayers = 0;
	private int maxPlayers = 0;
	private int playingTime = 0;
	private int age = 0;
	private String description = "";
	private String thumbnailUrl = null;
	private int usersRated = 0;
	private double average = 0.0;
	private double bayesAverage;
	private int rank = 0;
	private double standardDeviation;
	private int median;
	private int ownedCount;
	private int tradingCount;
	private int wantingCount;
	private int wishingCount;
	private int commentCount;
	private int weightCount;
	private double averageWeight;

	// TODO: designers
	// TODO: artists
	// TODO: publishers
	// TODO: categories
	// TODO: mechanics
	// TODO: expansions
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
		} else if (minPlayers == maxPlayers) {
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
	public int getUsersRated() {
		return usersRated;
	}

	public void setUsersRated(int usersRated) {
		this.usersRated = usersRated;
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

	// game description
	public String getDescription() {
		return decodeHtml(description);
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUsersWanting() {
		return users_wanting;
	}

	public void setUsersWanting(String users_wanting) {
		this.users_wanting = users_wanting;
	}

	public String getUsersTrading() {
		return users_trading;
	}

	public void setUsersTrading(String users_trading) {
		this.users_trading = users_trading;
	}

	public String getGameInfo() {
		// TODO: convert to a StringBuffer
		String game_info = "";
		getPlayers();
		if (gameId == null) {
			game_info = "Game Not Found";
		} else {
			if (yearPublished != 0) {
				game_info += "Year Published: ";
				game_info += yearPublished;
				game_info += "\n";
			}

			game_info += "Players: " + getPlayers() + "\n";

			if (playingTime != 0) {
				game_info += "Playing Time: ";
				game_info += playingTime + " minutes";
				game_info += "\n";
			}

			if (age != 0) {
				game_info += "Ages: ";
				game_info += age + " and up";
				game_info += "\n";
			}

			game_info += "GameID: " + gameId;
		}
		return game_info;
	}

	public String decodeHtml(String encodedHtml) {
		// this replaces Html.FromHtml but preserves new lines
		String decodedHtml = encodedHtml;
		decodedHtml = decodedHtml.replace("&lt;", "<");
		decodedHtml = decodedHtml.replace("&gt;", ">");
		decodedHtml = decodedHtml.replace("&quot;", "\"");
		decodedHtml = decodedHtml.replace("&apos;", "'");
		decodedHtml = decodedHtml.replace("&rsquo;", "'");
		decodedHtml = decodedHtml.replace("&amp;", "&");
		decodedHtml = decodedHtml.replace("\n\n\n", "\n\n");
		return decodedHtml;
	}

	public void setStandardDeviation(double standardDeviation) {
		this.standardDeviation = standardDeviation;
	}

	public double getStandardDeviation() {
		return standardDeviation;
	}

	public void setMedian(int median) {
		this.median = median;
	}

	public int getMedian() {
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
}