package com.boardgamegeek.ui;

import android.content.ContentValues;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.adapter.GameColorRecyclerViewAdapter;
import com.boardgamegeek.ui.adapter.GameColorRecyclerViewAdapter.Callback;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment.EditTextDialogListener;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.TaskUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class ColorsFragment extends Fragment implements LoaderCallbacks<Cursor> {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_ICON_COLOR = "ICON_COLOR";
	private static final int TOKEN = 0x20;
	private int gameId;
	@ColorInt private int iconColor;
	private GameColorRecyclerViewAdapter adapter;
	private EditTextDialogFragment editTextDialogFragment;
	private ActionMode actionMode;

	private Unbinder unbinder;
	@BindView(R.id.root_container) CoordinatorLayout containerView;
	@BindView(android.R.id.progress) View progressView;
	@BindView(android.R.id.empty) View emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;
	@BindView(R.id.fab) FloatingActionButton fab;
	private final Paint swipePaint = new Paint();
	private Bitmap deleteIcon;
	@BindDimen(R.dimen.material_margin_horizontal) float horizontalPadding;

	public static ColorsFragment newInstance(int gameId, @ColorInt int iconColor) {
		Bundle args = new Bundle();
		args.putInt(KEY_GAME_ID, gameId);
		args.putInt(KEY_ICON_COLOR, iconColor);
		ColorsFragment fragment = new ColorsFragment();
		fragment.setArguments(args);
		return fragment;
	}

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
		colorFab();
		setUpRecyclerView();
		return rootView;
	}

	private void setUpRecyclerView() {
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

		swipePaint.setColor(ContextCompat.getColor(getContext(), R.color.medium_blue));
		deleteIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_delete_white);

		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			@Override
			public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder, ViewHolder target) {
				return false;
			}

			@Override
			public void onSwiped(ViewHolder viewHolder, int swipeDir) {
				final String color = adapter.getColorName(viewHolder.getAdapterPosition());
				int count = getActivity().getContentResolver().delete(Games.buildColorsUri(gameId, color), null, null);
				if (count > 0) {
					Snackbar.make(containerView, getString(R.string.msg_color_deleted, color), Snackbar.LENGTH_INDEFINITE)
						.setAction(R.string.undo, new OnClickListener() {
							@Override
							public void onClick(View v) {
								addColor(color);
							}
						})
						.show();
				}
			}

			@Override
			public int getSwipeDirs(RecyclerView recyclerView, ViewHolder viewHolder) {
				if (actionMode != null) {
					return 0;
				}
				return super.getSwipeDirs(recyclerView, viewHolder);
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
		itemTouchHelper.attachToRecyclerView(recyclerView);
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

		readBundle(getArguments());
		colorFab();

		getLoaderManager().restartLoader(TOKEN, getArguments(), this);
	}

	private void readBundle(Bundle bundle) {
		gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID);
		iconColor = bundle.getInt(KEY_ICON_COLOR, Color.TRANSPARENT);
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
			adapter = new GameColorRecyclerViewAdapter(cursor, R.layout.row_color, new Callback() {
				@Override
				public void onItemClick(int position) {
					if (actionMode != null) {
						toggleSelection(position);
					}
				}

				@Override
				public boolean onItemLongPress(int position) {
					if (actionMode != null) {
						return false;
					}
					actionMode = getActivity().startActionMode(new ActionMode.Callback() {
						@Override
						public boolean onCreateActionMode(ActionMode mode, Menu menu) {
							MenuInflater inflater = mode.getMenuInflater();
							inflater.inflate(R.menu.colors_context, menu);
							fab.hide();
							return true;
						}

						@Override
						public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
							return false;
						}

						@Override
						public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
							switch (item.getItemId()) {
								case R.id.menu_delete:
									List<Integer> selectedItemPositions = adapter.getSelectedItems();
									int count = 0;
									for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
										int position = selectedItemPositions.get(i);
										String color = adapter.getColorName(position);
										count += getActivity().getContentResolver().delete(Games.buildColorsUri(gameId, color), null, null);
									}
									Snackbar.make(containerView, getResources().getQuantityString(R.plurals.msg_colors_deleted, count, count), Snackbar.LENGTH_SHORT).show();
									mode.finish();
									return true;
							}
							mode.finish();
							return false;
						}

						@Override
						public void onDestroyActionMode(ActionMode mode) {
							actionMode = null;
							adapter.clearSelections();
							fab.show();
						}
					});
					toggleSelection(position);
					return true;
				}

				private void toggleSelection(int position) {
					adapter.toggleSelection(position);
					int count = adapter.getSelectedItemCount();
					if (count == 0) {
						actionMode.finish();
					} else {
						actionMode.setTitle(getResources().getQuantityString(R.plurals.msg_colors_selected, count, count));
					}
				}
			});
			recyclerView.setAdapter(adapter);
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
			Timber.w("Query complete, Not Actionable: %s", token);
			cursor.close();
		}

		AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
		fab.show();
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
						addColor(inputText);
					}
				}
			});
		}
		DialogUtils.showFragment(getActivity(), editTextDialogFragment, "edit_color");
	}

	@DebugLog
	private void colorFab() {
		if (fab != null && iconColor != Color.TRANSPARENT) {
			fab.setBackgroundTintList(ColorStateList.valueOf(iconColor));
		}
	}

	@DebugLog
	private void addColor(String color) {
		ContentValues values = new ContentValues();
		values.put(GameColors.COLOR, color);
		getActivity().getContentResolver().insert(Games.buildColorsUri(gameId), values);
	}

	private class Task extends AsyncTask<Void, Void, Integer> {
		@DebugLog
		@Override
		protected Integer doInBackground(Void... params) {
			Integer count = 0;
			Cursor cursor = null;
			try {
				cursor = getActivity().getContentResolver().query(Plays.buildPlayersByColor(),
					new String[] { PlayPlayers.COLOR }, Plays.OBJECT_ID + "=?",
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
