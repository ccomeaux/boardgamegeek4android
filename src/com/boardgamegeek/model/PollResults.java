package com.boardgamegeek.model;

import java.util.ArrayList;
import java.util.List;

public class PollResults {

	private int id;
	private String numberOfPlayers;
	private List<PollResult> resultList = new ArrayList<PollResult>();

	public PollResults(String numberOfPlayers) {
		this(numberOfPlayers, -1);
	}

	public PollResults(String numberOfPlayers, int id) {
		this.numberOfPlayers = numberOfPlayers;
		this.id = id;
	}

	public void setNumberOfPlayers(String numberOfPlayers) {
		this.numberOfPlayers = numberOfPlayers;
	}

	public String getNumberOfPlayers() {
		return numberOfPlayers;
	}

	public List<PollResult> getResultList() {
		return resultList;
	}

	public void addResult(PollResult result) {
		resultList.add(result);
	}

	public int getId() {
		return id;
	}
}
