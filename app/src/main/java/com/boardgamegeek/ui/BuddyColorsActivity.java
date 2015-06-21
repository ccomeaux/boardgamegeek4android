package com.boardgamegeek.ui;

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.ui.model.BuddyColor;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.RandomUtils;
import com.boardgamegeek.util.ResolverUtils;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DropListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class BuddyColorsActivity extends BaseActivity {
	private QueryHandler mHandler;
	private String mBuddyName;
	private List<BuddyColor> mColors;
	private Adapter mAdapter;

	@InjectView(R.id.toolbar) Toolbar mToolbar;
	@InjectView(android.R.id.progress) View mProgress;
	@InjectView(android.R.id.empty) View mEmpty;
	@InjectView(android.R.id.list) DragSortListView mList;

	@SuppressLint("HandlerLeak")
	private class QueryHandler extends AsyncQueryHandler {
		public QueryHandler(ContentResolver cr) {
			super(cr);
		}

		@DebugLog
		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			// If the query didn't return a cursor for some reason return
			if (cursor == null) {
				return;
			}

			// If the Activity is finishing, then close the cursor
			if (isFinishing()) {
				cursor.close();
				return;
			}

			mColors = new ArrayList<>(cursor.getCount());
			try {
				while (cursor.moveToNext()) {
					mColors.add(BuddyColor.fromCursor(cursor));
				}
				mAdapter = new Adapter();
				mList.setAdapter(mAdapter);
			} finally {
				cursor.close();
			}

			bindUi();
		}
	}

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_buddy_colors);
		ButterKnife.inject(this);

		mBuddyName = getIntent().getStringExtra(ActivityUtils.KEY_BUDDY_NAME);
		if (TextUtils.isEmpty(mBuddyName)) {
			Timber.w("Can't launch - missing buddy name.");
			finish();
		}
		setSubtitle(mBuddyName);

		mList.setSelector(android.R.color.transparent);

		if (mToolbar != null) {
			setSupportActionBar(mToolbar);
		}
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		mHandler = new QueryHandler(getContentResolver());
		startQuery();
	}

	@DebugLog
	@Override
	protected void onStop() {
		super.onStop();
		if (mColors != null && mColors.size() > 0) {
			final ContentResolver resolver = getContentResolver();
			ArrayList<ContentProviderOperation> batch = new ArrayList<>();
			for (int i = 0; i < mColors.size(); i++) {
				final String color = mColors.get(i).getColor();
				final int sortOrder = mColors.get(i).getSortOrder();

				Builder builder;
				if (ResolverUtils.rowExists(resolver, PlayerColors.buildUserUri(mBuddyName, sortOrder))) {
					builder = ContentProviderOperation.newUpdate(PlayerColors.buildUserUri(mBuddyName, sortOrder));
				} else {
					builder = ContentProviderOperation.newInsert(PlayerColors.buildUserUri(mBuddyName))
						.withValue(PlayerColors.PLAYER_COLOR_SORT_ORDER, sortOrder);
				}
				batch.add(builder.withValue(PlayerColors.PLAYER_COLOR, color).build());
			}
			ResolverUtils.applyBatch(this, batch);
			mColors = null; // to force a load from cursor
		}
	}

	@DebugLog
	@Override
	protected int getOptionsMenuId() {
		return R.menu.player_colors;
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_clear:
				DialogUtils.createConfirmationDialog(this, R.string.are_you_sure_clear_colors, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mColors.clear();
						if (mAdapter != null) {
							mAdapter.notifyDataSetChanged();
						}
						bindUi();
					}
				}).show();
				break;
			case android.R.id.home:
				ActivityUtils.navigateUpToBuddy(this, mBuddyName);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@DebugLog
	private void startQuery() {
		if (mColors != null) {
			// we already have the play from the saved instance
			bindUi();
		} else {
			mHandler.startQuery(0, null, PlayerColors.buildUserUri(mBuddyName), BuddyColor.PROJECTION, null, null, null);
		}
	}

	@DebugLog
	@OnClick(R.id.empty_button)
	void onEmptyClick(View view) {
		List<Pair<String, Integer>> colors = ColorUtils.getColorList();
		Random r = RandomUtils.getRandom();
		int order = 1;
		while (colors.size() > 0) {
			int i = r.nextInt(colors.size());
			BuddyColor color = new BuddyColor(colors.remove(i).first, order++);
			mColors.add(color);
		}
		bindUi();
	}

	@DebugLog
	private void bindUi() {
		if (mColors == null) {
			mProgress.setVisibility(View.VISIBLE);
			mEmpty.setVisibility(View.GONE);
			mList.setVisibility(View.GONE);
		} else if (mColors.size() == 0) {
			mProgress.setVisibility(View.GONE);
			mEmpty.setVisibility(View.VISIBLE);
			mList.setVisibility(View.GONE);
		} else {
			mProgress.setVisibility(View.GONE);
			mEmpty.setVisibility(View.GONE);
			mList.setVisibility(View.VISIBLE);
			mAdapter.notifyDataSetChanged();
		}
	}

	private class Adapter extends BaseAdapter implements DropListener {
		@DebugLog
		@Override
		public int getCount() {
			return mColors == null ? 0 : mColors.size();
		}

		@DebugLog
		@Override
		public Object getItem(int position) {
			if (mColors == null) {
				return null;
			}
			for (BuddyColor color : mColors) {
				if (color.getSortOrder() == (position + 1)) {
					return color;
				}
			}
			return null;
		}

		@DebugLog
		@Override
		public long getItemId(int position) {
			return position;
		}

		@DebugLog
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(BuddyColorsActivity.this).inflate(R.layout.row_player_color, parent, false);
			}
			BuddyColor color = (BuddyColor) getItem(position);
			if (color != null) {
				((TextView) convertView.findViewById(android.R.id.title)).setText(color.getColor());
				ColorUtils.setColorViewValue(convertView.findViewById(R.id.color_view), ColorUtils.parseColor(color.getColor()));
			}
			return convertView;
		}

		@DebugLog
		@Override
		public void drop(int from, int to) {
			final BuddyColor remove = mColors.remove(from);
			mColors.add(to, remove);
			for (int i = 0; i < mColors.size(); i++) {
				final BuddyColor color = mColors.get(i);
				color.setSortOrder(i + 1);
			}

			notifyDataSetChanged();
		}
	}
}
