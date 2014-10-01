package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Geeklist;
import com.boardgamegeek.model.GeeklistItem;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.widget.BggLoader;
import com.boardgamegeek.ui.widget.Data;
import com.boardgamegeek.util.BoardGameGeekConstants;
import com.boardgamegeek.util.GeeklistUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class GeeklistFragment extends BggListFragment implements LoaderManager.LoaderCallbacks<GeeklistFragment.GeeklistData> {
	private static final int GEEKLIST_LOADER_ID = 99103;

	private GeeklistAdapter mGeeklistAdapter;
	private int mGeeklistId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGeeklistId = intent.getIntExtra(GeeklistUtils.KEY_GEEKLIST_ID, BggContract.INVALID_ID);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		final ListView listView = getListView();
		listView.setSmoothScrollbarEnabled(false);
		listView.setSelector(android.R.color.transparent);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_geeklist));
	}

	@Override
	public void onResume() {
		super.onResume();
		// If this is called in onActivityCreated as recommended, the loader is finished twice
		getLoaderManager().initLoader(GEEKLIST_LOADER_ID, null, this);
	}

	@Override
	public Loader<GeeklistData> onCreateLoader(int id, Bundle data) {
		return new GeeklistLoader(getActivity(), mGeeklistId);
	}

	@Override
	public void onLoadFinished(Loader<GeeklistData> loader, GeeklistData data) {
		if (getActivity() == null) {
			return;
		}

		if (mGeeklistAdapter == null) {
			mGeeklistAdapter = new GeeklistAdapter(getActivity(), data.list());
			setListAdapter(mGeeklistAdapter);
		}
		mGeeklistAdapter.notifyDataSetChanged();

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
	public void onLoaderReset(Loader<GeeklistData> loader) {
	}

    private static class GeeklistLoader extends BggLoader<GeeklistData> {
		private BggService mService;
		private int mGeeklistId;

		public GeeklistLoader(Context context, int geeklistId) {
			super(context);
			mService = Adapter.create();
			mGeeklistId = geeklistId;
		}

		@Override
		public GeeklistData loadInBackground() {
			GeeklistData geeklistData;
			try {
				geeklistData = new GeeklistData(mService.geeklist(mGeeklistId));
			} catch (Exception e) {
				geeklistData = new GeeklistData(e);
			}
			return geeklistData;
		}
	}

	static class GeeklistData extends Data<GeeklistItem> {
		private Geeklist mGeeklist;

		public GeeklistData(Geeklist geeklist) {
			super();
			mGeeklist = geeklist;
		}

		public GeeklistData(Exception e) {
			super(e);
		}

		@Override
		protected List<GeeklistItem> list() {
			if (mGeeklist == null || mGeeklist.items == null) {
				return new ArrayList<>();
			}
			return mGeeklist.items;
		}
	}

	public class GeeklistAdapter extends ArrayAdapter<GeeklistItem> {
		private LayoutInflater mInflater;

		public GeeklistAdapter(Activity activity, List<GeeklistItem> items) {
			super(activity, R.layout.row_geeklist_item, items);
			mInflater = activity.getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_geeklist_item, parent, false);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			GeeklistItem item;
			try {
				item = getItem(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (item != null) {
				String content = GeeklistUtils.convertBoardGameGeekXmlText(item.body);
				
				holder.username.setText("Posted by " + item.username);
                holder.postedDate.setText("Posted " + item.postdate);
                holder.editedDate.setText("Edited " + item.editdate);
                holder.gameName.setText(item.objectname);

                loadThumbnail(BoardGameGeekConstants.BGG_BOARDGAME_IMAGE + item.imageid + "_t.jpg", holder.thumbnail);

				UIUtils.setWebViewText(holder.body, content);
			}
			return convertView;
		}
	}

	public static class ViewHolder {
		TextView username;
		TextView postedDate;
		TextView editedDate;
        TextView gameName;
        ImageView thumbnail;
		WebView body;

		public ViewHolder(View view) {
			username = (TextView) view.findViewById(R.id.geeklist_item_username);
            postedDate = (TextView) view.findViewById(R.id.geeklist_item_posted_date);
            editedDate = (TextView) view.findViewById(R.id.geeklist_item_edited_date);
            gameName = (TextView) view.findViewById(R.id.game_name);
            thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
			body = (WebView) view.findViewById(R.id.geeklist_item_body);
		}
	}
}
