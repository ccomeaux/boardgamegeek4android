package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteHotnessHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.HotGame;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.widget.BezelImageView;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.ImageFetcher;
import com.boardgamegeek.util.UIUtils;

public class HotnessFragment extends SherlockListFragment implements AbsListView.OnScrollListener {
	private static final String TAG = makeLogTag(HotnessActivity.class);

	private List<HotGame> mHotGames = new ArrayList<HotGame>();
	private BoardGameAdapter mAdapter;
	private ImageFetcher mImageFetcher;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mImageFetcher = UIUtils.getImageFetcher(getActivity());
		mImageFetcher.setLoadingImage(R.drawable.thumbnail_image_empty);
		mImageFetcher.setImageSize((int) getResources().getDimension(R.dimen.thumbnail_list_size));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setListShown(false);

		HotnessTask task = new HotnessTask();
		task.execute();
	}

	@Override
	public void onPause() {
		super.onPause();
		mImageFetcher.flushCache();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mImageFetcher.closeCache();
	}

	@Override
	public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// do nothing
	}

	@Override
	public void onScrollStateChanged(AbsListView listView, int scrollState) {
		// Pause disk cache access to ensure smoother scrolling
		if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
			|| scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
			mImageFetcher.setPauseWork(true);
		} else {
			mImageFetcher.setPauseWork(false);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		HotGame game = (HotGame) mAdapter.getItem(position);
		viewBoardGame(game.Id, game.Name);
	}

	private void viewBoardGame(int gameId, String gameName) {
		final Uri gameUri = Games.buildGameUri(gameId);
		final Intent intent = new Intent(Intent.ACTION_VIEW, gameUri);
		intent.putExtra(BoardgameActivity.KEY_GAME_NAME, gameName);
		startActivity(intent);
	}

	private class HotnessTask extends AsyncTask<Void, Void, RemoteHotnessHandler> {

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;
		private RemoteHotnessHandler mHandler = new RemoteHotnessHandler();

		@Override
		protected void onPreExecute() {
			mHotGames.clear();
			mHttpClient = HttpUtils.createHttpClient(getActivity(), true);
			mExecutor = new RemoteExecutor(mHttpClient, null);
		}

		@Override
		protected RemoteHotnessHandler doInBackground(Void... params) {
			String url = HttpUtils.constructHotnessUrl();
			try {
				mExecutor.executeGet(url, mHandler);
			} catch (HandlerException e) {
				LOGE(TAG, "getting hotness", e);
			}
			return mHandler;
		}

		@Override
		protected void onPostExecute(RemoteHotnessHandler result) {
			if (mAdapter == null) {
				mAdapter = new BoardGameAdapter();
				setListAdapter(mAdapter);
			}

			if (result.isBggDown()) {
				setEmptyText(getString(R.string.bgg_down));
			} else if (result.getCount() == 0) {
				setEmptyText(getString(R.string.hotness_no_results_details));
			} else {
				mHotGames = result.getResults();
				mAdapter.addAll(mHotGames);
			}

			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
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
				mImageFetcher.loadAvatarImage(game.ThumbnailUrl, null, holder.thumbnail);
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
}
