package com.boardgamegeek.ui.dialog;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilter;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.ui.CollectionActivity;

public class CollectionStatusFilter {
	
	final String[] items = {Collection.STATUS_OWN, Collection.STATUS_FOR_TRADE,
			Collection.STATUS_PREORDERED, Collection.STATUS_PREVIOUSLY_OWNED,
			Collection.STATUS_WANT, Collection.STATUS_WISHLIST};
	boolean[] mSelected = new boolean[items.length];
	
	public void createDialog(final CollectionActivity activity) {		
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.menu_collection_status);
		builder.setMultiChoiceItems(items, mSelected, new DialogInterface.OnMultiChoiceClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which,
					boolean isChecked) {
				mSelected[which] = isChecked;
			}
		});
		builder.setNeutralButton("Or", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				CollectionFilter filter = new CollectionFilter();
				String name = "", selection = "";
				List<String> selectionArgs = new ArrayList<String>(mSelected.length);
								
				for(int i = 0; i < mSelected.length; i++) {					
					if(mSelected[i]) {
						name += items[i] + " | ";
						selection += items[i] + ">=? OR ";
						selectionArgs.add("1");
					}																			
				}
				
				//Remove the last trailing combiners
				name = name.substring(0, name.length()-2);
				selection = selection.substring(0, selection.length()-4);
				
		    	filter.
				name(name).
				selection(selection).
				selectionargs(selectionArgs.toArray(new String[selectionArgs.size()])).
				id(R.id.menu_collection_status);
				
				activity.addFilter(filter);
			}
		}).
		setPositiveButton("And", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				CollectionFilter filter = new CollectionFilter();
				String name = "", selection = "";
				List<String> selectionArgs = new ArrayList<String>(mSelected.length);
								
				for(int i = 0; i < mSelected.length; i++) {					
					if(mSelected[i]) {
						name += items[i] + " & ";
						selection += items[i] + ">=? AND ";
						selectionArgs.add("1");
					}																			
				}
				
				//Remove the last trailing combiners
				name = name.substring(0, name.length()-2);
				selection = selection.substring(0, selection.length()-5);
				
		    	filter.
				name(name).
				selection(selection).
				selectionargs(selectionArgs.toArray(new String[selectionArgs.size()])).
				id(R.id.menu_collection_status);
				
				activity.addFilter(filter);
			}
		})
		.setNegativeButton("Clear", new DialogInterface.OnClickListener() {
    		
			@Override
			public void onClick(DialogInterface dialog, int which) {
				CollectionFilter filter = new CollectionFilter().id(R.id.menu_collection_status);
				activity.removeFilter(filter);
			}        		
    	});
		
		AlertDialog alert = builder.create();
		alert.show();
	}
}
