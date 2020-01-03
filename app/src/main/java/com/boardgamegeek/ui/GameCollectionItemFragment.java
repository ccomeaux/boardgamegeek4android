package com.boardgamegeek.ui;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.events.CollectionItemChangedEvent;
import com.boardgamegeek.events.CollectionItemResetEvent;
import com.boardgamegeek.events.CollectionItemUpdatedEvent;
import com.boardgamegeek.extensions.DialogUtils;
import com.boardgamegeek.extensions.DoubleUtils;
import com.boardgamegeek.extensions.IntUtils;
import com.boardgamegeek.extensions.TaskUtils;
import com.boardgamegeek.extensions.TextViewUtils;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.tasks.ResetCollectionItemTask;
import com.boardgamegeek.tasks.UpdateCollectionItemStatusTask;
import com.boardgamegeek.tasks.sync.SyncCollectionByGameTask;
import com.boardgamegeek.tasks.sync.SyncCollectionByGameTask.CompletedEvent;
import com.boardgamegeek.ui.dialog.EditCollectionTextDialogFragment;
import com.boardgamegeek.ui.dialog.PrivateInfoDialogFragment;
import com.boardgamegeek.ui.model.CollectionItem;
import com.boardgamegeek.ui.model.PrivateInfo;
import com.boardgamegeek.ui.widget.RatingView;
import com.boardgamegeek.ui.widget.TextEditorView;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.StringUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.palette.graphics.Palette;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;
import timber.log.Timber;

public class GameCollectionItemFragment extends Fragment implements LoaderCallbacks<Cursor> {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_COLLECTION_ID = "COLLECTION_ID";
	private static final int _TOKEN = 0;
	private static final int AGE_IN_DAYS_TO_REFRESH = 7;

	private Unbinder unbinder;

	@BindView(R.id.invalid_status) View invalidStatusView;
	@BindView(R.id.main_container) ViewGroup mainContainer;

	// statuses
	@BindView(R.id.status) TextView statusView;
	@BindView(R.id.want_to_buy) CheckBox wantToBuyView;
	@BindView(R.id.preordered) CheckBox preorderedView;
	@BindView(R.id.own) CheckBox ownView;
	@BindView(R.id.want_to_play) CheckBox wantToPlayView;
	@BindView(R.id.previously_owned) CheckBox previouslyOwnedView;

	// rating
	@BindView(R.id.rating) RatingView ratingView;

	// comment
	@BindView(R.id.comment) TextEditorView commentView;

	// wishlist
	@BindView(R.id.wishlist_status) TextView wishlistStatusView;
	@BindView(R.id.wishlist) CheckBox wishlistView;
	@BindView(R.id.wishlist_priority) Spinner wishlistPriorityView;
	@BindView(R.id.wishlist_comment) TextEditorView wishlistCommentView;

	// trade
	@BindView(R.id.trade_status) TextView tradeStatusView;
	@BindView(R.id.want_in_trade) CheckBox wantInTradeView;
	@BindView(R.id.for_trade) CheckBox forTradeView;
	@BindView(R.id.condition) TextEditorView conditionView;
	@BindView(R.id.want_parts) TextEditorView wantPartsView;
	@BindView(R.id.has_parts) TextEditorView hasPartsView;

	// private info
	@BindView(R.id.private_info_container) ViewGroup privateInfoContainer;
	@BindView(R.id.private_info_hint) TextView privateInfoHintView;
	@BindView(R.id.private_info_view) TextView viewPrivateInfoView;
	@BindView(R.id.private_info_edit) TextView editPrivateInfoView;
	@BindView(R.id.private_comment) TextEditorView privateInfoCommentView;

	// footer
	@BindView(R.id.last_modified) TimestampView lastModifiedView;
	@BindView(R.id.collection_id) TextView idView;
	@BindView(R.id.updated) TimestampView updatedView;

