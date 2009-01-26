package com.boardgamegeek;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ViewBoardGameList extends ListActivity
{	
	// declare variables
	private BoardGameList boardGameList;
	HashMap<String, String> gameListItems = new HashMap<String, String>();
	private ProgressDialog progress;
    final Handler handler = new Handler();
    String DEBUG_TAG = "BoardGameGeek DEBUG:";
	private SharedPreferences preferences;
    boolean exactSearch;
	boolean first_pass = true;
	
	@Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
    
        //get preferences
        getPreferences();
       	
    	// call the xml layout
        this.setContentView(R.layout.viewboardgamelist);
        
        getBoardGameList();
   	}

    @Override
    public void onResume()
    {
        super.onResume();
    
        //get preferences
        getPreferences();	
    }
    
	private void getBoardGameList()
	{
        // get the query from the intent
    	final String query = getIntent().getExtras().getString("QUERY");
    	
		// clear existing game list items
		gameListItems = new HashMap<String, String>();
		
		// display a progress dialog while fetching the game data
        if (first_pass)
        	progress = ProgressDialog.show(this, "Searching...", "Connecting to site...", true, false);
        else
        	progress = ProgressDialog.show(this, "No Results...", "Trying wider search...", true, false);
        	
        new Thread() {
        	public void run()
        	{
        		try
        		{
        			// set url
        			String query_url = "http://www.boardgamegeek.com/xmlapi/search?search="+query;
        			if (exactSearch && first_pass)
        				query_url += "&exact=1";

        			URL url = new URL(query_url.replace(" ", "%20"));

        			// create a new sax parser and get an xml reader from it
        			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        			XMLReader xmlReader = saxParser.getXMLReader();
                          
        			// set the xml reader's content handler and parse the xml
        			BoardGameListHandler boardGameListHandler = new BoardGameListHandler();
        			xmlReader.setContentHandler(boardGameListHandler);
        			xmlReader.parse(new InputSource(url.openStream()));

        			// get the parsed data as an object
        			boardGameList = boardGameListHandler.getBoardGameList();
        		}
        		catch (Exception e)
        		{
        			Log.d(DEBUG_TAG, "Exception", e);
        		}
        		handler.post(updateResults);
            }
        }.start();
	}
	
	// get results from handler
    final Runnable updateResults = new Runnable()
    {
        public void run()
        {
            updateUI();
        }
    };

	// updates ui after running progress dialog
	private void updateUI()
    {
		// iterate through search results and add to game list
		int count = Integer.parseInt(boardGameList.getCount());
		if (count == 0 && exactSearch && first_pass)
		{
			// try again if exactsearch is on and no results were found
			progress.dismiss();
			first_pass = false;
	        getBoardGameList();
		}
		else if (count == 0 && (!exactSearch || !first_pass))
		{
			// display if no results are found
			gameListItems.put("No Results Found", "20115");
			progress.dismiss();
		}
		else
		{
			// display results
			for (int i = 0; i < count; i++)
			{
				BoardGame boardGame = boardGameList.elementAt(i);
				if (boardGame.getYearPublished().equals("0"))
					gameListItems.put(boardGame.getName(), boardGame.getGameID());	
				else
					gameListItems.put(boardGame.getName()+" ("+boardGame.getYearPublished()+")", boardGame.getGameID());
			}
			progress.dismiss();
		}
		
        // display game list
        this.setListAdapter( new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>(gameListItems.keySet())) );
    }
	
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		// get the game id using the name as a key
		String game_id = gameListItems.get(this.getListAdapter().getItem(position));
		viewBoardGame(game_id);
	}  
	
	public void viewBoardGame(String game_id)
	{
		Intent myIntent = new Intent();
		myIntent.setClassName("com.boardgamegeek", "com.boardgamegeek.ViewBoardGame");
		myIntent.putExtra("GAME_ID", game_id);
		startActivity(myIntent);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        
        //inflate the menu from xml
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
       
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        	case R.id.reload:
        		getPreferences();
        		getBoardGameList();
                return true;
        	case R.id.settings:
        		startActivity(new Intent(this, Preferences.class));          
        		return true;
        	case R.id.credits:
                Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Special thanks to:");
                builder.setMessage(R.string.special_thanks); 
                builder.show();
                return true;
        }
        return false;
    }
    
    public void getPreferences()
    {
    	preferences = PreferenceManager.getDefaultSharedPreferences(this);
        exactSearch = preferences.getBoolean("exactSearch", true);
    }
}