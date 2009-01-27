package com.boardgamegeek;

import java.text.DecimalFormat;

import android.text.Html;

public class BoardGame
{
     private String gameid = null;
     private String yearpublished = null;
     private String minplayers = null;
     private String maxplayers = null;
     private String players = null;
     private String playingtime = null;
     private String age = null;
     private String name = "";
     private String rank = "N/A";
     private String rating = "0.00";
     private float numericrating = 0;
     private String num_ratings = null;
     private String thumbnail = null;
     private String description = "";

     // game id
     public String getGameID() {
    	 return gameid;
     }
     public void setGameID(String gameid) {
    	 this.gameid = gameid;
     }
     
     // year published
     public String getYearPublished() {
    	 return yearpublished;
     }
     public void setYearPublished(String yearpublished) {
    	 this.yearpublished = yearpublished;
     }     
     
     // min number of players
     public String getMinPlayers() {
    	 return minplayers;
     }
     public void setMinPlayers(String minplayers) {
    	 this.minplayers = minplayers;
     }
     
     // max number of players
     public String getMaxPlayers() {
    	 return maxplayers;
     }
     public void setMaxPlayers(String maxplayers) {
    	 this.maxplayers = maxplayers;
     }

     // number of players
     public void getPlayers() {
    	 if (this.minplayers.equals(this.maxplayers))
    		 this.players = this.minplayers;
    	 else
    		 this.players = this.minplayers + " - " + this.maxplayers;
     }
     
     // game playing time
     public String getPlayingTime() {
    	 return playingtime;
     }
     public void setPlayingTime(String playingtime) {
    	 this.playingtime = playingtime;
     }
     
     // player age (minimum)
     public String getAge() {
    	 return age;
     }
     public void setAge(String age) {
    	 this.age = age;
     }
     
     // game name
     public String getName() {
    	 return name;
     }
     public void setName(String name) {
    	 this.name += name;
     }
     
     // game rank
     public String getRank() {
    	 return rank;
     }
     public void setRank(String rank) {
         this.rank = rank;
     }
    
     // game rating
     public String getRating() {
         return rating;
     }
     public void setRating(String rating) {
    	 setNumericRating(rating);
    	 this.rating = Float.toString(this.numericrating);
     }
     public float getNumericRating() {
         return numericrating;
     }
     public void setNumericRating(String rating) {
    	 this.numericrating = Float.valueOf(rating);
    	 this.numericrating = Float.valueOf((new DecimalFormat("###.00").format(this.numericrating)));
     }
     
     // number of ratings
     public String getNumRatings() {
    	 return num_ratings;
     }
     public void setNumRatings(String num_ratings) {
         this.num_ratings = num_ratings;
     }
     
     // thumbnail image
     public String getThumbnail() {
         return thumbnail;
     }
     public void setThumbnail(String thumbnail) {
         this.thumbnail = thumbnail;
     }     
     
     // game description
     public String getDescription() {
    	 String nonhtml_description = Html.fromHtml(this.description).toString();
    	 nonhtml_description = nonhtml_description.replace("\n\n", "\n");
         return nonhtml_description;
     }
     public void setDescription(String description) {
         this.description += description;
     }
     
     public String getGameInfo() {
    	 String game_info = "";
    	 getPlayers();
    	 if (gameid == null)
    		 game_info = "Game Not Found";
    	 else
    	 {
    		 game_info += "Year Published: ";
    		 if (!this.yearpublished.equals("0"))
    			 game_info += this.yearpublished;
    		 game_info += "\n";
    		 
    		 game_info += "Players: ";
    		 if (!this.players.equals("0"))
    			 game_info += this.players;
    		 game_info += "\n";
    		 
    		 game_info += "Playing Time: ";
    		 if (!this.playingtime.equals("0"))
    			 game_info += this.playingtime + " minutes";
    		 game_info += "\n";
    		 
    		 game_info += "Ages: ";
    		 if (!this.age.equals("0"))
    			 game_info += this.age + " and up";
    		 game_info += "\n";
    		 
    		 game_info += "GameID: " + this.gameid;
    	 }
    	 return game_info;
     }
     
     public String getThumbnailURL() {
         return this.thumbnail;
     }
}