	@BindViews({
		R.id.card_header_private_info,
		R.id.wishlist_header,
		R.id.trade_header,
		R.id.private_info_hint
	}) List<TextView> colorizedHeaders;
	@BindViews({
		R.id.comment,
		R.id.private_comment,
		R.id.wishlist_comment,
		R.id.condition,
		R.id.want_parts,
		R.id.has_parts
	}) List<TextEditorView> textEditorViews;
	@BindViews({
		R.id.status_edit_container,
		R.id.private_info_edit_container,
		R.id.wishlist_edit_container,
		R.id.trade_edit_container
	}) List<View> editFields;
	@BindViews({
		R.id.status
	}) List<View> viewOnlyFields;
	@BindViews({
		R.id.status,
		R.id.private_info_view,
		R.id.wishlist_status,
		R.id.trade_status
	}) List<View> visibleByTagOrGoneViews;
	@BindViews({
		R.id.main_container,
		R.id.private_info_container,
		R.id.wishlist_container,
		R.id.trade_container
	}) List<ViewGroup> visibleByChildrenViews;
	@BindViews({
		R.id.want_to_buy,
		R.id.preordered,
		R.id.own,
		R.id.want_to_play,
		R.id.previously_owned,
		R.id.want_in_trade,
		R.id.for_trade,
		R.id.wishlist
	}) List<CheckBox> statusViews;

	private int gameId = BggContract.INVALID_ID;
	private int collectionId = BggContract.INVALID_ID;
	private long internalId = 0;
	private boolean isRefreshing;
	private boolean mightNeedRefreshing;
	private Palette palette;
	private boolean needsUploading;
	@State boolean isItemEditable;
	private boolean isInEditMode;
	private boolean isDirty = false;

	public static GameCollectionItemFragment newInstance(int gameId, int collectionId) {
		Bundle args = new Bundle();
		args.putInt(KEY_GAME_ID, gameId);
		args.putInt(KEY_COLLECTION_ID, collectionId);
		GameCollectionItemFragment fragment = new GameCollectionItemFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@DebugLog
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		readBundle(getArguments());
		Icepick.restoreInstanceState(this, savedInstanceState);
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID);
		collectionId = bundle.getInt(KEY_COLLECTION_ID, BggContract.INVALID_ID);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_game_collection_item, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		unbinder = ButterKnife.bind(this, view);

		colorize(palette);

