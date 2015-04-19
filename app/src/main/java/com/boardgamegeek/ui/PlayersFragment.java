package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.sorter.PlayersSorter;
import com.boardgamegeek.sorter.PlayersSorterFactory;
import com.boardgamegeek.ui.model.Player;
import com.boardgamegeek.util.UIUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import timber.log.Timber;

public class PlayersFragment extends StickyHeaderListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String STATE_SELECTED_NAME = "selectedName";
	private static final String STATE_SELECTED_USERNAME = "selectedUsername";
	private static final String STATE_SORT_TYPE = "sortType";
	private static final int TOKEN = 0;
	private PlayersAdapter mAdapter;
	private String mSelectedName;
	private String mSelectedUsername;
	private PlayersSorter mSorter;

	public interface Callbacks {
		boolean onPlayerSelected(String name, String username);

		void onPlayerCountChanged(int count);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public boolean onPlayerSelected(String name, String username) {
			return true;
		}

		@Override
		public void onPlayerCountChanged(int count) {
		}
	};

	private Callbacks mCallbacks = sDummyCallbacks;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof Callbacks)) {
			throw new ClassCastException("Activity must implement fragment's callbacks.");
		}
		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			mSelectedName = savedInstanceState.getString(STATE_SELECTED_NAME);
			mSelectedUsername = savedInstanceState.getString(STATE_SELECTED_USERNAME);
		}
	}


	@DebugLog
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		int sortType = PlayersSorterFactory.TYPE_DEFAULT;
		if (savedInstanceState != null) {
			sortType = savedInstanceState.getInt(STATE_SORT_TYPE);
		}
		mSorter = PlayersSorterFactory.create(sortType, getActivity());

		setEmptyText(getString(R.string.empty_players));
		requery();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (!TextUtils.isEmpty(mSelectedName) || !TextUtils.isEmpty(mSelectedUsername)) {
			outState.putString(STATE_SELECTED_NAME, mSelectedName);
			outState.putString(STATE_SELECTED_USERNAME, mSelectedUsername);
		}
		outState.putInt(STATE_SORT_TYPE, mSorter.getType());
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onListItemClick(View v, int position, long id) {
		final String name = (String) v.getTag(R.id.name);
		final String username = (String) v.getTag(R.id.username);
		if (mCallbacks.onPlayerSelected(name, username)) {
			setSelectedPlayer(name, username);
		}
	}

	@Override
	protected boolean padBottom() {
		return true;
	}

	@DebugLog
	public void requery() {
		getLoaderManager().restartLoader(TOKEN, getArguments(), this);
	}

	public int getSort() {
		return mSorter.getType();
	}


	@DebugLog
	public void setSort(int sort) {
		if (mSorter.getType() != sort) {
			mSorter = PlayersSorterFactory.create(sort, getActivity());
			if (mSorter == null) {
				mSorter = PlayersSorterFactory.create(PlayersSorterFactory.TYPE_DEFAULT, getActivity());
			}
			requery();
		}
	}

	public void setSelectedPlayer(String name, String username) {
		mSelectedName = name;
		mSelectedUsername = username;
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		return new CursorLoader(getActivity(), Plays.buildPlayersByUniquePlayerUri(),
			Player.PROJECTION, null, null, mSorter.getOrderByClause());
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		int token = loader.getId();
		if (token == TOKEN) {
			if (mAdapter == null) {
				mAdapter = new PlayersAdapter(getActivity());
				setListAdapter(mAdapter);
			}
			mAdapter.changeCursor(cursor);
			mCallbacks.onPlayerCountChanged(cursor.getCount());
			restoreScrollState();
		} else {
			Timber.d("Query complete, Not Actionable: " + token);
			cursor.close();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.changeCursor(null);
	}

	public class PlayersAdapter extends CursorAdapter implements StickyListHeadersAdapter {
		private LayoutInflater mInflater;

		@DebugLog
		public PlayersAdapter(Context context) {
			super(context, null, false);
			mInflater = LayoutInflater.from(context);
		}

		@DebugLog
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(R.layout.row_players_player, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@DebugLog
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();

			Player player = Player.fromCursor(cursor);

			UIUtils.setActivatedCompat(view,
				player.getName().equals(mSelectedName) && player.getUsername().equals(mSelectedUsername));

			holder.name.setText(player.getName());
			holder.username.setText(player.getUsername());
			holder.username.setVisibility(TextUtils.isEmpty(player.getUsername()) ? View.GONE : View.VISIBLE);
			holder.quantity.setText(getResources().getQuantityString(R.plurals.plays, player.getPlayCount(), player.getPlayCount()));

			view.setTag(R.id.name, player.getName());
			view.setTag(R.id.username, player.getUsername());
		}

		@DebugLog
		@Override
		public long getHeaderId(int position) {
			return mSorter.getHeaderId(getCursor(), position);
		}

		@DebugLog
		@Override
		public View getHeaderView(int position, View convertView, ViewGroup parent) {
			HeaderViewHolder holder;
			if (convertView == null) {
				holder = new HeaderViewHolder();
				convertView = mInflater.inflate(R.layout.row_header, parent, false);
				holder.text = (TextView) convertView.findViewById(android.R.id.title);
				convertView.setTag(holder);
			} else {
				holder = (HeaderViewHolder) convertView.getTag();
			}
			holder.text.setText(getHeaderText(position));
			return convertView;
		}


		@DebugLog
		private String getHeaderText(int position) {
			return mSorter.getHeaderText(getCursor(), position);
		}

		class ViewHolder {
			@InjectView(android.R.id.title) TextView name;
			@InjectView(android.R.id.text1) TextView username;
			@InjectView(android.R.id.text2) TextView quantity;

			public ViewHolder(View view) {
				ButterKnife.inject(this, view);
			}
		}

		class HeaderViewHolder {
			TextView text;
		}
	}
}
