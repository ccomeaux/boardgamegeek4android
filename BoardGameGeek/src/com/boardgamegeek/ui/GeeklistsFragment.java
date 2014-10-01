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

import com.boardgamegeek.R;
import com.boardgamegeek.model.GeeklistEntry;
import com.boardgamegeek.ui.widget.BggLoader;
import com.boardgamegeek.ui.widget.Data;
import com.boardgamegeek.util.BoardGameGeekConstants;
import com.boardgamegeek.util.GeeklistUtils;
import com.boardgamegeek.util.UIUtils;

public class GeeklistsFragment extends BggListFragment implements LoaderManager.LoaderCallbacks<GeeklistsFragment.GeeklistsData> {
	private static final int GEEKLISTS_LOADER_ID = 0;

	private GeeklistsAdapter mGeeklistsAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		intent.getData();
	}

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
	public Loader<GeeklistsData> onCreateLoader(int id, Bundle data) {
		return new GeeklistsLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<GeeklistsData> loader, GeeklistsData data) {
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
	public void onLoaderReset(Loader<GeeklistsData> loader) {
	}

	@Override
	public void onListItemClick(ListView listView, View convertView, int position, long id) {
		GeeklistViewHolder holder = (GeeklistViewHolder) convertView.getTag();
		if (holder != null) {
			Intent intent = new Intent(getActivity(), GeeklistActivity.class);
			intent.putExtra(GeeklistUtils.KEY_GEEKLIST_ID, holder.geeklistId);
			intent.putExtra(GeeklistUtils.KEY_GEEKLIST_TITLE, holder.geeklistTitle.getText());
			startActivity(intent);
		}
	}
	
	private static class GeeklistsLoader extends BggLoader<GeeklistsData> {
		public GeeklistsLoader(Context context) {
			super(context);
		}

		@Override
		public GeeklistsData loadInBackground() {
			GeeklistsData geeklistsData = null;
			try {
				geeklistsData = new GeeklistsData();
				List<GeeklistEntry> geeklists = new ArrayList<>();
				geeklists.addAll(getGeeklists(1, "hot"));
				geeklists.addAll(getGeeklists(2, "hot"));
				geeklists.addAll(getGeeklists(3, "hot"));
				geeklistsData.geeklists = geeklists;
			} catch (Exception e) {
				geeklistsData = new GeeklistsData(e);
			}
			return geeklistsData;
		}

	    private List<GeeklistEntry> getGeeklists(int page, String sort) 
	    {
	        List<GeeklistEntry> geeklists = new ArrayList<>();

	        String url = BoardGameGeekConstants.BGG_GEEKLIST + "module?ajax=1&domain=boardgame&nosession=1&objectid=0&objecttype=&pageid=" + page + "&showcontrols=1&showcount=12&sort=" + sort + "&tradelists=0&version=v2";

	        StringBuilder sb = new StringBuilder();
	        try {
		        URLConnection connection = new URL(url).openConnection();
		        connection.setRequestProperty("Accept", "application/json, text/plain, */*");
		        connection.setRequestProperty("Accept-Charset", "UTF-8");
		        InputStream response = connection.getInputStream();
	
		        if ("gzip".equals(connection.getContentEncoding())) {
		            response = new GZIPInputStream(response);
		        }
	
		        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response, Charset.forName("UTF-8")));
		        
		        String line = null;
		        while ((line = bufferedReader.readLine()) != null) {
		            sb.append(line + "\n");
		        }
		        bufferedReader.close();
	        } catch (Exception e) {
	        }
	        
	        String json = sb.toString();
	        
	        if(!json.isEmpty()) {
		        try {
		        	JSONObject jsonObject = new JSONObject(json);  
	                
	                JSONArray lists = jsonObject.getJSONArray("lists");
	                
	                for (int i=0; i < lists.length(); i++) {
		                JSONObject geeklistJson = lists.getJSONObject(i);
		                GeeklistEntry geeklist = getGeekList(geeklistJson);
		                geeklists.add(geeklist);
	                }  
		        } catch (JSONException e) {
		        }
	        }

	        return geeklists;
	    }

	    private GeeklistEntry getGeekList(JSONObject item) throws JSONException
	    {
	    	GeeklistEntry geekList = new GeeklistEntry();
	        geekList.thumbs = Integer.parseInt(item.getString("numpositive"));
	        geekList.title = item.getString("title");
	        String link = BoardGameGeekConstants.BGG_WEBSITE + item.getString("href");
	        geekList.id = getId(link);
	        geekList.link = link;
	        geekList.creator = item.getString("username");
	        geekList.entries = Integer.parseInt(item.getString("numitems"));
	        return geekList;
	    }

	    private int getId(String link)
	    {
	        int start = link.indexOf("/geeklist/");
	        return Integer.valueOf(link.substring(start + 10, link.lastIndexOf("/")));
	    }
	}

	static class GeeklistsData extends Data<GeeklistEntry> {
		
		List<GeeklistEntry> geeklists = new ArrayList<>();

		public GeeklistsData() {
		}

		public GeeklistsData(Exception e) {
			super(e);
		}

		@Override
		public List<GeeklistEntry> list() {
			return geeklists;
		}
	}

	public static class GeeklistsAdapter extends ArrayAdapter<GeeklistEntry> {
		public static final int ITEM_VIEW_TYPE_GEEKLIST = 0;

		private LayoutInflater mInflater;

		public GeeklistsAdapter(Activity activity, List<GeeklistEntry> geeklists) {
			super(activity, R.layout.row_geeklist, geeklists);
			mInflater = activity.getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			GeeklistEntry geeklist;
			try {
				geeklist = getItem(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			
			GeeklistViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_geeklist, parent, false);
				holder = new GeeklistViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (GeeklistViewHolder) convertView.getTag();
			}

			if (geeklist != null) {
				holder.geeklistId = geeklist.id;
				holder.geeklistTitle.setText(geeklist.title);
				holder.creator.setText("by " + geeklist.creator);
				holder.numThumbs.setText(geeklist.thumbs + " thumbs");
			}
			return convertView;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public int getItemViewType(int position) {
			return ITEM_VIEW_TYPE_GEEKLIST;
		}
	}

	static class GeeklistViewHolder {
		public int geeklistId;
		public TextView geeklistTitle;
		public TextView creator;
		public TextView numThumbs;

		public GeeklistViewHolder(View view) {
			geeklistTitle = (TextView) view.findViewById(R.id.geeklist_title);
			creator = (TextView) view.findViewById(R.id.geeklist_creator);
			numThumbs = (TextView) view.findViewById(R.id.geeklist_thumbs);
		}
	}

	static class HeaderViewHolder {
		public TextView header;

		public HeaderViewHolder(View view) {
			header = (TextView) view.findViewById(android.R.id.title);
		}
	}
}
