package com.boardgamegeek.ui;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.adapter.GameColorRecyclerViewAdapter;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment.EditTextDialogListener;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class ColorsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final int TOKEN = 0x20;
	private int gameId;
	private GameColorRecyclerViewAdapter adapter;
	private EditTextDialogFragment editTextDialogFragment;

	private Unbinder unbinder;
	@BindView(R.id.root_container) CoordinatorLayout containerView;
	@BindView(android.R.id.progress) View progressView;
	@BindView(android.R.id.empty) View emptyView;
	@BindView(android.R.id.list) RecyclerView listView;
	@BindView(R.id.fab) FloatingActionButton fab;
	private final Paint swipePaint = new Paint();
	private Bitmap deleteIcon;
	private float horizontalPadding;

	@DebugLog
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Nullable
	@DebugLog
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_colors, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		setUpRecyclerView();
		return rootView;
	}

	private void setUpRecyclerView() {
		LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		listView.setLayoutManager(layoutManager);

		swipePaint.setColor(getResources().getColor(R.color.medium_blue));
		deleteIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_delete_white);
		horizontalPadding = getResources().getDimension(R.dimen.material_margin_horizontal);

		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			@Override
			public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder, ViewHolder target) {
				return false;
			}

			@Override
			public void onSwiped(ViewHolder viewHolder, int swipeDir) {
				String color = adapter.getColorName(viewHolder.getAdapterPosition());
				int count = getActivity().getContentResolver().delete(Games.buildColorsUri(gameId, color), null, null);
				Snackbar.make(containerView, getResources().getQuantityString(R.plurals.msg_colors_deleted, count, count), Snackbar.LENGTH_SHORT).show();
			}

			@Override
			public void onChildDraw(Canvas c, RecyclerView recyclerView, ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
				if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
					View itemView = viewHolder.itemView;

					float verticalPadding = (itemView.getHeight() - deleteIcon.getHeight()) / 2;
					RectF background;
					Rect iconSrc;
					RectF iconDst;

					if (dX > 0) {
						background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom());
						iconSrc = new Rect(0, 0, (int) (dX - itemView.getLeft() - horizontalPadding), deleteIcon.getHeight());
						iconDst = new RectF((float) itemView.getLeft() + horizontalPadding, (float) itemView.getTop() + verticalPadding, Math.min(itemView.getLeft() + horizontalPadding + deleteIcon.getWidth(), dX), (float) itemView.getBottom() - verticalPadding);
					} else {
						background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
						iconSrc = new Rect(Math.max(deleteIcon.getWidth() + (int) horizontalPadding + (int) dX, 0), 0, deleteIcon.getWidth(), deleteIcon.getHeight());
						iconDst = new RectF(Math.max((float) itemView.getRight() + dX, (float) itemView.getRight() - horizontalPadding - deleteIcon.getWidth()), (float) itemView.getTop() + verticalPadding, (float) itemView.getRight() - horizontalPadding, (float) itemView.getBottom() - verticalPadding);
					}
					c.drawRect(background, swipePaint);
					c.drawBitmap(deleteIcon, iconSrc, iconDst, swipePaint);
				}
				super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
			}
		});
		itemTouchHelper.attachToRecyclerView(listView);
	}

	@Override
	public void onDestroyView() {
		unbinder.unbind();
		super.onDestroyView();
	}

	@DebugLog
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Uri uri = UIUtils.fragmentArgumentsToIntent(getArguments()).getData();
		gameId = Games.getGameId(uri);

		getLoaderManager().restartLoader(TOKEN, getArguments(), this);
	}

	@DebugLog
	@Override
	public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
		inflater.inflate(R.menu.game_colors, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_colors_generate:
				TaskUtils.executeAsyncTask(new Task());
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Nullable
	@DebugLog
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		return new CursorLoader(getActivity(), Games.buildColorsUri(gameId), GameColorRecyclerViewAdapter.PROJECTION, null, null, null);
	}

	@DebugLog
	@Override
	public void onLoadFinished(@NonNull Loader<Cursor> loader, @NonNull Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new GameColorRecyclerViewAdapter(cursor, R.layout.row_color);
			listView.setAdapter(adapter);
		}

		int token = loader.getId();
		if (token == TOKEN) {
			if (cursor.getCount() == 0) {
				AnimationUtils.fadeIn(emptyView);
			} else {
				AnimationUtils.fadeOut(emptyView);
			}

			adapter.changeCursor(cursor);
		} else {
			Timber.w("Query complete, Not Actionable: " + token);
			cursor.close();
		}

		AnimationUtils.fadeIn(getActivity(), listView, isResumed());
		AnimationUtils.fadeIn(getActivity(), fab, isResumed());
		AnimationUtils.fadeOut(progressView);
	}

	@DebugLog
	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		if (adapter != null) {
			adapter.changeCursor(null);
		}
	}

	@OnClick(R.id.fab)
	public void onFabClicked() {
		if (editTextDialogFragment == null) {
			editTextDialogFragment = EditTextDialogFragment.newInstance(R.string.title_add_color, null, new EditTextDialogListener() {
				@Override
				public void onFinishEditDialog(String inputText) {
					if (!TextUtils.isEmpty(inputText)) {
						ContentValues values = new ContentValues();
						values.put(GameColors.COLOR, inputText);
						getActivity().getContentResolver().insert(Games.buildColorsUri(gameId), values);
					}
				}
			});
		}
		DialogUtils.showFragment(getActivity(), editTextDialogFragment, "edit_color");
	}

	private class Task extends AsyncTask<Void, Void, Integer> {
		@DebugLog
		@Override
		protected Integer doInBackground(Void... params) {
			Integer count = 0;
			Cursor cursor = null;
			try {
				cursor = getActivity().getContentResolver().query(Plays.buildPlayersByColor(),
					new String[] { PlayPlayers.COLOR }, PlayItems.OBJECT_ID + "=?",
					new String[] { String.valueOf(gameId) }, null);
				if (cursor != null && cursor.moveToFirst()) {
					List<ContentValues> values = new ArrayList<>();
					do {
						String color = cursor.getString(0);
						if (!TextUtils.isEmpty(color)) {
							ContentValues cv = new ContentValues();
							cv.put(GameColors.COLOR, color);
							values.add(cv);
						}
					} while (cursor.moveToNext());
					if (values.size() > 0) {
						ContentValues[] array = {};
						count = getActivity().getContentResolver().bulkInsert(Games.buildColorsUri(gameId), values.toArray(array));
					}
				}
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
			return count;
		}

		@DebugLog
		@Override
		protected void onPostExecute(Integer result) {
			if (result > 0) {
				Snackbar.make(containerView, R.string.msg_colors_generated, Snackbar.LENGTH_SHORT).show();
			}
		}
	}
}
