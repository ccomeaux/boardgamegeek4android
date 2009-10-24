package com.boardgamegeek;

import java.util.ArrayList;
import java.util.List;

public class PollResults {
	
	private String numberOfPlayers;
	private List<PollResult> resultList = new ArrayList<PollResult>();
	
	public PollResults(String numberOfPlayers){
		this.numberOfPlayers = numberOfPlayers;
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
	
	public void addResult(PollResult result){
		resultList.add(result);
	}
}
