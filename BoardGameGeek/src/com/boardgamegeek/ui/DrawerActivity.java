package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Plays;

public abstract class DrawerActivity extends BaseActivity {
	private static final int REQUEST_SIGNIN = 1;
	protected static final String EXTRA_NAVIGATION_POSITION = "EXTRA_NAVIGATION_POSITION";
	protected DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private int mPosition;
	private NavigationAdapter mAdapter;

	protected abstract int getContentViewId();

	protected boolean isTitleHidden() {
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getContentViewId());

		mPosition = getIntent().getIntExtra(EXTRA_NAVIGATION_POSITION, -1);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (mDrawerLayout != null) {
			mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		}

		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		if (mDrawerList != null) {
			mAdapter = new NavigationAdapter();
			mDrawerList.setAdapter(mAdapter);
			mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		}

		// TODO open the drawer upon launch until user opens it themselves
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshDrawer();
	}

	public boolean isDrawerOpen() {
		return mDrawerLayout != null && mDrawerList != null && mDrawerLayout.isDrawerOpen(mDrawerList);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_SIGNIN && resultCode == RESULT_OK) {
			onSignInSuccess();
		}
	}

	protected void onSignInSuccess() {
		refreshDrawer();
	}

	private void refreshDrawer() {
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position);
		}
	}

	private void selectItem(int position) {
		if (position != mPosition) {
			Intent intent = null;
			switch (mAdapter.getItem(position).first) {
				case R.string.title_collection:
					intent = new Intent(Intent.ACTION_VIEW, Collection.CONTENT_URI);
					break;
				case R.string.title_hotness:
					intent = new Intent(this, HotnessActivity.class);
					break;
				case R.string.title_plays:
					intent = new Intent(Intent.ACTION_VIEW, Plays.CONTENT_URI);
					break;
				case R.string.title_buddies:
					intent = new Intent(Intent.ACTION_VIEW, Buddies.CONTENT_URI);
					break;
				case R.string.title_forums:
					intent = new Intent(this, ForumsActivity.class);
					break;
				case R.string.title_signin:
					startActivityForResult(new Intent(this, LoginActivity.class), REQUEST_SIGNIN);
					break;
			}
			if (intent != null) {
				intent.putExtra(EXTRA_NAVIGATION_POSITION, position);
				startActivity(intent);
				overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
				finish();
			}
		}
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	private class NavigationAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		private List<Pair<Integer, Integer>> mTitles;

		public NavigationAdapter() {
			mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			init();
		}

		@Override
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged();
			init();
		}

		public void init() {
			mTitles = new ArrayList<Pair<Integer, Integer>>();
			if (!Authenticator.isSignedIn(DrawerActivity.this)) {
				mTitles.add(new Pair<Integer, Integer>(R.string.title_signin, R.drawable.home_btn_signin));
			} else {
				mTitles.add(new Pair<Integer, Integer>(R.string.title_collection, R.drawable.home_btn_collection));
				mTitles.add(new Pair<Integer, Integer>(R.string.title_plays, R.drawable.home_btn_plays));
				mTitles.add(new Pair<Integer, Integer>(R.string.title_buddies, R.drawable.home_btn_buddies));
			}
			mTitles.add(new Pair<Integer, Integer>(R.string.title_hotness, R.drawable.home_btn_hotness));
			mTitles.add(new Pair<Integer, Integer>(R.string.title_forums, R.drawable.home_btn_forums));
		}

		@Override
		public int getCount() {
			return mTitles.size();
		}

		@Override
		public Pair<Integer, Integer> getItem(int position) {
			return mTitles.get(position);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).first;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView == null) {
				view = mInflater.inflate(R.layout.row_drawer, parent, false);
			} else {
				view = convertView;
			}

			Pair<Integer, Integer> item = getItem(position);

			TextView text = (TextView) view.findViewById(android.R.id.text1);
			ImageView image = (ImageView) view.findViewById(android.R.id.icon);
			TextView separator = (TextView) view.findViewById(R.id.separator);

			text.setText(item.first);
			image.setImageResource(item.second);
			if (item.first == R.string.title_hotness) {
				separator.setVisibility(View.VISIBLE);
				separator.setText(R.string.title_browse);
			} else if (position == 0) {
				separator.setVisibility(View.VISIBLE);
				separator.setText(R.string.title_my_geek);
			} else {
				separator.setVisibility(View.GONE);
			}

			if (position == mPosition) {
				// TODO fix this for real
				// UIUtils.setActivatedCompat(view, true);
				view.setBackgroundResource(R.color.accent_light_3);
			} else {
				view.setBackgroundColor(Color.TRANSPARENT);
			}

			return view;
		}
	}
}
