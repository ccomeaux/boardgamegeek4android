package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.GeekListEntry;
import com.boardgamegeek.model.GeekListsResponse;
import com.boardgamegeek.ui.widget.BggLoader;
import com.boardgamegeek.ui.widget.Data;
import com.boardgamegeek.util.GeekListUtils;

public class GeekListsFragment extends BggListFragment implements
	LoaderManager.LoaderCallbacks<GeekListsFragment.GeekListsData> {
	private static final int GEEKLISTS_LOADER_ID = 0;

	private GeeklistsAdapter mGeeklistsAdapter;

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setEmptyText(getString(R.string.empty_geeklists));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(GEEKLISTS_LOADER_ID, null, this);
	}

	@Override
	public Loader<GeekListsData> onCreateLoader(int id, Bundle data) {
		return new GeekListsLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<GeekListsData> loader, GeekListsData data) {
		if (getActivity() == null) {
			return;
		}

		if (mGeeklistsAdapter == null) {
			mGeeklistsAdapter = new GeeklistsAdapter(getActivity(), data.list());
			setListAdapter(mGeeklistsAdapter);
		}
		mGeeklistsAdapter.notifyDataSetChanged();

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
	public void onLoaderReset(Loader<GeekListsData> loader) {
	}

	@Override
	public void onListItemClick(ListView listView, View convertView, int position, long id) {
		ViewHolder holder = (ViewHolder) convertView.getTag();
		if (holder != null) {
			Intent intent = new Intent(getActivity(), GeekListActivity.class);
			intent.putExtra(GeekListUtils.KEY_GEEKLIST_ID, holder.id);
			intent.putExtra(GeekListUtils.KEY_GEEKLIST_TITLE, holder.title.getText());
			startActivity(intent);
		}
	}

	private static class GeekListsLoader extends BggLoader<GeekListsData> {
		public GeekListsLoader(Context context) {
			super(context);
		}

		@Override
		public GeekListsData loadInBackground() {
			GeekListsData geekListsData = null;
			try {
				// TODO implement paginated data
				geekListsData = new GeekListsData();
				List<GeekListEntry> geekLists = new ArrayList<>();
				geekLists.addAll(getGeekLists(1, BggService.GEEKLIST_SORT_HOT));
				geekLists.addAll(getGeekLists(2, BggService.GEEKLIST_SORT_HOT));
				geekLists.addAll(getGeekLists(3, BggService.GEEKLIST_SORT_HOT));
				geekListsData.geekLists = geekLists;
			} catch (Exception e) {
				geekListsData = new GeekListsData(e);
			}
			return geekListsData;
		}

		private List<GeekListEntry> getGeekLists(int page, String sort) {
			BggService mService = Adapter.createWithJson();
			GeekListsResponse res = mService.geekLists(page, sort);
			return res.getGeekListEntries();
		}
	}

	static class GeekListsData extends Data<GeekListEntry> {
		List<GeekListEntry> geekLists = new ArrayList<>();

		public GeekListsData() {
		}

		public GeekListsData(Exception e) {
			super(e);
		}

		@Override
		public List<GeekListEntry> list() {
			return geekLists;
		}
	}

	public static class GeeklistsAdapter extends ArrayAdapter<GeekListEntry> {
		private LayoutInflater mInflater;

		public GeeklistsAdapter(Activity activity, List<GeekListEntry> geeklists) {
			super(activity, R.layout.row_geeklist, geeklists);
			mInflater = activity.getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			GeekListEntry geeklist;
			try {
				geeklist = getItem(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}

			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_geeklist, parent, false);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			if (geeklist != null) {
				Context context = parent.getContext();
				holder.id = geeklist.getId();
				holder.title.setText(geeklist.getTitle());
				holder.creator.setText(context.getString(R.string.by_prefix, geeklist.getAuthor()));
				holder.numThumbs.setText(context.getString(R.string.thumbs_suffix, geeklist.getNumberOfThumbs()));
			}
			return convertView;
		}
	}

	static class ViewHolder {
		public int id;
		@InjectView(R.id.geeklist_title) TextView title;
		@InjectView(R.id.geeklist_creator) TextView creator;
		@InjectView(R.id.geeklist_thumbs) TextView numThumbs;

		public ViewHolder(View view) {
			ButterKnife.inject(this, view);
		}
	}
}
