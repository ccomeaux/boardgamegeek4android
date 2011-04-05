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

	private List<Map<String, String>> mGroupData;
	private List<List<PollResult>> mChildData;
	private ExpandableListAdapter mAdapter;

	private NotifyingAsyncQueryHandler mHandler;

	private Uri mPollsUri;
	private PollsObserver mPollsObserver;

	private static final String ID = "ID";
	private static final String TITLE = "NAME";
	private static final String COUNT = "COUNT";

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
		final int gameId = Games.getGameId(gameUri);
		mPollsUri = Games.buildPollsUri(gameId);

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

					if (!poll.name.equals("suggested_numplayers")) {
						createPollGroup(poll.id, poll.title, poll.totalVotes, new ArrayList<PollResult>());
					}

					mHandler.startQuery(TOKEN_POLL_RESULTS, poll, GamePolls.buildPollResultsUri(poll.id),
							GamePollResultsQuery.PROJECTION, null, null, GamePollResults.DEFAULT_SORT);
				}

			} else if (token == TOKEN_POLL_RESULTS) {
				while (cursor.moveToNext()) {
					Poll poll = (Poll) cookie;
					String players = cursor.getString(GamePollResultsQuery.POLL_RESULTS_PLAYERS.ordinal());
					int id = cursor.getInt(GamePollResultsQuery._ID.ordinal());
					int groupPosition = -1;
					if (poll.name.equals("suggested_numplayers")) {
						createPollGroup(poll.id, poll.title + ": " + players, poll.totalVotes,
								new ArrayList<PollResult>());
						groupPosition = mGroupData.size() - 1;
					} else {
						groupPosition = getPollGroupPosition(poll.id);
					}

					// TODO: default sort doesn't work correctly for player age
					// due to it being saved as string
					mHandler.startQuery(TOKEN_POLL_RESULTS_RESULT, groupPosition,
							GamePollResults.buildPollResultsResultUri(id), GamePollResultsResultQuery.PROJECTION, null,
							null, GamePollResultsResult.DEFAULT_SORT);
				}

			} else if (token == TOKEN_POLL_RESULTS_RESULT) {
				int groupPosition = (Integer) cookie;

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

	/**
	 * @param pollId
	 * @return the groupPosition.
	 */
	private int getPollGroupPosition(int pollId) {

		for (int i = 0; i < mGroupData.size(); i++) {
			Map<String, String> entryMap = mGroupData.get(i);
			if (entryMap.get(ID).equals("" + pollId)) {
				return i;
			}
		}

		throw new IllegalArgumentException("pollId does not exist in Poll Groups");
	}

	private void createPollGroup(int pollId, String title, int numVotes, List<PollResult> resultList) {
		Map<String, String> groupMap = new HashMap<String, String>();
		mGroupData.add(groupMap);
		groupMap.put(ID, "" + pollId);
		groupMap.put(TITLE, title);
		groupMap.put(COUNT, "" + numVotes);
		mChildData.add(resultList);
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
