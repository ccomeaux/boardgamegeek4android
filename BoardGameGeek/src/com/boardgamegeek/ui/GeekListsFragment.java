package com.boardgamegeek.ui;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import com.boardgamegeek.model.GeekListEntry;
import com.boardgamegeek.ui.widget.BggLoader;
import com.boardgamegeek.ui.widget.Data;
import com.boardgamegeek.util.BoardGameGeekConstants;
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
				geekLists.addAll(getGeekLists(1, "hot"));
				geekLists.addAll(getGeekLists(2, "hot"));
				geekLists.addAll(getGeekLists(3, "hot"));
				geekListsData.geekLists = geekLists;
			} catch (Exception e) {
				geekListsData = new GeekListsData(e);
			}
			return geekListsData;
		}

		// TODO use okhttp/retrofit
		private List<GeekListEntry> getGeekLists(int page, String sort) {
			List<GeekListEntry> geeklists = new ArrayList<>();

			String url = BoardGameGeekConstants.BGG_GEEKLIST
				+ "module?ajax=1&domain=boardgame&nosession=1&objectid=0&objecttype=&pageid=" + page
				+ "&showcontrols=1&showcount=12&sort=" + sort + "&tradelists=0&version=v2";

			StringBuilder sb = new StringBuilder();
			try {
				URLConnection connection = new URL(url).openConnection();
				connection.setRequestProperty("Accept", "application/json, text/plain, */*");
				connection.setRequestProperty("Accept-Charset", "UTF-8");
				InputStream response = connection.getInputStream();

				if ("gzip".equals(connection.getContentEncoding())) {
					response = new GZIPInputStream(response);
				}

				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response,
					Charset.forName("UTF-8")));

				String line = null;
				while ((line = bufferedReader.readLine()) != null) {
					sb.append(line + "\n");
				}
				bufferedReader.close();
			} catch (Exception e) {
				// TODO LOGE()
			}

			String json = sb.toString();

			if (!json.isEmpty()) {
				try {
					JSONObject jsonObject = new JSONObject(json);

					JSONArray lists = jsonObject.getJSONArray("lists");

					for (int i = 0; i < lists.length(); i++) {
						JSONObject geeklistJson = lists.getJSONObject(i);
						GeekListEntry geeklist = getGeekList(geeklistJson);
						geeklists.add(geeklist);
					}
				} catch (JSONException e) {
				}
			}

			return geeklists;
		}

		private GeekListEntry getGeekList(JSONObject item) throws JSONException {
			GeekListEntry geekList = new GeekListEntry();
			geekList.thumbs = Integer.parseInt(item.getString("numpositive"));
			geekList.title = item.getString("title");
			String link = BoardGameGeekConstants.BGG_WEBSITE + item.getString("href");
			geekList.id = getId(link);
			geekList.link = link;
			geekList.creator = item.getString("username");
			geekList.entries = Integer.parseInt(item.getString("numitems"));
			return geekList;
		}

		private int getId(String link) {
			int start = link.indexOf("/geeklist/");
			return Integer.valueOf(link.substring(start + 10, link.lastIndexOf("/")));
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
				holder.id = geeklist.id;
				holder.title.setText(geeklist.title);
				holder.creator.setText(context.getString(R.string.by_prefix, geeklist.creator));
				holder.numThumbs.setText(context.getString(R.string.thumbs_suffix, geeklist.thumbs));
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
