package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Forum;
import com.boardgamegeek.model.ForumListResponse;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.widget.BggLoader;
import com.boardgamegeek.ui.widget.Data;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.UIUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class ForumsFragment extends BggListFragment implements LoaderManager.LoaderCallbacks<ForumsFragment.ForumsData> {
	private static final int FORUMS_LOADER_ID = 0;

	private int mGameId;
	private String mGameName;
	private ForumsAdapter mForumsAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		Uri uri = intent.getData();
		mGameId = Games.getGameId(uri);
		mGameName = intent.getStringExtra(ForumsUtils.KEY_GAME_NAME);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setEmptyText(getString(R.string.empty_forums));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(FORUMS_LOADER_ID, null, this);
	}

	@Override
	public Loader<ForumsData> onCreateLoader(int id, Bundle data) {
		return new ForumsLoader(getActivity(), mGameId);
	}

	@Override
	public void onLoadFinished(Loader<ForumsData> loader, ForumsData data) {
		if (getActivity() == null) {
			return;
		}

		if (mForumsAdapter == null) {
			mForumsAdapter = new ForumsAdapter(getActivity(), data.list());
			setListAdapter(mForumsAdapter);
		}
		mForumsAdapter.notifyDataSetChanged();

		if (data.hasError()) {
			setEmptyText(data.getErrorMessage());
		} else {
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
			restoreScrollState();
		}
	}

	@Override
	public void onLoaderReset(Loader<ForumsData> loader) {
	}

	@Override
	public void onListItemClick(ListView listView, View convertView, int position, long id) {
		if (mForumsAdapter.getItemViewType(position) == ForumsAdapter.ITEM_VIEW_TYPE_FORUM) {
			ForumViewHolder holder = (ForumViewHolder) convertView.getTag();
			if (holder != null) {
				Intent intent = new Intent(getActivity(), ForumActivity.class);
				intent.putExtra(ForumsUtils.KEY_FORUM_ID, holder.forumId);
				intent.putExtra(ForumsUtils.KEY_FORUM_TITLE, holder.forumTitle.getText());
				intent.putExtra(ForumsUtils.KEY_GAME_ID, mGameId);
				intent.putExtra(ForumsUtils.KEY_GAME_NAME, mGameName);
				startActivity(intent);
			}
		}
	}

	private static class ForumsLoader extends BggLoader<ForumsData> {
		private BggService mService;
		private int mGameId;

		public ForumsLoader(Context context, int gameId) {
			super(context);
			mService = Adapter.create();
			mGameId = gameId;
		}

		@Override
		public ForumsData loadInBackground() {
			ForumsData forums;
			try {
				if (mGameId == BggContract.INVALID_ID) {
					forums = new ForumsData(mService.forumList(BggService.FORUM_TYPE_REGION, BggService.FORUM_REGION_BOARDGAME));
				} else {
					forums = new ForumsData(mService.forumList(BggService.FORUM_TYPE_THING, mGameId));
				}
			} catch (Exception e) {
				forums = new ForumsData(e);
			}
			return forums;
		}
	}

	static class ForumsData extends Data<Forum> {
		private ForumListResponse mResponse;

		public ForumsData(ForumListResponse response) {
			mResponse = response;
		}

		public ForumsData(Exception e) {
			super(e);
		}

		@Override
		public List<Forum> list() {
			if (mResponse == null || mResponse.forums == null) {
				return new ArrayList<>();
			}
			return mResponse.forums;
		}
	}

	public static class ForumsAdapter extends ArrayAdapter<Forum> {
		public static final int ITEM_VIEW_TYPE_FORUM = 0;
		public static final int ITEM_VIEW_TYPE_HEADER = 1;

		private LayoutInflater mInflater;
		private Resources mResources;
		private NumberFormat mFormat = NumberFormat.getInstance();

		public ForumsAdapter(Activity activity, List<Forum> forums) {
			super(activity, R.layout.row_forum, forums);
			mInflater = activity.getLayoutInflater();
			mResources = activity.getResources();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Forum forum;
			try {
				forum = getItem(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}

			int type = getItemViewType(position);
			if (type == ITEM_VIEW_TYPE_FORUM) {
				ForumViewHolder holder;
				if (convertView == null) {
					convertView = mInflater.inflate(R.layout.row_forum, parent, false);
					holder = new ForumViewHolder(convertView);
					convertView.setTag(holder);
				} else {
					holder = (ForumViewHolder) convertView.getTag();
				}

				if (forum != null) {
					holder.forumId = forum.id;
					holder.forumTitle.setText(forum.title);
					holder.numThreads.setText(mResources.getQuantityString(R.plurals.forum_threads,
						forum.numberOfThreads, mFormat.format(forum.numberOfThreads)));
					holder.lastPost.setText(DateTimeUtils.formatForumDate(getContext(), forum.lastPostDate()));
					holder.lastPost.setVisibility((forum.lastPostDate() > 0) ? View.VISIBLE : View.GONE);
				}
				return convertView;
			} else {
				HeaderViewHolder holder;
				if (convertView == null) {
					convertView = mInflater.inflate(R.layout.row_header, parent, false);
					holder = new HeaderViewHolder(convertView);
					convertView.setTag(holder);
				} else {
					holder = (HeaderViewHolder) convertView.getTag();
				}
				if (forum != null) {
					holder.header.setText(forum.title);
				}
				return convertView;
			}
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			try {
				Forum forum = getItem(position);
				if (forum != null && forum.isHeader()) {
					return ITEM_VIEW_TYPE_HEADER;
				}
				return ITEM_VIEW_TYPE_FORUM;
			} catch (ArrayIndexOutOfBoundsException e) {
				return ITEM_VIEW_TYPE_FORUM;
			}
		}
	}

	static class ForumViewHolder {
		public int forumId;
		public TextView forumTitle;
		public TextView numThreads;
		public TextView lastPost;

		public ForumViewHolder(View view) {
			forumTitle = (TextView) view.findViewById(R.id.forum_title);
			numThreads = (TextView) view.findViewById(R.id.numthreads);
			lastPost = (TextView) view.findViewById(R.id.lastpost);
		}
	}

	static class HeaderViewHolder {
		public TextView header;

		public HeaderViewHolder(View view) {
			header = (TextView) view.findViewById(android.R.id.title);
		}
	}
}
