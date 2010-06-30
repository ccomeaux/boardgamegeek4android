package com.boardgamegeek.model;

import java.util.ArrayList;
import java.util.List;

public class Poll {

	private int id;
	private String name;
	private String title;
	private int totalVotes;
	List<PollResults> resultsList = new ArrayList<PollResults>();

	public Poll(String name, String title, int totalVotes) {
		this(name, title, totalVotes, -1);
	}

	public Poll(String name, String title, int totalVotes, int id) {
		this.name = name;
		this.title = title;
		this.totalVotes = totalVotes;
		this.id = id;
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

	public int getId() {
		return id;
	}
}
