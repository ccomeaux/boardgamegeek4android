package com.boardgamegeek;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ExpandableListActivity;
import android.os.Bundle;
import android.widget.ExpandableListAdapter;
import android.widget.SimpleExpandableListAdapter;

public class BoardGameExtraTab extends ExpandableListActivity {

	private List<Map<String, String>> groupData;
	private List<List<Map<String, String>>> childData;
	private static final String NAME = "NAME";
	private static final String COUNT = "COUNT";
	private ExpandableListAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (ViewBoardGame.boardGame == null){
			return;
		}

		groupData = new ArrayList<Map<String, String>>();
		childData = new ArrayList<List<Map<String, String>>>();

		createGroup(R.string.designers, ViewBoardGame.boardGame.getDesignerNames());
		createGroup(R.string.artists, ViewBoardGame.boardGame.getArtistNames());
		createGroup(R.string.publishers, ViewBoardGame.boardGame.getPublisherNames());
		createGroup(R.string.categories, ViewBoardGame.boardGame.getCategoryNames());
		createGroup(R.string.mechanics, ViewBoardGame.boardGame.getMechanicNames());
		createGroup(R.string.expansions, ViewBoardGame.boardGame.getExpansionNames());

		adapter = new SimpleExpandableListAdapter(this, groupData,
				R.layout.grouprow, new String[] { NAME, COUNT }, new int[] {
						R.id.name, R.id.count }, childData, R.layout.childrow,
				new String[] { NAME, COUNT },
				new int[] { R.id.name, R.id.count });
		setListAdapter(adapter);
	}

	private void createGroup(int nameId, Collection<String> children) {
		Map<String, String> groupMap = new HashMap<String, String>();
		groupData.add(groupMap);
		groupMap.put(NAME, getResources().getString(nameId));
		groupMap.put(COUNT, "" + children.size());

		List<Map<String, String>> childrenMap = new ArrayList<Map<String, String>>();
		for (String designer : children) {
			Map<String, String> childMap = new HashMap<String, String>();
			childrenMap.add(childMap);
			childMap.put(NAME, designer);
		}
		childData.add(childrenMap);
	}
}
