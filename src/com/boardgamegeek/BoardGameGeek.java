package com.boardgamegeek;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class BoardGameGeek extends Activity
{	
	@Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        
        // allow type-to-search
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        
       	// call the xml layout
        this.setContentView(R.layout.main);
        
        // invoke the search UI
        onSearchRequested();
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
        	case R.id.search:
        		onSearchRequested();
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
}