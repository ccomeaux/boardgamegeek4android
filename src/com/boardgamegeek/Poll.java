package com.boardgamegeek;

import java.util.ArrayList;
import java.util.List;

public class Poll {

	private String name;
	private String title;
	private int totalVotes;
	List<PollResults> resultsList = new ArrayList<PollResults>();

	public Poll(String name, String title, int totalVotes) {
		this.name = name;
		this.title = title;
		this.totalVotes = totalVotes;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setTotalVotes(int totalVotes) {
		this.totalVotes = totalVotes;
	}

	public int getTotalVotes() {
		return totalVotes;
	}

	public List<PollResults> getResultsList() {
		return resultsList;
	}

	public void addResults(PollResults results) {
		resultsList.add(results);
	}
}
