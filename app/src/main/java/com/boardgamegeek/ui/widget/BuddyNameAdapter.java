package com.boardgamegeek.ui.widget;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;

import timber.log.Timber;

public class BuddyNameAdapter extends ArrayAdapter<BuddyNameAdapter.Result> implements Filterable {
	public static class Result {
		private final String mUsername;

		public Result(String username) {
			this.mUsername = username;
		}

		@Override
		public String toString() {
			return mUsername;
		}
	}

	private static ArrayList<Result> EMPTY_LIST = new ArrayList<Result>();

	private final ContentResolver mResolver;
	private final LayoutInflater mInflater;
	private final ArrayList<Result> mResultList = new ArrayList<Result>();

	public BuddyNameAdapter(Context context) {
		super(context, R.layout.autocomplete_item, EMPTY_LIST);

		mResolver = context.getContentResolver();
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return mResultList.size();
	}

	@Override
	public Result getItem(int index) {
		if (index < mResultList.size()) {
			return mResultList.get(index);
		} else {
			return null;
		}
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = mInflater.inflate(R.layout.autocomplete_item, parent, false);
		}
		final Result result = getItem(position);
		if (result == null) {
			return view;
		}

		TextView titleView = (TextView) view.findViewById(R.id.autocomplete_item);
		if (titleView != null) {
			if (result.mUsername == null) {
				titleView.setVisibility(View.GONE);
			} else {
				titleView.setVisibility(View.VISIBLE);
				titleView.setText(result.mUsername);
			}
		}

		return view;
	}

	@Override
	public Filter getFilter() {
		return new PlayerFilter();
	}

	public class PlayerFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			final String filter = constraint == null ? "" : constraint.toString();
			if (filter.length() == 0) {
				return null;
			}

			AsyncTask<Void, Void, List<Result>> playerQueryTask = new AsyncTask<Void, Void, List<Result>>() {
				@Override
				protected List<Result> doInBackground(Void... params) {
					return queryPlayerHistory(mResolver, filter);
				}
			}.execute();

			HashSet<String> buddyUsernames = new HashSet<String>();
			List<Result> buddies = queryBuddies(mResolver, filter, buddyUsernames);

			ArrayList<Result> resultList = new ArrayList<Result>();
			if (buddies != null) {
				resultList.addAll(buddies);
			}

			try {
				List<Result> players = playerQueryTask.get();

				for (Result player : players) {
					if (TextUtils.isEmpty(player.mUsername) || !buddyUsernames.contains(player.mUsername))
						resultList.add(player);
				}
			} catch (ExecutionException e) {
				Timber.e("Failed waiting for player query results.", e);
			} catch (InterruptedException e) {
				Timber.e("Failed waiting for player query results.", e);
			}

			final FilterResults filterResults = new FilterResults();
			filterResults.values = resultList;
			filterResults.count = resultList.size();
			return filterResults;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			mResultList.clear();
			if (results != null && results.count > 0) {
				mResultList.addAll((ArrayList<Result>) results.values);
				notifyDataSetChanged();
			} else {
				notifyDataSetInvalidated();
			}
		}
	}

	private static final String PLAYER_SELECTION = PlayPlayers.USER_NAME + " LIKE ?";
	private static final String[] PLAYER_PROJECTION = new String[] { PlayPlayers._ID, PlayPlayers.USER_NAME };
	private static final int PLAYER_USERNAME = 1;

	private static List<Result> queryPlayerHistory(ContentResolver resolver, String input) {
		String where = null;
		String[] whereArgs = null;

		if (!TextUtils.isEmpty(input)) {
			where = PLAYER_SELECTION;
			String param = input + "%";
			whereArgs = new String[] { param };
		}

		Cursor c = resolver.query(Plays.buildPlayersByUniqueUserUri(), PLAYER_PROJECTION, where, whereArgs,
			PlayPlayers.NAME);
		try {
			List<Result> results = new ArrayList<Result>();

			c.moveToPosition(-1);
			while (c.moveToNext()) {
				String username = c.getString(PLAYER_USERNAME);

				results.add(new Result(username));
			}
			return results;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	private static final String BUDDY_SELECTION = Buddies.BUDDY_NAME + " LIKE ?";
	private static final String[] BUDDY_PROJECTION = { Buddies._ID, Buddies.BUDDY_NAME };
	private static final int BUDDY_NAME = 1;

	private static List<Result> queryBuddies(ContentResolver resolver, String input, HashSet<String> usernames) {
		String where = null;
		String[] whereArgs = null;

		if (!TextUtils.isEmpty(input)) {
			where = BUDDY_SELECTION;
			String param = input + "%";
			whereArgs = new String[] { param };
		}
		Cursor c = resolver.query(Buddies.CONTENT_URI, BUDDY_PROJECTION, where, whereArgs, Buddies.NAME_SORT);
		try {
			List<Result> results = new ArrayList<Result>();

			c.moveToPosition(-1);
			while (c.moveToNext()) {
				String userName = c.getString(BUDDY_NAME);

				results.add(new Result(userName));
				usernames.add(userName);
			}
			return results;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}
}