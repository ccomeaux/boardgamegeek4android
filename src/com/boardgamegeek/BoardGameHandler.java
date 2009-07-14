package com.boardgamegeek;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class BoardGameHandler extends DefaultHandler {

	private StringBuffer currentElement;
	private BoardGame boardGame;
	private Boolean isPrimaryName = false;
	private boolean isStats;
	private boolean isRanks;
	private boolean isRank;
	private String objectId;

	// returns object after parsing
	public BoardGame getBoardGame() {
		return boardGame;
	}

	@Override
	public void startDocument() throws SAXException {
		// initialize object
		boardGame = new BoardGame();
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {

		currentElement = new StringBuffer();

		if (localName.equals("boardgame")) {
			boardGame.setGameId(atts.getValue("objectid"));
		} else if (localName == "name") {
			String primaryAttribute = atts.getValue("primary");
			if (primaryAttribute != null
					&& primaryAttribute.equalsIgnoreCase("true")) {
				isPrimaryName = true;
			}
		} else if (localName == "statistics") {
			isStats = true;
		} else if (localName == "boardgamedesigner"
				|| localName == "boardgameartist"
				|| localName == "boardgamepublisher"
				|| localName == "boardgamecategory"
				|| localName == "boardgamemechanic"
				|| localName == "boardgameexpansion") {
			String idAttribute = atts.getValue("objectid");
			if (idAttribute != null) {
				objectId = idAttribute;
			}
		} else if (isStats) {
			if (isRanks) {
				if (localName == "rank") {
					String attribute = atts.getValue("type");
					if (attribute != null
							&& attribute.equalsIgnoreCase("boardgame")) {
						isRank = true;
					}
				}
			} else if (localName == "ranks") {
				isRanks = true;
			}
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {

		if (localName == "yearpublished") {
			boardGame.setYearPublished(parseInt(currentElement.toString()));
		} else if (localName == "minplayers") {
			boardGame.setMinPlayers(parseInt(currentElement.toString()));
		} else if (localName == "maxplayers") {
			boardGame.setMaxPlayers(parseInt(currentElement.toString()));
		} else if (localName == "playingtime") {
			boardGame.setPlayingTime(parseInt(currentElement.toString()));
		} else if (localName == "age") {
			boardGame.setAge(parseInt(currentElement.toString()));
		} else if (isPrimaryName && localName == "name") {
			boardGame.setName(currentElement.toString());
			isPrimaryName = false;
		} else if (localName == "description") {
			boardGame.setDescription(currentElement.toString());
		} else if (localName == "thumbnail") {
			boardGame.setThumbnailUrl(currentElement.toString());
		} else if (localName == "statistics") {
			isStats = false;
		} else if (localName == "boardgamedesigner") {
			if (objectId != "") {
				boardGame.addDesigner(objectId, currentElement.toString());
			}
			objectId = "";
		} else if (localName == "boardgameartist") {
			if (objectId != "") {
				boardGame.addArtist(objectId, currentElement.toString());
			}
			objectId = "";
		} else if (localName == "boardgamepublisher") {
			if (objectId != "") {
				boardGame.addPublisher(objectId, currentElement.toString());
			}
			objectId = "";
		} else if (localName == "boardgamecategory") {
			if (objectId != "") {
				boardGame.addCategory(objectId, currentElement.toString());
			}
			objectId = "";
		} else if (localName == "boardgamemechanic") {
			if (objectId != "") {
				boardGame.addMechanic(objectId, currentElement.toString());
			}
			objectId = "";
		} else if (localName == "boardgameexpansion") {
			if (objectId != "") {
				boardGame.addExpansion(objectId, currentElement.toString());
			}
			objectId = "";
		} else if (isStats) {
			if (localName == "usersrated") {
				boardGame.setUsersRated(parseInt(currentElement.toString()));
			} else if (localName == "average") {
				boardGame.setAverage(parseDouble(currentElement.toString()));
			} else if (localName == "bayesaverage") {
				boardGame
						.setBayesAverage(parseDouble(currentElement.toString()));
			} else if (isRanks && localName == "ranks") {
				isRanks = false;
			} else if (isRank && localName == "rank") {
				boardGame.setRank(parseInt(currentElement.toString()));
				isRank = false;
			} else if (localName == "stddev") {
				boardGame.setStandardDeviation(parseDouble(currentElement
						.toString()));
			} else if (localName == "median") {
				boardGame.setMedian(parseInt(currentElement.toString()));
			} else if (localName == "owned") {
				boardGame.setOwnedCount(parseInt(currentElement.toString()));
			} else if (localName == "trading") {
				boardGame.setTradingCount(parseInt(currentElement.toString()));
			} else if (localName == "wanting") {
				boardGame.setWantingCount(parseInt(currentElement.toString()));
			} else if (localName == "wishing") {
				boardGame.setWishingCount(parseInt(currentElement.toString()));
			} else if (localName == "numcomments") {
				boardGame.setCommentCount(parseInt(currentElement.toString()));
			} else if (localName == "numweights") {
				boardGame.setWeightCount(parseInt(currentElement.toString()));
			} else if (localName == "averageweight") {
				boardGame.setAverageWeight(parseDouble(currentElement
						.toString()));
			}
		}
	}

	@Override
	public void characters(char ch[], int start, int length) {
		currentElement.append(ch, start, length);
	}

	private int parseInt(String text) {
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	private double parseDouble(String text) {
		try {
			return Double.parseDouble(text);
		} catch (NumberFormatException ex) {
			return 0;
		}
	}
}