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
	private String rankType;
	private String objectId;
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
		} else if (localName == "name") {
			String primaryAttribute = atts.getValue("primary");
			if (primaryAttribute != null && primaryAttribute.equalsIgnoreCase("true")) {
				isPrimaryName = true;
				boardGame.setSortIndex(Utility.parseInt(atts.getValue("sortindex"), 1));
			}
		} else if (localName == "statistics") {
			isStats = true;
		} else if (localName == "boardgamedesigner" || localName == "boardgameartist"
			|| localName == "boardgamepublisher" || localName == "boardgamecategory"
			|| localName == "boardgamemechanic" || localName == "boardgameexpansion") {
			String idAttribute = atts.getValue("objectid");
			if (idAttribute != null) {
				objectId = idAttribute;
			}
		} else if (isStats) {
			if (isRanks) {
				if (localName == "rank") {
					rankType = atts.getValue("type");
				}
			} else if (localName == "ranks") {
				isRanks = true;
			}
		} else if (localName == "poll") {
			String pollName = atts.getValue("name");
			String pollTitle = atts.getValue("title");
			int pollVotes = Utility.parseInt(atts.getValue("totalvotes"));
			currentPoll = new Poll(pollName, pollTitle, pollVotes);
		} else if (currentPoll != null) {
			if (localName == "results") {
				currentPollResults = new PollResults(atts.getValue("numplayers"));
			} else if (currentPollResults != null && localName == "result") {
				String value = atts.getValue("value");
				int numberOfVotes = Utility.parseInt(atts.getValue("numvotes"));
				int level = Utility.parseInt(atts.getValue("level"));
				PollResult result = new PollResult(value, numberOfVotes, level);
				currentPollResults.addResult(result);
			}
		} else if (localName == "error") {
			String message = atts.getValue("message");
			boardGame.setName(message);
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {

		if (localName == "yearpublished") {
			boardGame.setYearPublished(Utility.parseInt(currentElement.toString()));
		} else if (localName == "minplayers") {
			boardGame.setMinPlayers(Utility.parseInt(currentElement.toString()));
		} else if (localName == "maxplayers") {
			boardGame.setMaxPlayers(Utility.parseInt(currentElement.toString()));
		} else if (localName == "playingtime") {
			boardGame.setPlayingTime(Utility.parseInt(currentElement.toString()));
		} else if (localName == "age") {
			boardGame.setAge(Utility.parseInt(currentElement.toString()));
		} else if (isPrimaryName && localName == "name") {
			boardGame.setName(currentElement.toString());
			isPrimaryName = false;
		} else if (localName == "description") {
			boardGame.setDescription(currentElement.toString());
		} else if (localName == "thumbnail") {
			boardGame.setThumbnailUrl(currentElement.toString());
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
		} else if (localName == "poll") {
			if (currentPoll != null) {
				boardGame.addPoll(currentPoll);
				currentPoll = null;
			}
		} else if (localName == "results") {
			if (currentPoll != null && currentPollResults != null) {
				currentPoll.addResults(currentPollResults);
				currentPollResults = null;
			}
		} else if (localName == "statistics") {
			isStats = false;
		} else if (isStats) {
			if (localName == "usersrated") {
				boardGame.setRatingCount(Utility.parseInt(currentElement.toString()));
			} else if (localName == "average") {
				boardGame.setAverage(Utility.parseDouble(currentElement.toString()));
			} else if (localName == "bayesaverage") {
				boardGame.setBayesAverage(Utility.parseDouble(currentElement.toString()));
			} else if (isRanks && localName == "ranks") {
				isRanks = false;
			} else if (isRanks && localName == "rank") {
				int rank = Utility.parseInt(currentElement.toString());
				if (rankType.equalsIgnoreCase("boardgame")) {
					boardGame.setRank(rank);
				} else if (rankType.equalsIgnoreCase("subdomain_abstracts")) {
					boardGame.setRankAbstract(rank);
				} else if (rankType.equalsIgnoreCase("subdomain_ccgrank")) {
					boardGame.setRankCcg(rank);
				} else if (rankType.equalsIgnoreCase("subdomain_familygamesrank")) {
					boardGame.setRankFamily(rank);
				} else if (rankType.equalsIgnoreCase("subdomain_kidsgames")) {
					boardGame.setRankKids(rank);
				} else if (rankType.equalsIgnoreCase("subdomain_partygamerank")) {
					boardGame.setRankParty(rank);
				} else if (rankType.equalsIgnoreCase("subdomain_strategygamesrank")) {
					boardGame.setRankStrategy(rank);
				} else if (rankType.equalsIgnoreCase("subdomain_thematic")) {
					boardGame.setRankTheme(rank);
				} else if (rankType.equalsIgnoreCase("subdomain_wargames")) {
					boardGame.setRankWar(rank);
				}
			} else if (localName == "stddev") {
				boardGame.setStandardDeviation(Utility.parseDouble(currentElement.toString()));
			} else if (localName == "median") {
				boardGame.setMedian(Utility.parseDouble(currentElement.toString()));
			} else if (localName == "owned") {
				boardGame.setOwnedCount(Utility.parseInt(currentElement.toString()));
			} else if (localName == "trading") {
				boardGame.setTradingCount(Utility.parseInt(currentElement.toString()));
			} else if (localName == "wanting") {
				boardGame.setWantingCount(Utility.parseInt(currentElement.toString()));
			} else if (localName == "wishing") {
				boardGame.setWishingCount(Utility.parseInt(currentElement.toString()));
			} else if (localName == "numcomments") {
				boardGame.setCommentCount(Utility.parseInt(currentElement.toString()));
			} else if (localName == "numweights") {
				boardGame.setWeightCount(Utility.parseInt(currentElement.toString()));
			} else if (localName == "averageweight") {
				boardGame.setAverageWeight(Utility.parseDouble(currentElement.toString()));
			}
		}
	}

	@Override
	public void characters(char ch[], int start, int length) {
		currentElement.append(ch, start, length);
	}
}