		mightNeedRefreshing = true;
		LoaderManager.getInstance(this).restartLoader(_TOKEN, getArguments(), this);
	}

	@DebugLog
	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@DebugLog
	@Override
	public void onStop() {
		super.onStop();
		EventBus.getDefault().unregister(this);
	}


	@DebugLog
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@DebugLog
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.game_collection_fragment, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.menu_discard).setVisible(!isInEditMode && isDirty);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_discard:
				DialogUtils.createDiscardDialog(getActivity(), R.string.collection_item, false, false, R.string.keep,
					() -> TaskUtils.executeAsyncTask(new ResetCollectionItemTask(getContext(), internalId))
				).show();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@DebugLog
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		if (id != _TOKEN || getContext() == null) return null;

		return new CursorLoader(getContext(),
			CollectionItem.getUri(),
			CollectionItem.getProjection(),
			CollectionItem.getSelection(collectionId),
			CollectionItem.getSelectionArgs(collectionId, gameId),
			null);
	}

	@DebugLog
	@Override
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) return;

		if (loader.getId() == _TOKEN) {
			if (cursor == null || !cursor.moveToFirst()) {
				if (mightNeedRefreshing) triggerRefresh();
				return;
			}

			CollectionItem item = CollectionItem.fromCursor(cursor);
			internalId = item.getInternalId();
			isDirty = item.isDirty();
			updateUi(item);

			if (mightNeedRefreshing) {
				if (DateTimeUtils.howManyDaysOld(item.getUpdated()) > AGE_IN_DAYS_TO_REFRESH) {
					triggerRefresh();
				}
			}
			mightNeedRefreshing = false;
		} else {
			Timber.d("Query complete, Not Actionable: %s", loader.getId());
			if (cursor != null) cursor.close();
		}
	}

	@DebugLog
	@Override
	public void onLoaderReset(@NonNull Loader<Cursor> loader) {
	}

	public void enableEditMode(boolean enable) {
		isInEditMode = enable;
		bindVisibility();
	}

	private void bindVisibility() {
		boolean isEdit = isInEditMode && isItemEditable;

		for (View view : editFields) {
			view.setVisibility(isEdit ? View.VISIBLE : View.GONE);
		}

		ratingView.enableEditMode(isEdit);
		commentView.enableEditMode(isEdit);
		privateInfoCommentView.enableEditMode(isEdit);
		wishlistCommentView.enableEditMode(isEdit);
		conditionView.enableEditMode(isEdit);
		wantPartsView.enableEditMode(isEdit);
		hasPartsView.enableEditMode(isEdit);

		if (isEdit) {
			for (View view : visibleByTagOrGoneViews) {
				view.setVisibility(View.GONE);
			}
		} else {
			for (View view : visibleByTagOrGoneViews) {
				boolean isVisible = getVisibleTag(view);
				view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
			}
		}

		for (ViewGroup view : visibleByChildrenViews) {
			setVisibilityByChildren(view);
		}

		invalidStatusView.setVisibility(getInvalidVisibility() ? View.VISIBLE : View.GONE);
	}

	private boolean getInvalidVisibility() {
		for (View view : visibleByChildrenViews) {
			if (view.getVisibility() == View.VISIBLE) {
				return false;
			}
		}
		return true;
	}

	public void syncChanges() {
		if (needsUploading) {
			SyncService.sync(getContext(), SyncService.FLAG_SYNC_COLLECTION_UPLOAD);
			needsUploading = false;
		}
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@DebugLog
	@Subscribe
	public void onEvent(CollectionItemUpdatedEvent event) {
		needsUploading = true;
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@DebugLog
	@Subscribe
	public void onEvent(CollectionItemResetEvent event) {
		if (event.getInternalId() == internalId) {
			needsUploading = false;
			TaskUtils.executeAsyncTask(new SyncCollectionByGameTask((BggApplication) getActivity().getApplication(), gameId));
		}
	}

	@DebugLog
	public void onPaletteGenerated(Palette palette) {
		this.palette = palette;
		colorize(palette);
	}

	@DebugLog
	private void colorize(Palette palette) {
		if (palette == null || !isAdded()) return;
		if (colorizedHeaders == null || textEditorViews == null) return;
		Palette.Swatch swatch = PaletteUtils.getHeaderSwatch(palette);
		for (TextView view : colorizedHeaders) {
			view.setTextColor(swatch.getRgb());
		}
		for (TextEditorView textEditorView : textEditorViews) {
			textEditorView.setHeaderColor(swatch);
		}
	}

	@OnCheckedChanged({
		R.id.want_to_buy,
		R.id.preordered,
		R.id.own,
		R.id.want_to_play,
		R.id.previously_owned,
		R.id.want_in_trade,
		R.id.for_trade,
		R.id.wishlist
	})
	void onStatusCheckChanged(CompoundButton view) {
		if (view.getVisibility() != View.VISIBLE) return;
		if (!isInEditMode) return;
		if (view == wishlistView) {
			wishlistPriorityView.setEnabled(wishlistView.isChecked());
		}
		updateStatuses();
	}

	@OnItemSelected(R.id.wishlist_priority)
	void onWishlistPrioritySelected() {
		if (wishlistPriorityView.getVisibility() != View.VISIBLE) return;
		if (!wishlistPriorityView.isEnabled()) return;
		if (!wishlistView.isChecked()) return;
		if (!isInEditMode) return;
		updateStatuses();
	}

	private void updateStatuses() {
		List<String> statuses = new ArrayList<>();
		for (CheckBox checkBox : statusViews) {
			if (checkBox.isChecked()) {
				String status = (String) checkBox.getTag();
				if (!TextUtils.isEmpty(status)) statuses.add(status);
			}
		}
		int wishlistPriority = wishlistView.isChecked() ?
			wishlistPriorityView.getSelectedItemPosition() + 1 : 0;
		UpdateCollectionItemStatusTask task =
			new UpdateCollectionItemStatusTask(getContext(),
				gameId, collectionId, internalId,
				statuses, wishlistPriority);
		TaskUtils.executeAsyncTask(task);
	}

	@DebugLog
	@OnClick(R.id.comment)
	public void onCommentClick() {
		onTextEditorClick(commentView, Collection.COMMENT, Collection.COMMENT_DIRTY_TIMESTAMP);
	}

	@DebugLog
	@OnClick(R.id.private_comment)
	public void onPrivateCommentClick() {
		onTextEditorClick(privateInfoCommentView, Collection.PRIVATE_INFO_COMMENT, Collection.PRIVATE_INFO_DIRTY_TIMESTAMP);
	}

	@DebugLog
	@OnClick(R.id.wishlist_comment)
	public void onWishlistCommentClick() {
		onTextEditorClick(wishlistCommentView, Collection.WISHLIST_COMMENT, Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP);
	}

	@DebugLog
	@OnClick(R.id.condition)
	public void onConditionClick() {
		onTextEditorClick(conditionView, Collection.CONDITION, Collection.TRADE_CONDITION_DIRTY_TIMESTAMP);
	}

	@DebugLog
	@OnClick(R.id.want_parts)
	public void onWantPartsClick() {
		onTextEditorClick(wantPartsView, Collection.WANTPARTS_LIST, Collection.WANT_PARTS_DIRTY_TIMESTAMP);
	}

	@DebugLog
	@OnClick(R.id.has_parts)
	public void onHasPartsClick() {
		onTextEditorClick(hasPartsView, Collection.HASPARTS_LIST, Collection.HAS_PARTS_DIRTY_TIMESTAMP);
	}

	@DebugLog
	private void onTextEditorClick(TextEditorView view, final String textColumn, final String timestampColumn) {
		EditCollectionTextDialogFragment dialogFragment = EditCollectionTextDialogFragment.newInstance(
			view.getHeaderText(),
			view.getContentText(),
			textColumn,
			timestampColumn);
		DialogUtils.showFragment(getActivity(), dialogFragment, view.toString());
	}

	@DebugLog
	@OnClick(R.id.private_info_edit_container)
	public void onPrivateInfoClick() {
		PrivateInfoDialogFragment privateInfoDialogFragment = PrivateInfoDialogFragment.newInstance();
		privateInfoDialogFragment.setPrivateInfo(new PrivateInfo(
			String.valueOf(editPrivateInfoView.getTag(R.id.priceCurrencyView)),
			getDoubleFromTag(editPrivateInfoView, R.id.priceView),
			String.valueOf(editPrivateInfoView.getTag(R.id.currentValueCurrencyView)),
			getDoubleFromTag(editPrivateInfoView, R.id.currentValueView),
			getIntFromTag(editPrivateInfoView, R.id.quantityView),
			String.valueOf(editPrivateInfoView.getTag(R.id.acquisitionDateView)),
			String.valueOf(editPrivateInfoView.getTag(R.id.acquiredFromView)),
			String.valueOf(editPrivateInfoView.getTag(R.id.inventoryLocationView))
		));
		DialogUtils.showAndSurvive(this, privateInfoDialogFragment);
	}

	private double getDoubleFromTag(View textView, @IdRes int key) {
		final Object tag = textView.getTag(key);
		if (tag == null) return 0.0;
		return (double) tag;
	}

	private int getIntFromTag(View textView, @IdRes int key) {
		final Object tag = textView.getTag(key);
		if (tag == null) return 1;
		return (int) tag;
	}

	@DebugLog
	public boolean triggerRefresh() {
		mightNeedRefreshing = false;
		if (!isRefreshing && gameId != BggContract.INVALID_ID) {
			isRefreshing = true;
			TaskUtils.executeAsyncTask(new SyncCollectionByGameTask((BggApplication) getActivity().getApplication(), gameId));
			return true;
		}
		return false;
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(CompletedEvent event) {
		if (event.getGameId() == gameId) {
			isRefreshing = false;
		}
	}

	@DebugLog
	private void updateUi(CollectionItem item) {
		notifyChange(item);
		bindMainContainer(item);
		bindWishlist(item);
		bindTrade(item);
		bindPrivateInfo(item);
		bindFooter(item);
		bindVisibility();
		isItemEditable = true;
	}

	@DebugLog
	private void notifyChange(CollectionItem item) {
		CollectionItemChangedEvent event = new CollectionItemChangedEvent(item.getName(), item.getThumbnailUrl(), item.getHeroImageUrl(), item.getYear());
		EventBus.getDefault().post(event);
	}

	private void bindMainContainer(CollectionItem item) {
		final String statusDescription = getStatusDescription(item);
		TextViewUtils.setTextOrHide(statusView, statusDescription);
		setVisibleTag(statusView, !TextUtils.isEmpty(statusDescription));

		wantToBuyView.setChecked(item.isWantToBuy());
		preorderedView.setChecked(item.isPreordered());
		ownView.setChecked(item.isOwn());
		wantToPlayView.setChecked(item.isWantToPlay());
		previouslyOwnedView.setChecked(item.isPreviouslyOwned());

		ratingView.setContent(item.getRating(), item.getRatingTimestamp(), gameId, collectionId, item.getInternalId());

		commentView.setContent(item.getComment(), item.getCommentTimestamp());
	}

	private void bindWishlist(CollectionItem item) {
		// view
		if (item.isWishlist()) {
			TextViewUtils.setTextOrHide(wishlistStatusView, IntUtils.asWishListPriority(item.getWishlistPriority(), getContext()));
		} else {
			wishlistStatusView.setVisibility(View.GONE);
		}
		setVisibleTag(wishlistStatusView, item.isWishlist());

		// edit
		if (wishlistPriorityView.getAdapter() == null)
			wishlistPriorityView.setAdapter(new WishlistPriorityAdapter(getContext()));
		if (item.isWishlist()) wishlistPriorityView.setSelection(item.getSafeWishlistPriority() - 1);
		wishlistPriorityView.setEnabled(item.isWishlist());
		wishlistView.setChecked(item.isWishlist());

		wishlistCommentView.setContent(item.getWishlistComment(), item.getWishlistCommentDirtyTimestamp());
	}

	private void bindTrade(CollectionItem item) {
		// view
		List<String> statusDescriptions = new ArrayList<>();
		if (item.isForTrade()) statusDescriptions.add(getString(R.string.collection_status_for_trade));
		if (item.isWantInTrade()) statusDescriptions.add(getString(R.string.collection_status_want_in_trade));
		TextViewUtils.setTextOrHide(tradeStatusView, StringUtils.formatList(statusDescriptions));
		setVisibleTag(tradeStatusView, item.isForTrade() || item.isWantInTrade());

		// edit
		wantInTradeView.setChecked(item.isWantInTrade());
		forTradeView.setChecked(item.isForTrade());

		// both
		conditionView.setContent(item.getCondition(), item.getTradeConditionDirtyTimestamp());
		wantPartsView.setContent(item.getWantParts(), item.getWantPartsDirtyTimestamp());
		hasPartsView.setContent(item.getHasParts(), item.getHasPartsDirtyTimestamp());
	}

	private void bindPrivateInfo(CollectionItem item) {
		// view
		TextViewUtils.setTextOrHide(viewPrivateInfoView, getPrivateInfo(item));
		setVisibleTag(viewPrivateInfoView, hasPrivateInfo(item));

		// edit
		privateInfoHintView.setVisibility(hasPrivateInfo(item) ? View.GONE : View.VISIBLE);
		editPrivateInfoView.setVisibility(hasPrivateInfo(item) ? View.VISIBLE : View.GONE);
		editPrivateInfoView.setText(getPrivateInfo(item));
		editPrivateInfoView.setTag(R.id.priceCurrencyView, item.getPriceCurrency());
		editPrivateInfoView.setTag(R.id.priceView, item.getPrice());
		editPrivateInfoView.setTag(R.id.currentValueCurrencyView, item.getCurrentValueCurrency());
		editPrivateInfoView.setTag(R.id.currentValueView, item.getCurrentValue());
		editPrivateInfoView.setTag(R.id.quantityView, item.getQuantity());
		editPrivateInfoView.setTag(R.id.acquisitionDateView, item.getAcquisitionDate());
		editPrivateInfoView.setTag(R.id.acquiredFromView, item.getAcquiredFrom());
		editPrivateInfoView.setTag(R.id.inventoryLocationView, item.getInventoryLocation());

		// both
		privateInfoCommentView.setContent(item.getPrivateComment(), item.getPrivateInfoTimestamp());
	}

	private void bindFooter(CollectionItem item) {
		final long defaultTimestamp = item.getStatusTimestamp() > 0 ? item.getStatusTimestamp() : item.getLastModifiedDateTime();
		final long timestamp = item.getDirtyTimestamp() > 0 ? item.getDirtyTimestamp() : defaultTimestamp;
		lastModifiedView.setTimestamp(timestamp);
		updatedView.setTimestamp(item.getUpdated());
		TextViewUtils.setTextOrHide(idView, String.valueOf(item.getId()));
	}

	private static void setVisibleTag(View view, boolean isVisible) {
		view.setTag(R.id.visibility, isVisible);
	}

	private static boolean getVisibleTag(View view) {
		final Object tag = view.getTag(R.id.visibility);
		return tag != null && (boolean) tag;
	}

	private String getStatusDescription(CollectionItem item) {
		List<String> statusDescriptions = new ArrayList<>();
		if (item.isOwn()) statusDescriptions.add(getString(R.string.collection_status_own));
		if (item.isPreviouslyOwned()) statusDescriptions.add(getString(R.string.collection_status_prev_owned));
		if (item.isWantToBuy()) statusDescriptions.add(getString(R.string.collection_status_want_to_buy));
		if (item.isWantToPlay()) statusDescriptions.add(getString(R.string.collection_status_want_to_play));
		if (item.isPreordered()) statusDescriptions.add(getString(R.string.collection_status_preordered));

		String status = StringUtils.formatList(statusDescriptions);
		if (TextUtils.isEmpty(status) && item.getNumberOfPlays() > 0) {
			return getString(R.string.played);
		}
		return status;
	}

	private CharSequence getPrivateInfo(CollectionItem item) {
		String initialText = getResources().getString(R.string.acquired);
		SpannableStringBuilder sb = new SpannableStringBuilder();
		sb.append(initialText);
		if (item.getQuantity() > 1) {
			sb.append(" ");
			StringUtils.appendBold(sb, String.valueOf(item.getQuantity()));
		}
		if (!TextUtils.isEmpty(item.getAcquisitionDate())) {
			String date = null;
			try {
				date = DateUtils.formatDateTime(getContext(), DateTimeUtils.getMillisFromApiDate(item.getAcquisitionDate(), 0), DateUtils.FORMAT_SHOW_DATE);
			} catch (Exception e) {
				Timber.w(e, "Could find a date in here: %s", item.getAcquisitionDate());
			}
			if (!TextUtils.isEmpty(date)) {
				sb.append(" ").append(getString(R.string.on)).append(" ");
				StringUtils.appendBold(sb, date);
			}
		}
		if (!TextUtils.isEmpty(item.getAcquiredFrom())) {
			sb.append(" ").append(getString(R.string.from)).append(" ");
			StringUtils.appendBold(sb, item.getAcquiredFrom());
		}
		if (item.getPrice() > 0.0) {
			sb.append(" ").append(getString(R.string.for_)).append(" ");
			StringUtils.appendBold(sb, DoubleUtils.asMoney(item.getPrice(), item.getPriceCurrency()));
		}
		if (item.getCurrentValue() > 0.0) {
			sb.append(" (").append(getString(R.string.currently_worth)).append(" ");
			StringUtils.appendBold(sb, DoubleUtils.asMoney(item.getCurrentValue(), item.getCurrentValueCurrency()));
			sb.append(")");
		}
		if (!TextUtils.isEmpty(item.getInventoryLocation())) {
			if (sb.toString().equals(initialText)) {
				sb.clear();
			} else {
				sb.append(". ");
			}
			sb.append(getString(R.string.located_in)).append(" ");
			StringUtils.appendBold(sb, item.getInventoryLocation());
		}

		if (sb.toString().equals(initialText)) {
			// shouldn't happen
			return "";
		}
		return sb;
	}

	static boolean hasPrivateInfo(CollectionItem item) {
		return item.getQuantity() > 1 ||
			!TextUtils.isEmpty(item.getAcquisitionDate()) ||
			!TextUtils.isEmpty(item.getAcquiredFrom()) ||
			item.getPrice() > 0.0 ||
			item.getCurrentValue() > 0.0 ||
			!TextUtils.isEmpty(item.getInventoryLocation());
	}

	private static class WishlistPriorityAdapter extends ArrayAdapter<String> {
		public WishlistPriorityAdapter(Context context) {
			super(context,
				android.R.layout.simple_spinner_item,
				context.getResources().getStringArray(R.array.wishlist_priority_finite));
			setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}
	}

	private void setVisibilityByChildren(ViewGroup view) {
		for (int i = 0; i < view.getChildCount(); i++) {
			final View child = view.getChildAt(i);
			if (setVisibilityByChild(view, child)) return;
		}
		view.setVisibility(View.GONE);
	}

	private static boolean setVisibilityByChild(@NonNull ViewGroup view, View child) {
		if (child instanceof ViewGroup) {
			String tag = (String) child.getTag();
			if (tag != null && tag.equals("container")) {
				ViewGroup childViewGroup = (ViewGroup) child;
				for (int j = 0; j < childViewGroup.getChildCount(); j++) {
					View grandchild = childViewGroup.getChildAt(j);
					if (setVisibilityByChild(view, grandchild)) return true;
				}
			} else {
				if (setVisibilityByChildView(view, child)) return true;
			}
		} else {
			if (setVisibilityByChildView(view, child)) return true;
		}
		return false;
	}

	private static boolean setVisibilityByChildView(View view, View v) {
		String tag = (String) v.getTag();
		if (tag != null && tag.equals("header")) return false;
		if (v.getVisibility() == View.VISIBLE) {
			view.setVisibility(View.VISIBLE);
			return true;
		}
		return false;
	}
}
