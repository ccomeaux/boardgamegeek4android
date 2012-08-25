package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.http.client.HttpClient;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
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

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteHotnessHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.HotGame;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.widget.BezelImageView;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.UIUtils;

public class HotnessActivity extends ListActivity implements AbsListView.OnScrollListener {
	private static final String TAG = makeLogTag(HotnessActivity.class);

	private List<HotGame> mHotGames = new ArrayList<HotGame>();
	private BoardGameAdapter mAdapter;
	private final BlockingQueue<String> mThumbnailQueue = new ArrayBlockingQueue<String>(12);
	private ThumbnailTask mThumbnailTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_hotness);

		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);
		getListView().setOnScrollListener(this);

		mAdapter = new BoardGameAdapter();

		HotnessTask task = new HotnessTask();
		task.execute();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mThumbnailTask = new ThumbnailTask();
		mThumbnailTask.execute();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mThumbnailQueue.clear();
		mThumbnailTask.cancel(true);
	}

	@Override
	public void setTitle(CharSequence title) {
		UIUtils.setTitle(this, title);
	}

	public void onHomeClick(View v) {
		UIUtils.resetToHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

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
			mHttpClient = HttpUtils.createHttpClient(HotnessActivity.this, true);
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
			int count = result.getCount();
			if (result.isBggDown()) {
				UIUtils.showListMessage(HotnessActivity.this, R.string.bgg_down);
			} else if (count == 0) {
				UIUtils.showListMessage(HotnessActivity.this, R.string.hotness_no_results_details);
			} else {
				mHotGames = result.getResults();
				mAdapter = new BoardGameAdapter();
				setListAdapter(mAdapter);
			}
		}
	}

	private class ThumbnailTask extends AsyncTask<Void, Void, Void> {

		private ListView mView;

		@Override
		protected void onPreExecute() {
			mView = HotnessActivity.this.getListView();
		}

		@Override
		protected Void doInBackground(Void... params) {
			while (!isCancelled()) {
				try {
					String url = mThumbnailQueue.take();
					String[] parts = url.split("|");
					ImageCache.getDrawable(HotnessActivity.this, Games.buildThumbnailUri(Integer.valueOf(parts[0])),
						parts[1]);
					publishProgress();
				} catch (InterruptedException e) {
					LOGW(TAG, e.toString());
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			mView.invalidateViews();
		}
	}

	class BoardGameAdapter extends ArrayAdapter<HotGame> {
		private LayoutInflater mInflater;

		BoardGameAdapter() {
			super(HotnessActivity.this, R.layout.row_hotness, mHotGames);
			mInflater = getLayoutInflater();
		}

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
				holder.rank.setText("" + game.Rank);
				holder.name.setText(game.Name);
				if (game.YearPublished > 0) {
					holder.year.setText(String.valueOf(game.YearPublished));
				}
				holder.thumbnailUrl = game.ThumbnailUrl;

				Drawable thumbnail = ImageCache.getDrawable(HotnessActivity.this, Games.buildThumbnailUri(game.Id),
					holder.thumbnailUrl);
				if (thumbnail == null) {
					holder.thumbnail.setVisibility(View.GONE);
				} else {
					holder.thumbnailUrl = null;
					holder.thumbnail.setImageDrawable(thumbnail);
					holder.thumbnail.setVisibility(View.VISIBLE);
				}
			}

			return convertView;
		}
	}

	static class ViewHolder {
		TextView rank;
		TextView name;
		TextView year;
		TextView gameId;
		BezelImageView thumbnail;
		String thumbnailUrl;

		public ViewHolder(View view) {
			rank = (TextView) view.findViewById(R.id.rank);
			name = (TextView) view.findViewById(R.id.name);
			year = (TextView) view.findViewById(R.id.year);
			gameId = (TextView) view.findViewById(R.id.gameId);
			thumbnail = (BezelImageView) view.findViewById(R.id.thumbnail);
		}
	}

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// do nothing
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState == SCROLL_STATE_IDLE) {
			final int count = view.getChildCount();
			for (int i = 0; i < count; i++) {
				ViewHolder vh = (ViewHolder) view.getChildAt(i).getTag();
				if (vh.thumbnailUrl != null) {
					mThumbnailQueue.offer(vh.gameId + "|" + vh.thumbnailUrl);
				}
			}
		} else {
			mThumbnailQueue.clear();
		}
	}
}
