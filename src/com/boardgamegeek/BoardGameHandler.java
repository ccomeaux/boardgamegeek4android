package com.boardgamegeek;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class BoardGameHandler extends DefaultHandler
{     
     // declare variables
     private BoardGame boardGame;
     private String current_tag = "";
     private Boolean primary = true;
     private Boolean is_name = false;
     private Boolean is_description = false;
     
     // returns object after parsing
     public BoardGame getBoardGame() { return boardGame; }

     @Override
     public void startDocument() throws SAXException
     {
    	 // initialize object
    	 boardGame = new BoardGame();
     }

     @Override
     public void endDocument() throws SAXException {}

     @Override
     public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException
     {
    	 // use this to keep track of where we are
    	 current_tag = localName;
 
    	 if (localName.equals("boardgame"))
    	 {
    		 String atts_gameid = atts.getValue("objectid");
    		 boardGame.setGameID(atts_gameid);
    	 }
     }
     
     @Override
     public void endElement(String namespaceURI, String localName, String qName) throws SAXException
     {
    	 current_tag = "";
    	 if (localName.equals("name"))
    		 is_name = false;
    	 else if (localName.equals("description"))
    		 is_description = false;
     }
     
     @Override
     public void characters(char ch[], int start, int length)
     {
    	 if (current_tag.equals("name") && primary)
    	 {
    		 boardGame.setName(new String(ch, start, length));
    		 is_name = true;
    		 primary = false;
    	 }
    	 else if (is_name)
    		 boardGame.setName(new String(ch, start, length));
    	 else if (current_tag.equals("thumbnail"))
    		 boardGame.setThumbnail(new String(ch, start, length));
    	 else if (current_tag.equals("description"))
    	 {
    		 boardGame.setDescription(new String(ch, start, length)); 
    		 is_description = true;
    	 }
    	 else if (is_description)
    		 boardGame.setDescription(new String(ch, start, length));
    	 else if (current_tag.equals("yearpublished"))
    		 boardGame.setYearPublished(new String(ch, start, length)); 
    	 else if (current_tag.equals("minplayers"))
    		 boardGame.setMinPlayers(new String(ch, start, length)); 
    	 else if (current_tag.equals("maxplayers"))
    		 boardGame.setMaxPlayers(new String(ch, start, length)); 
    	 else if (current_tag.equals("playingtime"))
    		 boardGame.setPlayingTime(new String(ch, start, length)); 
    	 else if (current_tag.equals("age"))
    		 boardGame.setAge(new String(ch, start, length));
    	 else if (current_tag.equals("usersrated"))
    		 boardGame.setNumRatings(new String(ch, start, length));
    	 else if (current_tag.equals("average"))
    		 boardGame.setRating(new String(ch, start, length));
    	 else if (current_tag.equals("rank"))
    		 boardGame.setRank(new String(ch, start, length));
     }
}