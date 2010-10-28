package com.boardgamegeek.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.text.TextUtils;

public class Poll {

	private int id;
	private String name;
	private String title;
	private int totalVotes;
	private final List<PollResults> resultsList = new ArrayList<PollResults>();

	public Poll(String name, String title, int totalVotes) {
		this(name, title, totalVotes, -1);
	}

	public Poll(String name, String title, int totalVotes, int id) {
		this.name = name;
		this.title = title;
		this.totalVotes = totalVotes;
		this.id = id;
	}

	public int getId() {
		return id;
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

	public PollResults getResultsByPlayers(String players) {
		if (resultsList == null) {
			return null;
		}
		if (TextUtils.isEmpty(players) && resultsList.size() == 1) {
			return resultsList.get(0);
		}
		for (PollResults results : resultsList) {
			if (results.getNumberOfPlayers().equals(players)) {
				return results;
			}
		}
		return null;
	}

	public Collection<Integer> getResultsIds() {
		if (resultsList == null) {
			return null;
		}
		final List<Integer> ids = new ArrayList<Integer>(resultsList.size());
		for (PollResults results : resultsList) {
			ids.add(results.getId());
		}
		return ids;
	}
}
