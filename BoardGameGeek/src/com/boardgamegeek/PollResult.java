package com.boardgamegeek;

public class PollResult {

	private int level;
	private String value;
	private int numberOfVotes;

	public PollResult(String value, int numberOfVotes) {
		this.value = value;
		this.numberOfVotes = numberOfVotes;
	}

	public PollResult(String value, int numberOfVotes, int level) {
		this.value = value;
		this.numberOfVotes = numberOfVotes;
		this.level = level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getLevel() {
		return level;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setNumberOfVotes(int numberOfVotes) {
		this.numberOfVotes = numberOfVotes;
	}

	public int getNumberOfVotes() {
		return numberOfVotes;
	}
}
