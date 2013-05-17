package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteHotnessHandler;
import com.boardgamegeek.model.HotGame;
import com.boardgamegeek.ui.widget.BezelImageView;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.actionmodecompat.ActionMode;
import com.boardgamegeek.util.actionmodecompat.MultiChoiceModeListener;

public class HotnessFragment extends BggListFragment implements AbsListView.OnScrollListener, MultiChoiceModeListener {
	// private static final String TAG = makeLogTag(HotnessActivity.class);
	private static final String KEY_HOT_GAMES = "HOT_GAMES";

	private List<HotGame> mHotGames = new ArrayList<HotGame>();
	private BoardGameAdapter mAdapter;
	private String mEmptyMessage;
	private LinkedHashSet<Integer> mSelectedPositions = new LinkedHashSet<Integer>();
	private MenuItem mLogPlayMenuItem;
	private MenuItem mLogPlayQuickMenuItem;
	private MenuItem mBggLinkMenuItem;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mEmptyMessage = getString(R.string.empty_hotness);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setListShown(false);

		if (savedInstanceState != null) {
			mHotGames = savedInstanceState.getParcelableArrayList(KEY_HOT_GAMES);
		}
		if (mHotGames == null || mHotGames.size() == 0) {
			HotnessTask task = new HotnessTask();
			task.execute();
		} else {
			showList();
		}

		ActionMode.setMultiChoiceMode(getListView(), getActivity(), this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mHotGames != null && mHotGames.size() > 0) {
			outState.putParcelableArrayList(KEY_HOT_GAMES, (ArrayList<? extends Parcelable>) mHotGames);
		}
	}

	@Override
	protected int getLoadingImage() {
		return R.drawable.thumbnail_image_empty;
	}

	@Override
	public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		HotGame game = (HotGame) mAdapter.getItem(position);
		ActivityUtils.launchGame(getActivity(), game.Id, game.Name);
	}

	private class HotnessTask extends AsyncTask<Void, Void, RemoteHotnessHandler> {

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;
		private RemoteHotnessHandler mHandler = new RemoteHotnessHandler();

		@Override
		protected void onPreExecute() {
			if (mHotGames == null) {
				mHotGames = new ArrayList<HotGame>();
			} else {
				mHotGames.clear();
			}
			mHttpClient = HttpUtils.createHttpClient(getActivity(), true);
			mExecutor = new RemoteExecutor(mHttpClient, getActivity());
		}

		@Override
		protected RemoteHotnessHandler doInBackground(Void... params) {
			String url = HttpUtils.constructHotnessUrl();
			mExecutor.safelyExecuteGet(url, mHandler);
			return mHandler;
		}

		@Override
		protected void onPostExecute(RemoteHotnessHandler result) {
			if (isAdded()) {
				mHotGames = result.getResults();
				if (result.hasError()) {
					mEmptyMessage = result.getErrorMessage();
				} else {
					mEmptyMessage = getString(R.string.empty_hotness);
				}
				showList();
			}
		}
	}

	private void showList() {
		if (mAdapter == null) {
			mAdapter = new BoardGameAdapter();
			setListAdapter(mAdapter);
		}

		setEmptyText(mEmptyMessage);
		// addAll not available until API11
		for (HotGame hotGame : mHotGames) {
			mAdapter.add(hotGame);
		}

		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	private class BoardGameAdapter extends ArrayAdapter<HotGame> {
		private LayoutInflater mInflater;

		BoardGameAdapter() {
			super(getActivity(), R.layout.row_hotness);
			mInflater = getActivity().getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_hotness, parent, false);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			HotGame game = mHotGames.get(position);
			if (game != null) {
				holder.name.setText(game.Name);
				if (game.YearPublished > 0) {
					holder.year.setText(String.valueOf(game.YearPublished));
				}
				holder.rank.setText(String.valueOf(game.Rank));
				getImageFetcher().loadAvatarImage(game.ThumbnailUrl, null, holder.thumbnail);
			}

			return convertView;
		}
	}

	private static class ViewHolder {
		TextView name;
		TextView year;
		TextView rank;
		BezelImageView thumbnail;

		public ViewHolder(View view) {
			name = (TextView) view.findViewById(R.id.name);
			year = (TextView) view.findViewById(R.id.year);
			rank = (TextView) view.findViewById(R.id.rank);
			thumbnail = (BezelImageView) view.findViewById(R.id.thumbnail);
		}
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.game_context, menu);
		mLogPlayMenuItem = menu.findItem(R.id.menu_log_play);
		mLogPlayQuickMenuItem = menu.findItem(R.id.menu_log_play_quick);
		mBggLinkMenuItem = menu.findItem(R.id.menu_link);
		mSelectedPositions.clear();
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
		if (checked) {
			mSelectedPositions.add(position);
		} else {
			mSelectedPositions.remove(position);
		}

		int count = mSelectedPositions.size();
		mode.setTitle(getResources().getQuantityString(R.plurals.msg_games_selected, count, count));

		mLogPlayMenuItem.setVisible(count == 1 && PreferencesUtils.showLogPlay(getActivity()));
		mLogPlayQuickMenuItem.setVisible(count == 1 && PreferencesUtils.showQuickLogPlay(getActivity()));
		mBggLinkMenuItem.setVisible(count == 1);
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		HotGame game = (HotGame) mAdapter.getItem(mSelectedPositions.iterator().next());
		switch (item.getItemId()) {
			case R.id.menu_log_play:
				mode.finish();
				ActivityUtils.logPlay(getActivity(), false, game.Id, game.Name);
				return true;
			case R.id.menu_log_play_quick:
				mode.finish();
				ActivityUtils.logPlay(getActivity(), true, game.Id, game.Name);
				return true;
			case R.id.menu_share:
				mode.finish();
				if (mSelectedPositions.size() == 1) {
					ActivityUtils.shareGame(getActivity(), game.Id, game.Name);
				} else {
					List<Pair<Integer, String>> games = new ArrayList<Pair<Integer, String>>(mSelectedPositions.size());
					for (int position : mSelectedPositions) {
						HotGame g = (HotGame) mAdapter.getItem(position);
						games.add(new Pair<Integer, String>(g.Id, g.Name));
					}
					ActivityUtils.shareGames(getActivity(), games);
				}
				return true;
			case R.id.menu_link:
				mode.finish();
				ActivityUtils.linkBgg(getActivity(), game.Id);
				return true;
		}
		return false;
	}
}
