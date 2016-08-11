package com.boardgamegeek.ui;

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.CollectionItemChangedEvent;
import com.boardgamegeek.events.CollectionItemUpdatedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.tasks.UpdateCollectionItemCommentTask;
import com.boardgamegeek.tasks.UpdateCollectionItemPrivateInfoTask;
import com.boardgamegeek.tasks.UpdateCollectionItemRatingTask;
import com.boardgamegeek.tasks.UpdateCollectionItemStatusTask;
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment;
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment.CollectionStatusDialogListener;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment.EditTextDialogListener;
import com.boardgamegeek.ui.dialog.NumberPadDialogFragment;
import com.boardgamegeek.ui.dialog.PrivateInfoDialogFragment;
import com.boardgamegeek.ui.dialog.PrivateInfoDialogFragment.PrivateInfoDialogListener;
import com.boardgamegeek.ui.model.PrivateInfo;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.MathUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class GameCollectionFragment extends Fragment implements LoaderCallbacks<Cursor> {
	private static final int AGE_IN_DAYS_TO_REFRESH = 7;
	private static final DecimalFormat RATING_EDIT_FORMAT = new DecimalFormat("0.#");

	private Unbinder unbinder;
	@BindView(R.id.year) TextView year;
	@BindView(R.id.info_bar) View infoBar;
	@BindView(R.id.status_container) ViewGroup statusContainer;
	@BindView(R.id.status) TextView statusView;
	@BindView(R.id.last_modified) TimestampView lastModified;
	@BindView(R.id.rating_container) View ratingContainer;
	@BindView(R.id.rating) TextView rating;
	@BindView(R.id.rating_timestamp) TimestampView ratingTimestampView;
	@BindView(R.id.comment_container) ViewGroup commentContainer;
	@BindView(R.id.add_comment) View addCommentView;
	@BindView(R.id.comment) TextView comment;
	@BindView(R.id.comment_timestamp) TimestampView commentTimestampView;
	@BindView(R.id.private_info_container) ViewGroup privateInfoContainer;
	@BindView(R.id.private_info) TextView privateInfo;
	@BindView(R.id.private_info_comments) TextView privateInfoComments;
	@BindView(R.id.private_info_timestamp) TimestampView privateInfoTimestampView;
	@BindView(R.id.wishlist_container) View wishlistContainer;
	@BindView(R.id.wishlist_comment) TextView wishlistComment;
	@BindView(R.id.condition_container) View conditionContainer;
	@BindView(R.id.condition_comment) TextView conditionComment;
	@BindView(R.id.want_parts_container) View wantPartsContainer;
	@BindView(R.id.want_parts_comment) TextView wantPartsComment;
	@BindView(R.id.has_parts_container) View hasPartsContainer;
	@BindView(R.id.has_parts_comment) TextView hasPartsComment;
	@BindView(R.id.collection_id) TextView id;
	@BindView(R.id.updated) TimestampView updated;
	@BindViews({
		R.id.status,
		R.id.last_modified,
		R.id.year
	}) List<TextView> colorizedTextViews;
	@BindViews({
		R.id.add_comment,
		R.id.card_header_private_info,
		R.id.card_header_wishlist,
		R.id.card_header_condition,
		R.id.card_header_want_parts,
		R.id.card_header_has_parts
	}) List<TextView> colorizedHeaders;
	private CollectionStatusDialogFragment statusDialogFragment;
	private EditTextDialogFragment commentDialogFragment;
	private PrivateInfoDialogFragment privateInfoDialogFragment;

	private int gameId = BggContract.INVALID_ID;
	private int collectionId = BggContract.INVALID_ID;
	private long internalId = 0;
	private boolean mightNeedRefreshing;
	private Palette palette;
	private boolean needsUploading;

	@DebugLog
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		gameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		collectionId = intent.getIntExtra(ActivityUtils.KEY_COLLECTION_ID, BggContract.INVALID_ID);
	}

	@DebugLog
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game_collection, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		colorize(palette);

		mightNeedRefreshing = true;
		getLoaderManager().restartLoader(CollectionItem._TOKEN, getArguments(), this);

		return rootView;
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
		if (needsUploading) {
			SyncService.sync(getActivity(), SyncService.FLAG_SYNC_COLLECTION_UPLOAD);
			needsUploading = false;
		}
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@DebugLog
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@DebugLog
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		if (id != CollectionItem._TOKEN || collectionId == BggContract.INVALID_ID) {
			return null;
		}
		if (collectionId != 0) {
			return new CursorLoader(getActivity(),
				Collection.CONTENT_URI,
				new CollectionItem().PROJECTION,
				Collection.COLLECTION_ID + "=?",
				new String[] { String.valueOf(collectionId) },
				null);
		} else {
			return new CursorLoader(getActivity(),
				Collection.CONTENT_URI,
				new CollectionItem().PROJECTION,
				"collection." + Collection.GAME_ID + "=? AND " + Collection.COLLECTION_ID + " IS NULL",
				new String[] { String.valueOf(gameId) },
				null);
		}
	}

	@DebugLog
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (loader.getId() == CollectionItem._TOKEN) {
			if (cursor == null || !cursor.moveToFirst()) {
				if (mightNeedRefreshing) {
					triggerRefresh();
				}
				return;
			}

			CollectionItem item = new CollectionItem(cursor);
			internalId = item.internalId;
			updateUi(item);

			if (mightNeedRefreshing) {
				long u = cursor.getLong(new CollectionItem().UPDATED);
				if (DateTimeUtils.howManyDaysOld(u) > AGE_IN_DAYS_TO_REFRESH) {
					triggerRefresh();
				}
			}
			mightNeedRefreshing = false;
		} else {
			Timber.d("Query complete, Not Actionable: " + loader.getId());
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@DebugLog
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@DebugLog
	@Subscribe
	public void onEvent(CollectionItemUpdatedEvent event) {
		needsUploading = true;
	}

	@DebugLog
	public void onPaletteGenerated(Palette palette) {
		this.palette = palette;
		colorize(palette);
	}

	@DebugLog
	private void colorize(Palette palette) {
		if (palette == null || !isAdded()) {
			return;
		}
		@SuppressWarnings("deprecation") Palette.Swatch swatch = PaletteUtils.getInverseSwatch(palette, getResources().getColor(R.color.info_background));
		infoBar.setBackgroundColor(swatch.getRgb());
		ButterKnife.apply(colorizedTextViews, PaletteUtils.colorTextViewOnBackgroundSetter, swatch);
		swatch = PaletteUtils.getHeaderSwatch(palette);
		ButterKnife.apply(colorizedHeaders, PaletteUtils.colorTextViewSetter, swatch);
	}

	@OnClick(R.id.status_container)
	public void onStatusClick() {
		ensureCollectionStatusDialogFragment();
		//noinspection unchecked
		statusDialogFragment.setSelectedStatuses((List<String>) statusView.getTag(R.id.status));
		statusDialogFragment.setWishlistPriority((int) statusView.getTag(R.id.wishlist_priority));
		DialogUtils.showFragment(getActivity(), statusDialogFragment, "status_dialog");
	}

	@DebugLog
	private void ensureCollectionStatusDialogFragment() {
		if (statusDialogFragment == null) {
			statusDialogFragment = CollectionStatusDialogFragment.newInstance(
				statusContainer,
				new CollectionStatusDialogListener() {
					@Override
					public void onSelectStatuses(List<String> selectedStatuses, int wishlistPriority) {
						UpdateCollectionItemStatusTask task =
							new UpdateCollectionItemStatusTask(getActivity(),
								gameId, collectionId, internalId,
								selectedStatuses, wishlistPriority);
						TaskUtils.executeAsyncTask(task);
					}
				}
			);
		}
	}

	@DebugLog
	@OnClick(R.id.comment_container)
	public void onCommentClick() {
		ensureCommentDialogFragment();
		commentDialogFragment.setText(comment.getText().toString());
		DialogUtils.showFragment(getActivity(), commentDialogFragment, "comment_dialog");
	}

	@DebugLog
	private void ensureCommentDialogFragment() {
		if (commentDialogFragment == null) {
			commentDialogFragment = EditTextDialogFragment.newLongFormInstance(
				R.string.title_comments,
				commentContainer,
				new EditTextDialogListener() {
					@Override
					public void onFinishEditDialog(String inputText) {
						UpdateCollectionItemCommentTask task =
							new UpdateCollectionItemCommentTask(getActivity(), gameId, collectionId, internalId, inputText);
						TaskUtils.executeAsyncTask(task);
					}
				}
			);
		}
	}

	@DebugLog
	@OnClick(R.id.rating_container)
	public void onRatingClick() {
		String output = RATING_EDIT_FORMAT.format((double) rating.getTag());
		if ("0".equals(output)) {
			output = "";
		}
		final NumberPadDialogFragment fragment = NumberPadDialogFragment.newInstance(getString(R.string.rating), output);
		fragment.setMinValue(1.0);
		fragment.setMaxValue(10.0);
		fragment.setMaxMantissa(6);
		fragment.setOnDoneClickListener(new NumberPadDialogFragment.OnClickListener() {
			@Override
			public void onDoneClick(String output) {
				double rating = StringUtils.parseDouble(output);
				UpdateCollectionItemRatingTask task =
					new UpdateCollectionItemRatingTask(getActivity(), gameId, collectionId, internalId, rating);
				TaskUtils.executeAsyncTask(task);
			}
		});
		DialogUtils.showFragment(getActivity(), fragment, "rating_dialog");
	}

	@DebugLog
	@OnClick(R.id.private_info_container)
	public void onPrivateInfoClick() {
		ensurePrivateInfoDialogFragment();
		privateInfoDialogFragment.setPriceCurrency(String.valueOf(privateInfo.getTag(R.id.price_currency)));
		privateInfoDialogFragment.setPrice((double) privateInfo.getTag(R.id.price));
		privateInfoDialogFragment.setCurrentValueCurrency(String.valueOf(privateInfo.getTag(R.id.current_value_currency)));
		privateInfoDialogFragment.setCurrentValue((double) privateInfo.getTag(R.id.current_value));
		privateInfoDialogFragment.setQuantity((int) privateInfo.getTag(R.id.quantity));
		privateInfoDialogFragment.setAcquisitionDate(String.valueOf(privateInfo.getTag(R.id.acquisition_date)));
		privateInfoDialogFragment.setAcquiredFrom(String.valueOf(privateInfo.getTag(R.id.acquired_from)));
		privateInfoDialogFragment.setComment(privateInfoComments.getText().toString());
		DialogUtils.showFragment(getActivity(), privateInfoDialogFragment, "private_info_dialog");
	}

	@DebugLog
	private void ensurePrivateInfoDialogFragment() {
		if (privateInfoDialogFragment == null) {
			privateInfoDialogFragment = PrivateInfoDialogFragment.newInstance(
				privateInfoContainer,
				new PrivateInfoDialogListener() {
					@Override
					public void onFinishEditDialog(PrivateInfo privateInfo) {
						UpdateCollectionItemPrivateInfoTask task =
							new UpdateCollectionItemPrivateInfoTask(getActivity(), gameId, collectionId, internalId, privateInfo);
						TaskUtils.executeAsyncTask(task);
					}
				}
			);
		}
	}

	@DebugLog
	public void triggerRefresh() {
		mightNeedRefreshing = false;
		if (gameId != BggContract.INVALID_ID) {
			UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_GAME_COLLECTION, gameId);
		}
	}

	@DebugLog
	private void notifyChange(CollectionItem item) {
		CollectionItemChangedEvent event = new CollectionItemChangedEvent(item.name, item.imageUrl, item.imageUrl);
		EventBus.getDefault().post(event);
	}

	@DebugLog
	private void updateUi(CollectionItem item) {
		notifyChange(item);

		year.setText(item.getYearDescription());
		lastModified.setTimestamp(item.dirtyTimestamp > 0 ? item.dirtyTimestamp :
			item.statusTimestamp > 0 ? item.statusTimestamp : item.lastModifiedDateTime);
		ratingContainer.setClickable(collectionId != 0);
		rating.setText(item.getRatingDescription());
		rating.setTag(MathUtils.constrain(item.rating, 0.0, 10.0));
		ColorUtils.setViewBackground(rating, ColorUtils.getRatingColor(item.rating));
		ratingTimestampView.setTimestamp(item.ratingTimestamp);

		statusView.setText(item.getStatusDescription());
		statusView.setTag(R.id.status, item.getStatuses());
		statusView.setTag(R.id.wishlist_priority, item.getWishlistPriority());
		commentContainer.setClickable(collectionId != 0);
		addCommentView.setVisibility(TextUtils.isEmpty(item.comment) ? View.VISIBLE : View.GONE);
		PresentationUtils.setTextOrHide(comment, item.comment);
		commentTimestampView.setTimestamp(item.commentTimestamp);

		privateInfoContainer.setClickable(collectionId != 0);
		privateInfo.setVisibility(item.hasPrivateInfo() ? View.VISIBLE : View.GONE);
		privateInfo.setText(item.getPrivateInfo());
		privateInfo.setTag(R.id.price_currency, item.getPriceCurrency());
		privateInfo.setTag(R.id.price, item.getPrice());
		privateInfo.setTag(R.id.current_value_currency, item.getCurrentValueCurrency());
		privateInfo.setTag(R.id.current_value, item.getCurrentValue());
		privateInfo.setTag(R.id.quantity, item.getQuantity());
		privateInfo.setTag(R.id.acquisition_date, item.getAcquisitionDate());
		privateInfo.setTag(R.id.acquired_from, item.getAcquiredFrom());
		PresentationUtils.setTextOrHide(privateInfoComments, item.privateComment);
		privateInfoTimestampView.setTimestamp(item.privateInfoTimestamp);

		showSection(item.wishlistComment, wishlistContainer, wishlistComment);
		showSection(item.condition, conditionContainer, conditionComment);
		showSection(item.wantParts, wantPartsContainer, wantPartsComment);
		showSection(item.hasParts, hasPartsContainer, hasPartsComment);

		id.setText(String.valueOf(item.id));
		id.setVisibility(item.id == 0 ? View.INVISIBLE : View.VISIBLE);
		updated.setTimestamp(item.updated);
	}

	@DebugLog
	private void showSection(String text, View container, TextView comment) {
		container.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
		comment.setText(text);
	}

	private class CollectionItem {
		static final int _TOKEN = 0x31;

		final String[] PROJECTION = { Collection._ID, Collection.COLLECTION_ID, Collection.COLLECTION_NAME,
			Collection.COLLECTION_SORT_NAME, Collection.COMMENT, Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
			Collection.PRIVATE_INFO_PRICE_PAID, Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
			Collection.PRIVATE_INFO_CURRENT_VALUE, Collection.PRIVATE_INFO_QUANTITY,
			Collection.PRIVATE_INFO_ACQUISITION_DATE, Collection.PRIVATE_INFO_ACQUIRED_FROM,
			Collection.PRIVATE_INFO_COMMENT, Collection.LAST_MODIFIED, Collection.COLLECTION_THUMBNAIL_URL,
			Collection.COLLECTION_IMAGE_URL, Collection.COLLECTION_YEAR_PUBLISHED, Collection.CONDITION,
			Collection.HASPARTS_LIST, Collection.WANTPARTS_LIST, Collection.WISHLIST_COMMENT, Collection.RATING,
			Collection.UPDATED, Collection.STATUS_OWN, Collection.STATUS_PREVIOUSLY_OWNED, Collection.STATUS_FOR_TRADE,
			Collection.STATUS_WANT, Collection.STATUS_WANT_TO_BUY, Collection.STATUS_WISHLIST,
			Collection.STATUS_WANT_TO_PLAY, Collection.STATUS_PREORDERED, Collection.STATUS_WISHLIST_PRIORITY,
			Collection.NUM_PLAYS, Collection.RATING_DIRTY_TIMESTAMP, Collection.COMMENT_DIRTY_TIMESTAMP,
			Collection.PRIVATE_INFO_DIRTY_TIMESTAMP, Collection.STATUS_DIRTY_TIMESTAMP, Collection.COLLECTION_DIRTY_TIMESTAMP };

		final int _ID = 0;
		final int COLLECTION_ID = 1;
		final int COLLECTION_NAME = 2;
		// int COLLECTION_SORT_NAME = 3;
		final int COMMENT = 4;
		final int PRIVATE_INFO_PRICE_PAID_CURRENCY = 5;
		final int PRIVATE_INFO_PRICE_PAID = 6;
		final int PRIVATE_INFO_CURRENT_VALUE_CURRENCY = 7;
		final int PRIVATE_INFO_CURRENT_VALUE = 8;
		final int PRIVATE_INFO_QUANTITY = 9;
		final int PRIVATE_INFO_ACQUISITION_DATE = 10;
		final int PRIVATE_INFO_ACQUIRED_FROM = 11;
		final int PRIVATE_INFO_COMMENT = 12;
		final int LAST_MODIFIED = 13;
		// int COLLECTION_THUMBNAIL_URL = 14;
		final int COLLECTION_IMAGE_URL = 15;
		final int COLLECTION_YEAR_PUBLISHED = 16;
		final int CONDITION = 17;
		final int HAS_PARTS_LIST = 18;
		final int WANT_PARTS_LIST = 19;
		final int WISHLIST_COMMENT = 20;
		final int RATING = 21;
		final int UPDATED = 22;
		final int STATUS_OWN = 23;
		// int STATUS_PREVIOUSLY_OWNED = 24;
		// int STATUS_FOR_TRADE = 25;
		// int STATUS_WANT = 26;
		// int STATUS_WANT_TO_BUY = 27;
		final int STATUS_WISHLIST = 28;
		// int STATUS_WANT_TO_PLAY = 29;
		final int STATUS_PREORDERED = 30;
		final int STATUS_WISHLIST_PRIORITY = 31;
		final int NUM_PLAYS = 32;
		final int RATING_DIRTY_TIMESTAMP = 33;
		final int COMMENT_DIRTY_TIMESTAMP = 34;
		final int PRIVATE_INFO_DIRTY_TIMESTAMP = 35;
		final int STATUS_DIRTY_TIMESTAMP = 36;
		final int COLLECTION_DIRTY_TIMESTAMP = 37;

		Resources r;
		int id;
		private long internalId;
		String name;
		// String sortName;
		private String comment;
		private long commentTimestamp;
		private long lastModifiedDateTime;
		private double rating;
		private long ratingTimestamp;
		private long updated;
		private String priceCurrency;
		private double price;
		private String currentValueCurrency;
		private double currentValue;
		int quantity;
		private String acquiredFrom;
		private String acquisitionDate;
		String privateComment;
		private long privateInfoTimestamp;
		private long statusTimestamp;
		String imageUrl;
		private int year;
		String condition;
		String wantParts;
		String hasParts;
		int wishlistPriority;
		String wishlistComment;
		int numPlays;
		private ArrayList<String> statusDescriptions;
		private ArrayList<String> statuses;
		private long dirtyTimestamp;

		public CollectionItem() {
			// TODO: delete this, here just to get the projection; gotta be a better way
		}

		public CollectionItem(Cursor cursor) {
			r = getResources();
			id = cursor.getInt(COLLECTION_ID);
			internalId = cursor.getLong(_ID);
			name = cursor.getString(COLLECTION_NAME);
			// sortName = cursor.getString(COLLECTION_SORT_NAME);
			comment = cursor.getString(COMMENT);
			commentTimestamp = cursor.getLong(COMMENT_DIRTY_TIMESTAMP);
			rating = cursor.getDouble(RATING);
			ratingTimestamp = cursor.getLong(RATING_DIRTY_TIMESTAMP);
			lastModifiedDateTime = cursor.getLong(LAST_MODIFIED);
			updated = cursor.getLong(UPDATED);
			priceCurrency = cursor.getString(PRIVATE_INFO_PRICE_PAID_CURRENCY);
			price = cursor.getDouble(PRIVATE_INFO_PRICE_PAID);
			currentValueCurrency = cursor.getString(PRIVATE_INFO_CURRENT_VALUE_CURRENCY);
			currentValue = cursor.getDouble(PRIVATE_INFO_CURRENT_VALUE);
			quantity = cursor.getInt(PRIVATE_INFO_QUANTITY);
			privateComment = cursor.getString(PRIVATE_INFO_COMMENT);
			acquiredFrom = cursor.getString(PRIVATE_INFO_ACQUIRED_FROM);
			acquisitionDate = cursor.getString(PRIVATE_INFO_ACQUISITION_DATE);
			privateInfoTimestamp = cursor.getLong(PRIVATE_INFO_DIRTY_TIMESTAMP);
			statusTimestamp = cursor.getLong(STATUS_DIRTY_TIMESTAMP);
			imageUrl = cursor.getString(COLLECTION_IMAGE_URL);
			year = cursor.getInt(COLLECTION_YEAR_PUBLISHED);
			wishlistPriority = cursor.getInt(STATUS_WISHLIST_PRIORITY);
			wishlistComment = cursor.getString(WISHLIST_COMMENT);
			condition = cursor.getString(CONDITION);
			wantParts = cursor.getString(WANT_PARTS_LIST);
			hasParts = cursor.getString(HAS_PARTS_LIST);
			numPlays = cursor.getInt(NUM_PLAYS);
			dirtyTimestamp = cursor.getLong(COLLECTION_DIRTY_TIMESTAMP);

			statuses = new ArrayList<>();
			statusDescriptions = new ArrayList<>();
			for (int i = STATUS_OWN; i <= STATUS_PREORDERED; i++) {
				if (cursor.getInt(i) == 1) {
					statuses.add(r.getStringArray(R.array.collection_status_filter_values)[i - STATUS_OWN]);
					if (i == STATUS_WISHLIST) {
						statusDescriptions.add(getWishlistPriorityDescription());
					} else {
						statusDescriptions.add(r.getStringArray(R.array.collection_status_filter_entries)[i - STATUS_OWN]);
					}
				}
			}
		}

		String getStatusDescription() {
			String status = StringUtils.formatList(statusDescriptions);
			if (TextUtils.isEmpty(status)) {
				if (numPlays > 0) {
					return r.getString(R.string.played);
				}
				return r.getString(R.string.invalid_collection_status);
			}
			return status;
		}

		List<String> getStatuses() {
			return statuses;
		}

		String getRatingDescription() {
			return PresentationUtils.describePersonalRating(getActivity(), rating);
		}

		String getYearDescription() {
			return PresentationUtils.describeYear(getActivity(), year);
		}

		int getWishlistPriority() {
			return wishlistPriority;
		}

		String getWishlistPriorityDescription() {
			return PresentationUtils.describeWishlist(getActivity(), wishlistPriority);
		}

		String getPriceDescription() {
			return PresentationUtils.describeMoney(priceCurrency, price);
		}

		String getCurrentValueDescription() {
			return PresentationUtils.describeMoney(currentValueCurrency, currentValue);
		}

		boolean hasPrivateInfo() {
			return hasQuantity() || hasAcquisitionDate() || hasAcquiredFrom() || hasPrice() || hasValue();
		}

		int getQuantity() {
			return quantity;
		}

		boolean hasQuantity() {
			return quantity > 1;
		}

		String getAcquisitionDate() {
			return acquisitionDate;
		}

		boolean hasAcquisitionDate() {
			return !TextUtils.isEmpty(acquisitionDate);
		}

		String getAcquiredFrom() {
			return acquiredFrom;
		}

		boolean hasAcquiredFrom() {
			return !TextUtils.isEmpty(acquiredFrom);
		}

		String getPriceCurrency() {
			return priceCurrency;
		}

		double getPrice() {
			return price;
		}

		boolean hasPrice() {
			return price > 0.0;
		}

		String getCurrentValueCurrency() {
			return currentValueCurrency;
		}

		double getCurrentValue() {
			return currentValue;
		}

		boolean hasValue() {
			return currentValue > 0.0;
		}

		CharSequence getPrivateInfo() {
			String initialText = r.getString(R.string.acquired);
			SpannableStringBuilder sb = new SpannableStringBuilder();
			sb.append(initialText);
			if (hasQuantity()) {
				sb.append(" ");
				StringUtils.appendBold(sb, String.valueOf(quantity));
			}
			if (hasAcquisitionDate()) {
				String date = null;
				try {
					date = DateUtils.formatDateTime(getContext(), DateTimeUtils.getMillisFromApiDate(acquisitionDate, 0), DateUtils.FORMAT_SHOW_DATE);
				} catch (Exception e) {
					Timber.w(e, "Could find a date in here: " + acquisitionDate);
				}
				if (!TextUtils.isEmpty(date)) {
					sb.append(" ").append(r.getString(R.string.on)).append(" ");
					StringUtils.appendBold(sb, date);
				}
			}
			if (hasAcquiredFrom()) {
				sb.append(" ").append(r.getString(R.string.from)).append(" ");
				StringUtils.appendBold(sb, acquiredFrom);
			}
			if (hasPrice()) {
				sb.append(" ").append(r.getString(R.string.for_)).append(" ");
				StringUtils.appendBold(sb, getPriceDescription());
			}
			if (hasValue()) {
				sb.append(" (").append(r.getString(R.string.currently_worth)).append(" ");
				StringUtils.appendBold(sb, getCurrentValueDescription());
				sb.append(")");
			}

			if (sb.toString().equals(initialText)) {
				// shouldn't happen
				return null;
			}
			return sb;
		}
	}
}
