package com.boardgamegeek.ui.adapter;

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
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.HttpUtils;
import com.squareup.picasso.Picasso;

import timber.log.Timber;

public class PlayerNameAdapter extends ArrayAdapter<PlayerNameAdapter.Result> implements Filterable {
	public static class Result {
		private final String mName;
		private final String mSubtitle;
		private final String mUsername;
		private final String mAvatarUrl;

		public Result(String displayName, String subtitle, String username, String avatarUrl) {
			this.mName = displayName;
			this.mSubtitle = subtitle;
			this.mUsername = username;
			this.mAvatarUrl = avatarUrl;
		}

		@Override
		public String toString() {
			return mName;
		}
	}

	private static ArrayList<Result> EMPTY_LIST = new ArrayList<>();

	private final ContentResolver mResolver;
	private final LayoutInflater mInflater;
	private final ArrayList<Result> mResultList = new ArrayList<>();

	public PlayerNameAdapter(Context context) {
		super(context, R.layout.autocomplete_player, EMPTY_LIST);

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
			view = mInflater.inflate(R.layout.autocomplete_player, parent, false);
		}
		final Result result = getItem(position);
		if (result == null) {
			return view;
		}

		TextView titleView = (TextView) view.findViewById(R.id.player_title);
		if (titleView != null) {
			if (TextUtils.isEmpty(result.mName)) {
				titleView.setVisibility(View.GONE);
			} else {
				titleView.setVisibility(View.VISIBLE);
				titleView.setText(result.mName);
			}
		}

		TextView subtitleView = (TextView) view.findViewById(R.id.player_subtitle);
		if (subtitleView != null) {
			if (TextUtils.isEmpty(result.mSubtitle)) {
				subtitleView.setVisibility(View.GONE);
			} else {
				subtitleView.setVisibility(View.VISIBLE);
				subtitleView.setText(result.mSubtitle);
			}
		}

		view.setTag(result.mUsername);

		ImageView avatarView = (ImageView) view.findViewById(R.id.player_avatar);
		if (avatarView != null) {
			//noinspection SuspiciousNameCombination
			Picasso.with(getContext()).load(HttpUtils.ensureScheme(result.mAvatarUrl))
				.placeholder(R.drawable.person_image_empty).error(R.drawable.person_image_empty)
				.resizeDimen(R.dimen.dropdownitem_min_height, R.dimen.dropdownitem_min_height).centerCrop()
				.into(avatarView);
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

			HashSet<String> buddyUsernames = new HashSet<>();
			List<Result> buddies = queryBuddies(mResolver, filter, buddyUsernames);

			ArrayList<Result> resultList = new ArrayList<>();
			if (buddies != null) {
				resultList.addAll(buddies);
			}

			try {
				List<Result> players = playerQueryTask.get();

				for (Result player : players) {
					if (TextUtils.isEmpty(player.mUsername) || !buddyUsernames.contains(player.mUsername))
						resultList.add(player);
				}
			} catch (ExecutionException | InterruptedException e) {
				Timber.e(e, "Failed waiting for player query results.");
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

	private static final String PLAYER_SELECTION = PlayPlayers.NAME + " LIKE ?";
	private static final String[] PLAYER_PROJECTION = new String[] { PlayPlayers._ID, PlayPlayers.USER_NAME,
		PlayPlayers.NAME };
	private static final int PLAYER_USERNAME = 1;
	private static final int PLAYER_NAME = 2;

	private static List<Result> queryPlayerHistory(ContentResolver resolver, String input) {
		String where = null;
		String[] whereArgs = null;

		if (!TextUtils.isEmpty(input)) {
			where = PLAYER_SELECTION;
			String param = input + "%";
			whereArgs = new String[] { param };
		}

		Cursor c = resolver.query(Plays.buildPlayersByUniquePlayerUri(), PLAYER_PROJECTION, where, whereArgs,
			PlayPlayers.NAME);
		try {
			List<Result> results = new ArrayList<>();

			c.moveToPosition(-1);
			while (c.moveToNext()) {
				String name = c.getString(PLAYER_NAME);
				String username = c.getString(PLAYER_USERNAME);

				results.add(new Result(name, username, username, null));
			}
			return results;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	private static final String BUDDY_SELECTION = Buddies.BUDDY_NAME + " LIKE ? OR " + Buddies.BUDDY_FIRSTNAME
		+ " LIKE ? OR " + Buddies.BUDDY_LASTNAME + " LIKE ? OR " + Buddies.PLAY_NICKNAME + " LIKE ?";
	private static final String[] BUDDY_PROJECTION = { Buddies._ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME,
		Buddies.BUDDY_LASTNAME, Buddies.PLAY_NICKNAME, Buddies.AVATAR_URL };
	private static final int BUDDY_NAME = 1;
	private static final int BUDDY_FIRST_NAME = 2;
	private static final int BUDDY_LAST_NAME = 3;
	private static final int BUDDY_PLAY_NICKNAME = 4;
	private static final int BUDDY_AVATAR_URL = 5;

	private static List<Result> queryBuddies(ContentResolver resolver, String input, HashSet<String> usernames) {
		String where = null;
		String[] whereArgs = null;

		if (!TextUtils.isEmpty(input)) {
			where = BUDDY_SELECTION;
			String param = input + "%";
			whereArgs = new String[] { param, param, param, param };
		}
		Cursor c = resolver.query(Buddies.CONTENT_URI, BUDDY_PROJECTION, where, whereArgs, Buddies.NAME_SORT);
		try {
			List<Result> results = new ArrayList<>();

			c.moveToPosition(-1);
			while (c.moveToNext()) {
				String userName = c.getString(BUDDY_NAME);
				String firstName = c.getString(BUDDY_FIRST_NAME);
				String lastName = c.getString(BUDDY_LAST_NAME);
				String nickname = c.getString(BUDDY_PLAY_NICKNAME);
				String avatarUrl = c.getString(BUDDY_AVATAR_URL);
				String fullName = (firstName + " " + lastName).trim();

				results.add(new Result(TextUtils.isEmpty(nickname) ? fullName : nickname,
					TextUtils.isEmpty(nickname) ? userName : fullName + " (" + userName + ")", userName, avatarUrl));
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
