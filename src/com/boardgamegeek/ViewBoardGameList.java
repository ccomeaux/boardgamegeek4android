package com.boardgamegeek;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
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
	private final int ID_DIALOG_SEARCHING = 1;
	private final int ID_DIALOG_RETRY = 2;
    private final String DEBUG_TAG = "BoardGameGeek DEBUG:";
    Handler handler = new Handler();
	private SharedPreferences preferences;
    boolean exactSearch;
    boolean skipResults;
	boolean first_pass = true;
	
	@Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
    	Log.d(DEBUG_TAG, "onCreate");
    	
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
    	Log.d(DEBUG_TAG, "onResume");
    	
        //get preferences
        getPreferences();	
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
    	Log.d(DEBUG_TAG, "onSaveInstanceState");
    	removeDialogs();
    	super.onSaveInstanceState(outState);
    } 
    
	private void getBoardGameList()
	{
    	Log.d(DEBUG_TAG, "getBoardGameList");
    	
        // get the query from the intent
    	final String query = getIntent().getExtras().getString("QUERY");
    	
		// clear existing game list items
		gameListItems = new HashMap<String, String>();
		
		// display a progress dialog while fetching the game data
        if (first_pass)
        	showDialog(ID_DIALOG_SEARCHING);
        else
        	showDialog(ID_DIALOG_RETRY);
        
        new Thread() {
        	public void run()
        	{
        		try
        		{
        	    	Log.d(DEBUG_TAG, "PULLING XML");
        	    	
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
        			Log.d(DEBUG_TAG, "PULLING XML - Failed", e);
        		}
        		handler.post(updateResults);
            }
        }.start();
	}
	
	// override progress dialog
	@Override
	protected Dialog onCreateDialog(int id)
	{
		if(id == ID_DIALOG_SEARCHING)
		{
	    	Log.d(DEBUG_TAG, "ID_DIALOG_SEARCHING - Created");
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setTitle("Searching...");
			dialog.setMessage("Connecting to site...");
			dialog.setIndeterminate(true);
			dialog.setCancelable(true);
			return dialog;
		}
		else if(id == ID_DIALOG_RETRY)
		{
	    	Log.d(DEBUG_TAG, "ID_DIALOG_RETRY - Created");
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setTitle("No results...");
			dialog.setMessage("Trying wider search...");
			dialog.setIndeterminate(true);
			dialog.setCancelable(true);
			return dialog;
		}

		return super.onCreateDialog(id);
	}

	// remove dialog boxes
	protected void removeDialogs()
	{
		// remove progress dialog (if any)
		try { removeDialog(ID_DIALOG_SEARCHING); Log.d(DEBUG_TAG, "ID_DIALOG_SEARCHING - Removed"); }
		catch (Exception e) { Log.d(DEBUG_TAG, "ID_DIALOG_SEARCHING - Remove Failed", e); }
		try { removeDialog(ID_DIALOG_RETRY); Log.d(DEBUG_TAG, "ID_DIALOG_RETRY - Removed"); }
		catch (Exception e) { Log.d(DEBUG_TAG, "ID_DIALOG_RETRY - Remove Failed", e); } 
	}

	// get results from handler
    final Runnable cancelResults = new Runnable()
    {
        public void run()
        {
	    	Log.d(DEBUG_TAG, "ATTEMPTING TO REMOVE DIALOGS");
            removeDialogs();
        }
    };
    
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
	    	Log.d(DEBUG_TAG, "RETRY SEARCH");
			
	    	// remove progress dialog (if any)
			removeDialogs();
	    	
			// try again if exactsearch is on and no results were found
			first_pass = false;
	        getBoardGameList();
		}
		else if (count == 0 && (!exactSearch || !first_pass))
		{
	    	Log.d(DEBUG_TAG, "NO RESULTS");
	    	
			// display if no results are found
			gameListItems.put("No Results Found", "20115");

	    	// remove progress dialog (if any)
			removeDialogs(); 
		}
		else
		{
	    	Log.d(DEBUG_TAG, "DISPLAY RESULTS");
	    	
			// display results
			for (int i = 0; i < count; i++)
			{
				BoardGame boardGame = boardGameList.elementAt(i);
				if (boardGame.getYearPublished().equals("0"))
					gameListItems.put(boardGame.getName(), boardGame.getGameID());	
				else
					gameListItems.put(boardGame.getName()+" ("+boardGame.getYearPublished()+")", boardGame.getGameID());
			}

	    	// remove progress dialog (if any)
			removeDialogs();
		}
    
		// display game list
		this.setListAdapter( new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>(gameListItems.keySet())) );
		
        // skip directly  to game if only one result
		if (count == 1 && skipResults)
		{
			BoardGame boardGame = boardGameList.elementAt(0);
			viewBoardGame(boardGame.getGameID());
		}
	}
	
	// gets the game id from the list item when clicked
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		// get the game id using the name as a key
		String game_id = gameListItems.get(this.getListAdapter().getItem(position));
		viewBoardGame(game_id);
	}  
	
	// calls the board game intent
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
        		Dialog dialog = new Dialog(this);
        		dialog.setContentView(R.layout.dialog);
        		dialog.setTitle(R.string.thanks_title);
        		dialog.show();
                return true;
        }
        return false;
    }
    
    public void getPreferences()
    {
    	preferences = PreferenceManager.getDefaultSharedPreferences(this);
        exactSearch = preferences.getBoolean("exactSearch", true);
        skipResults = preferences.getBoolean("skipResults", true);
    }
}