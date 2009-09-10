package com.boardgamegeek;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class BoardGamePollsTab extends ExpandableListActivity {

	private BoardGame boardGame;
	private List<Map<String, String>> groupData;
	private List<List<PollResult>> childData;
	private ExpandableListAdapter adapter;

	private static final String NAME = "NAME";
	private static final String COUNT = "COUNT";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		boardGame = ViewBoardGame.boardGame;
		if (boardGame == null) {
			return;
		}

		groupData = new ArrayList<Map<String, String>>();
		childData = new ArrayList<List<PollResult>>();

		for (Poll poll : boardGame.getPolls()) {
			if (poll.getResultsList().size() > 1) {
				for (PollResults results : poll.getResultsList()) {
					createGroup(poll.getTitle() + ": "
							+ results.getNumberOfPlayers(), poll
							.getTotalVotes(), results.getResultList());
				}
			} else {
				createGroup(poll.getTitle(), poll.getTotalVotes(), poll
						.getResultsList().get(0).getResultList());
			}
		}

		adapter = new PollAdapter();
		setListAdapter(adapter);
	}

	private void createGroup(String name, int votes, List<PollResult> resultList) {
		Map<String, String> groupMap = new HashMap<String, String>();
		groupData.add(groupMap);
		groupMap.put(NAME, name);
		groupMap.put(COUNT, "" + votes);
		childData.add(resultList);
	}

	class PollAdapter extends BaseExpandableListAdapter {

		private LayoutInflater inflater;

		public PollAdapter() {
			inflater = (LayoutInflater) BoardGamePollsTab.this
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return childData.get(groupPosition).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {

			View v;
			if (convertView == null) {
				v = inflater.inflate(R.layout.pollchildrow, parent, false);
			} else {
				v = convertView;
			}
			
			int max = Utility.parseInt(groupData.get(groupPosition).get(COUNT));
			PollResult result = childData.get(groupPosition).get(childPosition);
			String text;
			if (result.getLevel() != 0) {
				text = "" + result.getLevel();
			} else {
				text = result.getValue();
			}
			text += " = " + result.getNumberOfVotes();
			((TextView) v.findViewById(R.id.text)).setText(text);
			ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.bar);
			progressBar.setMax(max);
			progressBar.setProgress(result.getNumberOfVotes());

			return v;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return childData.get(groupPosition).size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return groupData.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return groupData.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@SuppressWarnings("unchecked")
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {

			View v;
			if (convertView == null) {
				v = inflater.inflate(R.layout.pollgrouprow, parent, false);
			} else {
				v = convertView;
			}

			Map<String, String> group = (Map<String, String>) getGroup(groupPosition);
			((TextView) v.findViewById(R.id.name)).setText(group.get(NAME));
			((TextView) v.findViewById(R.id.count)).setText(group.get(COUNT));

			return v;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}
	}

	// protected void onResume() {
	//
	// TextView tv = new TextView(this);
	//
	// String text = "";
	//
	// BoardGame bg = ViewBoardGame.boardGame;
	// for (Poll poll : bg.getPolls()) {
	// text += "\n" + poll.getTitle() + "(" + poll.getTotalVotes() + ")\n";
	// for (PollResults results : poll.getResultsList()) {
	// // text += "\t" + results.getNumberOfPlayers();
	// for (PollResult result : results.getResultList()) {
	// text += "\t" + result.getValue() + "("
	// + result.getNumberOfVotes() + ")\n";
	// }
	// }
	// }
	//
	// tv.setText(text);
	// }

	// class PollAdapter extends ArrayAdapter<Poll> {
	// PollAdapter() {
	// super(BoardGamePollsTab.this, android.R.layout.simple_list_item_1,
	// boardGame.getPolls());
	// }
	//
	// public View getView(int position, View convertView, ViewGroup parent) {
	// View row = convertView;
	// PollWrapper wrapper = null;
	//
	// if (row == null) {
	// LayoutInflater inflater = getLayoutInflater();
	// row = inflater.inflate(R.layout.row, null);
	// wrapper = new PollWrapper(row);
	// row.setTag(wrapper);
	// } else {
	// wrapper = (PollWrapper) row.getTag();
	// }
	//
	// wrapper.populateFrom(boardGame.getPolls().get(position));
	//
	// return row;
	// }
	// }
	//
	// class PollWrapper {
	// private View row = null;
	// private TextView name = null;
	// private TextView gameId = null;
	//
	// public PollWrapper(View row) {
	// this.row = row;
	// }
	//
	// void populateFrom(Poll poll) {
	// getName().setText(poll.getName());
	// getGameId().setText(poll.getTotalVotes());
	// }
	//
	// TextView getName() {
	// if (name == null) {
	// name = (TextView) row.findViewById(R.id.name);
	// }
	// return name;
	// }
	//
	// TextView getGameId() {
	// if (gameId == null) {
	// gameId = (TextView) row.findViewById(R.id.gameId);
	// }
	// return gameId;
	// }
	// }
}
