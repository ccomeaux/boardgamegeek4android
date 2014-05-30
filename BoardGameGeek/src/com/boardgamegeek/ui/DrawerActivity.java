package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Plays;

public abstract class DrawerActivity extends BaseActivity {
	private static final int REQUEST_SIGNIN = 1;
	protected DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private NavigationAdapter mAdapter;

	protected abstract int getContentViewId();

	protected int getDrawerResId() {
		return 0;
	}

	protected boolean isTitleHidden() {
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getContentViewId());

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
		Integer titleResId = mAdapter.getItem(position);
		if (titleResId != getDrawerResId()) {
			Intent intent = null;
			switch (titleResId) {
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
				startActivity(intent);
				finish();
			}
		}
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	private class NavigationAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		private List<Integer> mTitles;

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
			mTitles = new ArrayList<Integer>();
			if (!Authenticator.isSignedIn(DrawerActivity.this)) {
				mTitles.add(R.string.title_signin);
			} else {
				mTitles.add(R.string.title_collection);
				mTitles.add(R.string.title_plays);
				mTitles.add(R.string.title_buddies);
			}
			mTitles.add(R.string.title_hotness);
			mTitles.add(R.string.title_forums);
		}

		@Override
		public int getCount() {
			return mTitles.size();
		}

		@Override
		public Integer getItem(int position) {
			return mTitles.get(position);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView == null) {
				view = mInflater.inflate(R.layout.row_drawer, parent, false);
			} else {
				view = convertView;
			}

			int titleResId = getItem(position);

			TextView textView = (TextView) view.findViewById(android.R.id.text1);
			TextView headerView = (TextView) view.findViewById(R.id.separator);

			if (titleResId == getDrawerResId()) {
				String text = getString(titleResId);
				SpannableString ss = new SpannableString(text);
				ss.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				textView.setText(ss);
			} else {
				textView.setText(titleResId);
			}

			if (titleResId == R.string.title_hotness) {
				// Always show the "Browse" header above hotness
				headerView.setVisibility(View.VISIBLE);
				headerView.setText(R.string.title_browse);
			} else if (position == 0) {
				// Show the "My Geek" header above the first non-hotness item
				headerView.setVisibility(View.VISIBLE);
				headerView.setText(R.string.title_my_geek);
			} else {
				headerView.setVisibility(View.GONE);
			}

			return view;
		}
	}
}
