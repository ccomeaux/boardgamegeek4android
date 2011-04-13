package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class GamePollsActivityTab extends ExpandableListActivity implements AsyncQueryListener {

	private static int TOKEN_POLLS = 1;
	private static int TOKEN_POLL_RESULTS = 2;
	private static int TOKEN_POLL_RESULTS_RESULT = 3;

	private static final String ID = "ID";
	private static final String TITLE = "NAME";
	private static final String COUNT = "COUNT";
	private static final String PLAYERS = "PLAYERS";

	private List<Map<String, String>> mGroupData;
	private List<List<PollResult>> mChildData;
	private ExpandableListAdapter mAdapter;

	private NotifyingAsyncQueryHandler mHandler;

	private Uri mPollsUri;
	private PollsObserver mPollsObserver;

	private int mGameId;

	private class Poll {
		int id;
		String name;
		String title;
		int totalVotes;
	}

	private class PollResult {
		int id;
		int level;
		String value;
		int numberOfVotes;
	}

	@Override
	protected void onStart() {
		super.onStart();
		getContentResolver().registerContentObserver(mPollsUri, true, mPollsObserver);
	}

	@Override
	protected void onStop() {
		super.onStop();
		getContentResolver().unregisterContentObserver(mPollsObserver);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setAndObserveUris();
		startQueries();

		mGroupData = new ArrayList<Map<String, String>>();
		mChildData = new ArrayList<List<PollResult>>();

		mAdapter = new PollAdapter();
		setListAdapter(mAdapter);
	}

	private void startQueries() {
		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(TOKEN_POLLS, null, mPollsUri, GamePollsQuery.PROJECTION, null, null, GamePolls.DEFAULT_SORT);
	}

	private void setAndObserveUris() {
		final Uri gameUri = getIntent().getData();
		mGameId = Games.getGameId(gameUri);
		mPollsUri = Games.buildPollsUri(mGameId);

		mPollsObserver = new PollsObserver(null);
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (token == TOKEN_POLLS) {
				while (cursor.moveToNext()) {
					Poll poll = new Poll();
					poll.id = cursor.getInt(GamePollsQuery._ID.ordinal());
					poll.name = cursor.getString(GamePollsQuery.POLL_NAME.ordinal());
					poll.title = cursor.getString(GamePollsQuery.POLL_TITLE.ordinal());
					poll.totalVotes = cursor.getInt(GamePollsQuery.POLL_TOTAL_VOTES.ordinal());

					mHandler.startQuery(TOKEN_POLL_RESULTS, poll, Games.buildPollResultsUri(mGameId, poll.name),
							GamePollResultsQuery.PROJECTION, null, null, GamePollResults.DEFAULT_SORT);
				}

			} else if (token == TOKEN_POLL_RESULTS) {
				Poll poll = (Poll) cookie;
				while (cursor.moveToNext()) {
					String key = cursor.getString(GamePollResultsQuery.POLL_RESULTS_KEY.ordinal());
					String players = cursor.getString(GamePollResultsQuery.POLL_RESULTS_PLAYERS.ordinal());
					int groupPosition = createPollGroup(poll.id, poll.title, poll.totalVotes, players);

					mHandler.startQuery(TOKEN_POLL_RESULTS_RESULT, groupPosition,
							Games.buildPollResultsResultUri(mGameId, poll.name, key),
							GamePollResultsResultQuery.PROJECTION, null, null, GamePollResultsResult.DEFAULT_SORT);
				}

			} else if (token == TOKEN_POLL_RESULTS_RESULT) {
				int groupPosition = (Integer) cookie;
				mChildData.get(groupPosition).clear();
				while (cursor.moveToNext()) {
					PollResult result = new PollResult();
					result.id = cursor.getInt(GamePollResultsResultQuery.POLL_RESULTS_ID.ordinal());
					result.level = cursor.getInt(GamePollResultsResultQuery.POLL_RESULTS_LEVEL.ordinal());
					result.numberOfVotes = cursor.getInt(GamePollResultsResultQuery.POLL_RESULTS_VOTES.ordinal());
					result.value = cursor.getString(GamePollResultsResultQuery.POLL_RESULTS_VALUE.ordinal());
					mChildData.get(groupPosition).add(result);
				}
			}

			mAdapter = new PollAdapter();
			setListAdapter(mAdapter);

			// TODO: is a restore of the list needed for this?
			// restoreListView();
		} finally {
			cursor.close();
		}
	}

	private int getPollGroupPosition(int pollId, String players) {

		for (int i = 0; i < mGroupData.size(); i++) {
			Map<String, String> entryMap = mGroupData.get(i);
			if (entryMap.get(ID).equals("" + pollId) && players.equals(entryMap.get(PLAYERS))) {
				return i;
			}
		}

		return -1;
	}

	private int createPollGroup(int pollId, String title, int numVotes, String players) {

		Map<String, String> groupMap = null;
		int position = getPollGroupPosition(pollId, players);
		
		if (position == -1) {
			groupMap = new HashMap<String, String>();
			mGroupData.add(groupMap);
			mChildData.add(new ArrayList<PollResult>());
		} else {
			groupMap = mGroupData.get(position);
			mChildData.get(position).clear();
		}

		String displayTitle = title;
		if (!TextUtils.isEmpty(players) && !"X".equals(players)) {
			displayTitle += ": " + players;
		}
		
		groupMap.put(ID, "" + pollId);
		groupMap.put(TITLE, displayTitle);
		groupMap.put(COUNT, "" + numVotes);
		groupMap.put(PLAYERS, players);
		
		return position;
	}

	private class PollsObserver extends ContentObserver {

		public PollsObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			mHandler.startQuery(TOKEN_POLLS, null, mPollsUri, GamePollsQuery.PROJECTION, null, null,
					GamePolls.DEFAULT_SORT);
		}
	}

	class PollAdapter extends BaseExpandableListAdapter {

		private LayoutInflater mInflater;

		public PollAdapter() {
			mInflater = (LayoutInflater) GamePollsActivityTab.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public Object getChild(int groupPosition, int childPosition) {
			return mChildData.get(groupPosition).get(childPosition);
		}

		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@SuppressWarnings("unchecked")
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {

			View v;
			if (convertView == null) {
				v = mInflater.inflate(R.layout.pollchildrow, parent, false);
			} else {
				v = convertView;
			}

			Map<String, String> group = (Map<String, String>) getGroup(groupPosition);
			PollResult result = mChildData.get(groupPosition).get(childPosition);

			// TODO: ellipsize instead of putting level as text
			String text;
			if (result.level != 0) {
				text = "Level " + result.level;
			} else {
				text = result.value;
			}
			((TextView) v.findViewById(R.id.text)).setText(text);
			((TextView) v.findViewById(R.id.count)).setText("" + result.numberOfVotes + " / " + group.get(COUNT));

			int max = Integer.parseInt(mGroupData.get(groupPosition).get(COUNT));
			ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.bar);
			progressBar.setMax(max);
			progressBar.setProgress(result.numberOfVotes);

			return v;
		}

		public int getChildrenCount(int groupPosition) {
			return mChildData.get(groupPosition).size();
		}

		public Object getGroup(int groupPosition) {
			return mGroupData.get(groupPosition);
		}

		public int getGroupCount() {
			return mGroupData.size();
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@SuppressWarnings("unchecked")
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

			View v;
			if (convertView == null) {
				v = mInflater.inflate(R.layout.pollgrouprow, parent, false);
			} else {
				v = convertView;
			}

			Map<String, String> group = (Map<String, String>) getGroup(groupPosition);
			((TextView) v.findViewById(R.id.name)).setText(group.get(TITLE));

			return v;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}
	}

	private enum GamePollsQuery {
		_ID(GamePolls._ID),
		POLL_NAME(GamePolls.POLL_NAME),
		POLL_TITLE(GamePolls.POLL_TITLE),
		POLL_TOTAL_VOTES(GamePolls.POLL_TOTAL_VOTES);

		public static String[] PROJECTION = UIUtils.projectionFromEnums(GamePollsQuery.values());

		private String mColumnName;

		GamePollsQuery(String columnName) {
			mColumnName = columnName;
		}

		@Override
		public String toString() {
			return mColumnName;
		}
	}

	private enum GamePollResultsQuery {
		_ID(GamePollResults._ID),
		POLL_ID(GamePollResults.POLL_ID),
		POLL_RESULTS_KEY(GamePollResults.POLL_RESULTS_KEY),
		POLL_RESULTS_PLAYERS(GamePollResults.POLL_RESULTS_PLAYERS);

		public static String[] PROJECTION = UIUtils.projectionFromEnums(GamePollResultsQuery.values());

		private String mColumnName;

		GamePollResultsQuery(String columnName) {
			mColumnName = columnName;
		}

		@Override
		public String toString() {
			return mColumnName;
		}
	}

	private enum GamePollResultsResultQuery {
		POLL_RESULTS_ID(GamePollResultsResult.POLL_RESULTS_ID),
		POLL_RESULTS_LEVEL(GamePollResultsResult.POLL_RESULTS_RESULT_LEVEL),
		POLL_RESULTS_VALUE(GamePollResultsResult.POLL_RESULTS_RESULT_VALUE),
		POLL_RESULTS_VOTES(GamePollResultsResult.POLL_RESULTS_RESULT_VOTES);

		public static String[] PROJECTION = UIUtils.projectionFromEnums(GamePollResultsResultQuery.values());

		private String mColumnName;

		GamePollResultsResultQuery(String columnName) {
			mColumnName = columnName;
		}

		@Override
		public String toString() {
			return mColumnName;
		}
	}
}
