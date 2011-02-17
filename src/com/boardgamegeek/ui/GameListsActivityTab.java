package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ExpandableListActivity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ExpandableListAdapter;
import android.widget.SimpleExpandableListAdapter;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.GamesDesigners;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;

public class GameListsActivityTab extends ExpandableListActivity implements AsyncQueryListener {
	private static final String TAG = "GameListsActivityTab";

	private static final String NAME = "NAME";
	private static final String COUNT = "COUNT";

	private Uri mDesignersUri;
	private NotifyingAsyncQueryHandler mHandler;

	private List<Map<String, String>> groupData;
	private List<List<Map<String, String>>> childData;
	private ExpandableListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setUiVariables();
		setUris();

		getContentResolver().registerContentObserver(mDesignersUri, true, new GameObserver(null));

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(mDesignersUri, Query.PROJECTION);
	}

	private void setUiVariables() {
		groupData = new ArrayList<Map<String, String>>();
		childData = new ArrayList<List<Map<String, String>>>();
	}
	
	private void setUris(){
		final Uri gameUri = getIntent().getData();
		final int gameId = Games.getGameId(gameUri);
		mDesignersUri = Games.buildDesignersUri(gameId);
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			List<String> designerNames = new ArrayList<String>();
			while (cursor.moveToNext()) {
				designerNames.add(cursor.getString(Query.DESIGNER_NAME));
			}
			createGroup(R.string.designers, designerNames);

			adapter = new SimpleExpandableListAdapter(this, groupData, R.layout.grouprow, new String[] { NAME, COUNT },
					new int[] { R.id.name, R.id.count }, childData, R.layout.childrow, new String[] { NAME },
					new int[] { R.id.name });
			setListAdapter(adapter);

		} finally {
			cursor.close();
		}
	}

	private void createGroup(int nameId, Collection<String> children) {
		Map<String, String> groupMap = new HashMap<String, String>();
		groupData.add(groupMap);
		groupMap.put(NAME, getResources().getString(nameId));
		groupMap.put(COUNT, "" + children.size());

		List<Map<String, String>> childrenMap = new ArrayList<Map<String, String>>();
		for (String child : children) {
			Map<String, String> childMap = new HashMap<String, String>();
			childrenMap.add(childMap);
			childMap.put(NAME, child);
		}
		childData.add(childrenMap);
	}

	class GameObserver extends ContentObserver {

		public GameObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			Log.d(TAG, "Caught changed URI = " + mDesignersUri);
			mHandler.startQuery(mDesignersUri, Query.PROJECTION);
		}
	}

	private interface Query {
		String[] PROJECTION = { GamesDesigners.DESIGNER_ID, Designers.DESIGNER_NAME };

		int DESIGNER_ID = 0;
		int DESIGNER_NAME = 1;
	}
}
