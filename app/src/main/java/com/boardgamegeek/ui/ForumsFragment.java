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
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.SafeResponse;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;
import retrofit2.Call;

public class ForumsFragment extends BggListFragment implements LoaderManager.LoaderCallbacks<SafeResponse<ForumListResponse>> {
	private static final int LOADER_ID = 0;

	private int gameId;
	private String gameName;
	private ForumsAdapter adapter;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		Uri uri = intent.getData();
		gameId = Games.getGameId(uri);
		gameName = intent.getStringExtra(ActivityUtils.KEY_GAME_NAME);
	}

	@Override
	@DebugLog
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setEmptyText(getString(R.string.empty_forums));
	}

	@Override
	@DebugLog
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	@DebugLog
	protected boolean padTop() {
		return (gameId != BggContract.INVALID_ID);
	}

	@Override
	@DebugLog
	protected boolean dividerShown() {
		return true;
	}

	@Override
	@DebugLog
	public Loader<SafeResponse<ForumListResponse>> onCreateLoader(int id, Bundle data) {
		return new ForumsLoader(getActivity(), gameId);
	}

	@Override
	@DebugLog
	public void onLoadFinished(Loader<SafeResponse<ForumListResponse>> loader, SafeResponse<ForumListResponse> data) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new ForumsAdapter(getActivity(),
				data.getBody() == null ?
					new ArrayList<Forum>() :
					data.getBody().getForums());
			setListAdapter(adapter);
		}
		initializeTimeBasedUi();

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
	@DebugLog
	public void onLoaderReset(Loader<SafeResponse<ForumListResponse>> loader) {
	}

	@Override
	@DebugLog
	public void onListItemClick(ListView listView, View convertView, int position, long id) {
		if (adapter.getItemViewType(position) == ForumsAdapter.ITEM_VIEW_TYPE_FORUM) {
			ForumViewHolder holder = (ForumViewHolder) convertView.getTag();
			if (holder != null) {
				Intent intent = new Intent(getActivity(), ForumActivity.class);
				intent.putExtra(ActivityUtils.KEY_FORUM_ID, holder.forumId);
				intent.putExtra(ActivityUtils.KEY_FORUM_TITLE, holder.forumTitleView.getText());
				intent.putExtra(ActivityUtils.KEY_GAME_ID, gameId);
				intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
				startActivity(intent);
			}
		}
	}

	private static class ForumsLoader extends BggLoader<SafeResponse<ForumListResponse>> {
		private final BggService bggService;
		private final int gameId;

		@DebugLog
		public ForumsLoader(Context context, int gameId) {
			super(context);
			bggService = Adapter.createForXml();
			this.gameId = gameId;
		}

		@Override
		@DebugLog
		public SafeResponse<ForumListResponse> loadInBackground() {
			Call<ForumListResponse> call;
			if (gameId == BggContract.INVALID_ID) {
				call = bggService.forumList(BggService.FORUM_TYPE_REGION, BggService.FORUM_REGION_BOARDGAME);
			} else {
				call = bggService.forumList(BggService.FORUM_TYPE_THING, gameId);
			}
			return new SafeResponse<>(call);
		}
	}

	@Override
	@DebugLog
	protected void updateTimeBasedUi() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	public static class ForumsAdapter extends ArrayAdapter<Forum> {
		public static final int ITEM_VIEW_TYPE_FORUM = 0;
		public static final int ITEM_VIEW_TYPE_HEADER = 1;
		private final LayoutInflater inflater;
		private final Resources resources;

		@DebugLog
		public ForumsAdapter(Activity activity, List<Forum> forums) {
			super(activity, R.layout.row_forum, forums);
			inflater = activity.getLayoutInflater();
			resources = activity.getResources();
		}

		@Override
		@DebugLog
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
					convertView = inflater.inflate(R.layout.row_forum, parent, false);
					holder = new ForumViewHolder(convertView);
					convertView.setTag(holder);
				} else {
					holder = (ForumViewHolder) convertView.getTag();
				}

				if (forum != null) {
					holder.forumId = forum.id;
					holder.forumTitleView.setText(forum.title);
					holder.numberOfThreadsView.setText(resources.getQuantityString(R.plurals.forum_threads, forum.numberOfThreads, forum.numberOfThreads));
					holder.lastPostDateView.setText(getContext().getString(R.string.forum_last_post, DateTimeUtils.formatForumDate(getContext(), forum.lastPostDate())));
					holder.lastPostDateView.setVisibility((forum.lastPostDate() > 0) ? View.VISIBLE : View.GONE);
				}
				return convertView;
			} else {
				HeaderViewHolder holder;
				if (convertView == null) {
					convertView = inflater.inflate(R.layout.row_header, parent, false);
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
		@DebugLog
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		@DebugLog
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

	@SuppressWarnings("unused")
	static class ForumViewHolder {
		public int forumId;
		@BindView(R.id.forum_title) TextView forumTitleView;
		@BindView(R.id.number_of_threads) TextView numberOfThreadsView;
		@BindView(R.id.last_post_date) TextView lastPostDateView;

		public ForumViewHolder(View view) {
			ButterKnife.bind(this, view);
		}
	}

	@SuppressWarnings("unused")
	static class HeaderViewHolder {
		@BindView(android.R.id.title) TextView header;

		public HeaderViewHolder(View view) {
			ButterKnife.bind(this, view);
		}
	}
}
