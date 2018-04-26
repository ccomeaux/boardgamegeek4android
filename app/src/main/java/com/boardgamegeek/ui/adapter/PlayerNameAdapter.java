package com.boardgamegeek.ui.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
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
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PresentationUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class PlayerNameAdapter extends ArrayAdapter<PlayerNameAdapter.Result> implements Filterable {
	public static class Result {
		private final String title;
		private final String subtitle;
		private final String nickName;
		private final String username;
		private final String avatarUrl;

		public Result(String title, String subtitle, String nickName, String username, String avatarUrl) {
			this.title = title;
			this.subtitle = subtitle;
			this.nickName = nickName;
			this.username = username;
			this.avatarUrl = avatarUrl;
		}

		@Override
		public String toString() {
			return nickName;
		}

		public String getTitle() {
			return title;
		}

		public String getSubtitle() {
			return subtitle;
		}

		public String getUsername() {
			return username;
		}

		public String getAvatarUrl() {
			if (avatarUrl == null) return "";
			return avatarUrl;
		}
	}

	private static final ArrayList<Result> EMPTY_LIST = new ArrayList<>();
	private final ContentResolver resolver;
	private final LayoutInflater inflater;
	private final ArrayList<Result> resultList = new ArrayList<>();

	public PlayerNameAdapter(Context context) {
		super(context, R.layout.autocomplete_player, EMPTY_LIST);
		resolver = context.getContentResolver();
		inflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		return resultList.size();
	}

	@Override
	public Result getItem(int index) {
		if (index < resultList.size()) {
			return resultList.get(index);
		} else {
			return null;
		}
	}

	@NonNull
	@Override
	public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = inflater.inflate(R.layout.autocomplete_player, parent, false);
		}
		final Result result = getItem(position);
		if (result == null) {
			return view;
		}

		TextView titleView = view.findViewById(R.id.player_title);
		if (titleView != null) {
			if (TextUtils.isEmpty(result.getTitle())) {
				titleView.setVisibility(View.GONE);
			} else {
				titleView.setVisibility(View.VISIBLE);
				titleView.setText(result.getTitle());
			}
		}

		TextView subtitleView = view.findViewById(R.id.player_subtitle);
		if (subtitleView != null) {
			if (TextUtils.isEmpty(result.getSubtitle())) {
				subtitleView.setVisibility(View.GONE);
			} else {
				subtitleView.setVisibility(View.VISIBLE);
				subtitleView.setText(result.getSubtitle());
			}
		}

		view.setTag(result.getUsername());

		ImageView avatarView = view.findViewById(R.id.player_avatar);
		if (avatarView != null) {
			ImageUtils.loadThumbnail(avatarView, result.getAvatarUrl(), R.drawable.person_image_empty);
		}
		return view;
	}

	@NonNull
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

			HashSet<String> buddyUsernames = new HashSet<>();
			List<Result> buddies = queryBuddies(resolver, filter, buddyUsernames);

			ArrayList<Result> resultList = new ArrayList<>();
			if (buddies != null) {
				resultList.addAll(buddies);
			}

			List<Result> players = queryPlayerHistory(resolver, filter);

			for (Result player : players) {
				if (TextUtils.isEmpty(player.getUsername()) || !buddyUsernames.contains(player.getUsername()))
					resultList.add(player);
			}

			final FilterResults filterResults = new FilterResults();
			filterResults.values = resultList;
			filterResults.count = resultList.size();
			return filterResults;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			resultList.clear();
			if (results != null && results.count > 0) {
				resultList.addAll((ArrayList<Result>) results.values);
				notifyDataSetChanged();
			} else {
				notifyDataSetInvalidated();
			}
		}
	}

	private static final String PLAYER_SELECTION = PlayPlayers.NAME + " LIKE ?";
	private static final String[] PLAYER_PROJECTION = new String[] { PlayPlayers._ID, PlayPlayers.USER_NAME, PlayPlayers.NAME };
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

		Cursor cursor = resolver.query(Plays.buildPlayersByUniquePlayerUri(), PLAYER_PROJECTION, where, whereArgs, PlayPlayers.NAME);
		try {
			List<Result> results = new ArrayList<>();
			if (cursor != null) {
				cursor.moveToPosition(-1);
				while (cursor.moveToNext()) {
					String name = cursor.getString(PLAYER_NAME);
					String username = cursor.getString(PLAYER_USERNAME);

					results.add(new Result(name, username, name, username, null));
				}
			}
			return results;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private static final String BUDDY_SELECTION = Buddies.BUDDY_NAME + " LIKE ? OR " + Buddies.BUDDY_FIRSTNAME + " LIKE ? OR " + Buddies.BUDDY_LASTNAME + " LIKE ? OR " + Buddies.PLAY_NICKNAME + " LIKE ?";
	private static final String[] BUDDY_PROJECTION = { Buddies._ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME, Buddies.BUDDY_LASTNAME, Buddies.PLAY_NICKNAME, Buddies.AVATAR_URL };
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
		Cursor cursor = resolver.query(Buddies.CONTENT_URI, BUDDY_PROJECTION, where, whereArgs, Buddies.NAME_SORT);
		try {
			List<Result> results = new ArrayList<>();
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						String userName = cursor.getString(BUDDY_NAME);
						String firstName = cursor.getString(BUDDY_FIRST_NAME);
						String lastName = cursor.getString(BUDDY_LAST_NAME);
						String nickname = cursor.getString(BUDDY_PLAY_NICKNAME);
						String avatarUrl = cursor.getString(BUDDY_AVATAR_URL);
						String fullName = PresentationUtils.buildFullName(firstName, lastName);

						results.add(new Result(
							TextUtils.isEmpty(nickname) ? fullName : nickname,
							TextUtils.isEmpty(nickname) ? userName : fullName + " (" + userName + ")",
							TextUtils.isEmpty(nickname) ? firstName : nickname,
							userName,
							avatarUrl));
						usernames.add(userName);
					} while (cursor.moveToNext());
				}
			}
			return results;
		} finally {
			if (cursor != null) cursor.close();
		}
	}
}
