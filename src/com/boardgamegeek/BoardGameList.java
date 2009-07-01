package com.boardgamegeek;

import java.util.List;
import java.util.Vector;

public class BoardGameList {
	private List<BoardGame> boardGameList = new Vector<BoardGame>(0);

	void addItem(BoardGame boardGame) {
		boardGameList.add(boardGame);
	}

	List<BoardGame> getBoardGameList() {
		return boardGameList;
	}

	// results count
	public int getCount() {
		return boardGameList.size();
	}

	public BoardGame elementAt(int i) {
		return boardGameList.get(i);
	}
}