package com.boardgamegeek;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.boardgamegeek.model.*;

public class BoardGameHandler extends DefaultHandler {

	private StringBuffer currentElement;
	private BoardGame boardGame;
	private Boolean isPrimaryName = false;
	private boolean isStats;
	private boolean isRanks;
	private String rankType = "";
	private int objectId;
	private Poll currentPoll;
	private PollResults currentPollResults;

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
	public void endDocument() throws SAXException {}

	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
		throws SAXException {

		currentElement = new StringBuffer();

		if (localName.equals("boardgame")) {
			boardGame.setGameId(Utility.parseInt(atts.getValue("objectid")));
		} else if (localName.equals("name")) {
			String primaryAttribute = atts.getValue("primary");
			if (primaryAttribute != null && primaryAttribute.equalsIgnoreCase("true")) {
				isPrimaryName = true;
				boardGame.setSortIndex(Utility.parseInt(atts.getValue("sortindex"), 1));
			}
		} else if (localName.equals("statistics")) {
			isStats = true;
		} else if (localName.equals("boardgamedesigner") || localName.equals("boardgameartist")
			|| localName.equals("boardgamepublisher") || localName.equals("boardgamecategory")
			|| localName.equals("boardgamemechanic") || localName.equals("boardgameexpansion")) {
			String idAttribute = atts.getValue("objectid");
			if (idAttribute != null) {
				objectId = Utility.parseInt(idAttribute);
			}
		} else if (isStats) {
			if (localName.equals("ranks")) {
				isRanks = true;
			}
		} else if (localName.equals("poll")) {
			String pollName = atts.getValue("name");
			String pollTitle = atts.getValue("title");
			int pollVotes = Utility.parseInt(atts.getValue("totalvotes"));
			currentPoll = new Poll(pollName, pollTitle, pollVotes);
		} else if (currentPoll != null) {
			if (localName.equals("results")) {
				currentPollResults = new PollResults(atts.getValue("numplayers"));
			} else if (currentPollResults != null && localName.equals("result")) {
				String value = atts.getValue("value");
				int numberOfVotes = Utility.parseInt(atts.getValue("numvotes"));
				int level = Utility.parseInt(atts.getValue("level"));
				PollResult result = new PollResult(value, numberOfVotes, level);
				currentPollResults.addResult(result);
			}
		} else if (localName.equals("error")) {
			String message = atts.getValue("message");
			boardGame.setName(message);
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {

		if (localName.equals("yearpublished")) {
			boardGame.setYearPublished(Utility.parseInt(currentElement.toString()));
		} else if (localName.equals("minplayers")) {
			boardGame.setMinPlayers(Utility.parseInt(currentElement.toString()));
		} else if (localName.equals("maxplayers")) {
			boardGame.setMaxPlayers(Utility.parseInt(currentElement.toString()));
		} else if (localName.equals("playingtime")) {
			boardGame.setPlayingTime(Utility.parseInt(currentElement.toString()));
		} else if (localName.equals("age")) {
			boardGame.setAge(Utility.parseInt(currentElement.toString()));
		} else if (isPrimaryName && localName.equals("name")) {
			boardGame.setName(currentElement.toString());
			isPrimaryName = false;
		} else if (localName.equals("description")) {
			boardGame.setDescription(currentElement.toString());
		} else if (localName.equals("thumbnail")) {
			boardGame.setThumbnailUrl(currentElement.toString());
		} else if (localName.equals("boardgamedesigner")) {
			if (objectId != 0) {
				boardGame.addDesigner(objectId, currentElement.toString());
			}
			objectId = 0;
		} else if (localName.equals("boardgameartist")) {
			if (objectId != 0) {
				boardGame.addArtist(objectId, currentElement.toString());
			}
			objectId = 0;
		} else if (localName.equals("boardgamepublisher")) {
			if (objectId != 0) {
				boardGame.addPublisher(objectId, currentElement.toString());
			}
			objectId = 0;
		} else if (localName.equals("boardgamecategory")) {
			if (objectId != 0) {
				boardGame.addCategory(objectId, currentElement.toString());
			}
			objectId = 0;
		} else if (localName.equals("boardgamemechanic")) {
			if (objectId != 0) {
				boardGame.addMechanic(objectId, currentElement.toString());
			}
			objectId = 0;
		} else if (localName.equals("boardgameexpansion")) {
			if (objectId != 0) {
				boardGame.addExpansion(objectId, currentElement.toString());
			}
			objectId = 0;
		} else if (localName.equals("poll")) {
			if (currentPoll != null) {
				boardGame.addPoll(currentPoll);
				currentPoll = null;
			}
		} else if (localName.equals("results")) {
			if (currentPoll != null && currentPollResults != null) {
				currentPoll.addResults(currentPollResults);
				currentPollResults = null;
			}
		} else if (localName.equals("statistics")) {
			isStats = false;
		} else if (isStats) {
			if (localName.equals("usersrated")) {
				boardGame.setRatingCount(Utility.parseInt(currentElement.toString()));
			} else if (localName.equals("average")) {
				boardGame.setAverage(Utility.parseDouble(currentElement.toString()));
			} else if (localName.equals("bayesaverage")) {
				boardGame.setBayesAverage(Utility.parseDouble(currentElement.toString()));
			} else if (isRanks && localName.equals("ranks")) {
				isRanks = false;
			} else if (isRanks) {
				if (localName.equalsIgnoreCase("rankobjecttype") && currentElement != null
					&& currentElement.toString().equalsIgnoreCase("subtype")) {
					rankType = "boardgame";
				} else if (localName.equalsIgnoreCase("rankvalue") && rankType.equalsIgnoreCase("boardgame")
					&& currentElement != null) {
					boardGame.setRank(Utility.parseInt(currentElement.toString()));
				} else if (localName.equalsIgnoreCase("rank")) {
					rankType = "";
				}
			} else if (localName.equals("stddev")) {
				boardGame.setStandardDeviation(Utility.parseDouble(currentElement.toString()));
			} else if (localName.equals("median")) {
				boardGame.setMedian(Utility.parseDouble(currentElement.toString()));
			} else if (localName.equals("owned")) {
				boardGame.setOwnedCount(Utility.parseInt(currentElement.toString()));
			} else if (localName.equals("trading")) {
				boardGame.setTradingCount(Utility.parseInt(currentElement.toString()));
			} else if (localName.equals("wanting")) {
				boardGame.setWantingCount(Utility.parseInt(currentElement.toString()));
			} else if (localName.equals("wishing")) {
				boardGame.setWishingCount(Utility.parseInt(currentElement.toString()));
			} else if (localName.equals("numcomments")) {
				boardGame.setCommentCount(Utility.parseInt(currentElement.toString()));
			} else if (localName.equals("numweights")) {
				boardGame.setWeightCount(Utility.parseInt(currentElement.toString()));
			} else if (localName.equals("averageweight")) {
				boardGame.setAverageWeight(Utility.parseDouble(currentElement.toString()));
			}
		}
	}

	@Override
	public void characters(char ch[], int start, int length) {
		currentElement.append(ch, start, length);
	}
}