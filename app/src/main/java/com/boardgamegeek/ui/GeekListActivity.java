package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.Status;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.viewmodel.GeekListViewModel;
import com.boardgamegeek.util.ActivityUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProvider;

public class GeekListActivity extends TabActivity {
	private static final String KEY_ID = "GEEK_LIST_ID";
	private static final String KEY_TITLE = "GEEK_LIST_TITLE";
	private int geekListId;
	private String geekListTitle;
	private GeekListPagerAdapter adapter;
	private GeekListViewModel viewModel;

	public static void start(Context context, int id, String title) {
		Intent starter = createIntent(context, id, title);
		context.startActivity(starter);
	}

	public static void startUp(Context context, int id, String title) {
		Intent starter = createIntent(context, id, title);
		starter.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(starter);
	}

	@NonNull
	private static Intent createIntent(Context context, int id, String title) {
		Intent starter = new Intent(context, GeekListActivity.class);
		starter.putExtra(KEY_ID, id);
		starter.putExtra(KEY_TITLE, title);
		return starter;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		viewModel = new ViewModelProvider(this).get(GeekListViewModel.class);

		final Intent intent = getIntent();
		geekListId = intent.getIntExtra(KEY_ID, BggContract.INVALID_ID);
		geekListTitle = intent.getStringExtra(KEY_TITLE);
		safelySetTitle(geekListTitle);

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GeekList")
				.putContentId(String.valueOf(geekListId))
				.putContentName(geekListTitle));
		}

		viewModel.setId(geekListId);
		viewModel.getGeekList().observe(this, resource -> {
			if (resource != null && resource.getStatus() == Status.SUCCESS && resource.getData() != null) {
				safelySetTitle(resource.getData().title);
			}
		});
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view_share;
	}

	@Override
	public boolean onOptionsItemSelected(@NotNull MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_view:
				//noinspection SpellCheckingInspection
				ActivityUtils.linkToBgg(this, "geeklist", geekListId);
				return true;
			case R.id.menu_share:
				ActivityUtils.shareGeekList(this, geekListId, geekListTitle);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@NotNull
	@Override
	protected FragmentPagerAdapter createAdapter() {
		adapter = new GeekListPagerAdapter(getSupportFragmentManager(), this);
		adapter.addTab(GeekListDescriptionFragment.newInstance(), R.string.title_description);
		adapter.addTab(GeekListItemsFragment.newInstance(), R.string.title_items);
		return adapter;
	}

	private final static class GeekListPagerAdapter extends FragmentPagerAdapter {
		static final class TabInfo {
			private final Fragment fragment;
			@StringRes private final int titleRes;

			TabInfo(Fragment fragment, int titleRes) {
				this.fragment = fragment;
				this.titleRes = titleRes;
			}
		}

		private final Context context;
		private final ArrayList<TabInfo> tabs = new ArrayList<>();

		public GeekListPagerAdapter(FragmentManager fragmentManager, Context context) {
			super(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
			this.context = context;
			tabs.clear();
		}

		public void addTab(Fragment fragment, @StringRes int titleRes) {
			tabs.add(new TabInfo(fragment, titleRes));
			notifyDataSetChanged();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			TabInfo tabInfo = tabs.get(position);
			if (tabInfo == null) return "";
			return context.getString(tabInfo.titleRes);
		}

		@Override
		public Fragment getItem(int position) {
			TabInfo tabInfo = tabs.get(position);
			if (tabInfo == null) return null;
			return tabInfo.fragment;
		}

		@Override
		public int getCount() {
			return tabs.size();
		}
	}
}
