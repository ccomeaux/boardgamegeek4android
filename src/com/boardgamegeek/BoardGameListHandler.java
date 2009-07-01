package com.boardgamegeek;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class BoardGameListHandler extends DefaultHandler {
	// declare variables
	private BoardGameList boardGameList;
	private BoardGame boardGame;
	private String current_tag = "";
	private Boolean primary = true;
	private Boolean is_name = false;

	// returns object after parsing
	public BoardGameList getBoardGameList() {
		return boardGameList;
	}

	@Override
	public void startDocument() throws SAXException {
		// initialize objects
		boardGameList = new BoardGameList();
		boardGame = new BoardGame();
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		// use this to keep track of where we are
		current_tag = localName;

		if (localName.equals("boardgame")) {
			boardGame = new BoardGame();
			String atts_gameid = atts.getValue("objectid");
			boardGame.setGameID(atts_gameid);
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		current_tag = "";

		// add to the list if we are done parsing a game
		if (localName.equals("boardgame")) {
			boardGameList.addItem(boardGame);
			primary = true;
		} else if (localName.equals("name"))
			is_name = false;
	}

	@Override
	public void characters(char ch[], int start, int length) {
		if (current_tag.equals("name") && primary) {
			boardGame.setName(new String(ch, start, length));
			is_name = true;
			primary = false;
		} else if (is_name)
			boardGame.setName(new String(ch, start, length));
		else if (current_tag.equals("yearpublished"))
			boardGame.setYearPublished(new String(ch, start, length));
	}
}