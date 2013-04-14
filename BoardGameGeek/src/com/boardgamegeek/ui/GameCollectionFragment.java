package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.ui.widget.BezelImageView;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ImageFetcher;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class GameCollectionFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = makeLogTag(GameCollectionFragment.class);

	private int mGameId = BggContract.INVALID_ID;
	private CursorAdapter mAdapter;
	private ImageFetcher mImageFetcher;
	private boolean mMightNeedRefreshing;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mImageFetcher = UIUtils.getImageFetcher(getActivity());
		mImageFetcher.setImageFadeIn(false);
		mImageFetcher.setLoadingImage(R.drawable.thumbnail_image_empty);
		mImageFetcher.setImageSize((int) getResources().getDimension(R.dimen.thumbnail_list_size));
	}

	@Override
	public void onPause() {
		super.onPause();
		mImageFetcher.setPauseWork(false);
		mImageFetcher.flushCache();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mImageFetcher.closeCache();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);
		final ListView listView = getListView();
		listView.setSelector(android.R.color.transparent);
		listView.setCacheColorHint(Color.WHITE);
		listView.setFastScrollEnabled(true);
		listView.setDividerHeight(24);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_game_collection));
		setListShown(false);
		Uri uri = UIUtils.fragmentArgumentsToIntent(getArguments()).getData();
		if (uri != null && Games.isGameUri(uri)) {
			mMightNeedRefreshing = true;
			mGameId = Games.getGameId(uri);
			getLoaderManager().restartLoader(CollectionItem._TOKEN, getArguments(), this);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			triggerRefresh();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		if (id != CollectionItem._TOKEN) {
			return null;
		}
		// TODO: don't use table name directly
		return new CursorLoader(getActivity(), Collection.CONTENT_URI, new CollectionItem().PROJECTION, "collection."
			+ Collection.GAME_ID + "=?", new String[] { String.valueOf(mGameId) }, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			if (mMightNeedRefreshing) {
				triggerRefresh();
			}
			return;
		}

		if (getActivity() == null) {
			return;
		}

		if (mAdapter == null) {
			mAdapter = new CollectionAdapter(getActivity());
			setListAdapter(mAdapter);
		}

		int token = loader.getId();
		if (token == CollectionItem._TOKEN) {
			mAdapter.changeCursor(cursor);
			if (mMightNeedRefreshing) {
				cursor.moveToFirst();
				do {
					long u = cursor.getLong(new CollectionItem().UPDATED);
					if (DateTimeUtils.howManyDaysOld(u) > 1) {
						triggerRefresh();
						break;
					}
				} while (cursor.moveToNext());
				cursor.moveToPosition(-1);
			}
		} else {
			LOGD(TAG, "Query complete, Not Actionable: " + token);
			cursor.close();
		}

		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}

		mMightNeedRefreshing = false;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.changeCursor(null);
	}

	private void triggerRefresh() {
		mMightNeedRefreshing = false;
		UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_GAME_COLLECTION, mGameId, null);
	}

	private class CollectionAdapter extends CursorAdapter {
		private LayoutInflater mInflater;

		public CollectionAdapter(Context context) {
			super(context, null, false);
			mInflater = getActivity().getLayoutInflater();
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(R.layout.row_game_collection, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			CollectionItem item = new CollectionItem(cursor);

			holder.name.setText(item.name.trim());
			holder.rating.setText(item.getRatingDescription());
			holder.ratingBar.setRating((float) item.rating);
			holder.rating.setVisibility(item.hasRating() ? View.VISIBLE : View.GONE);
			holder.ratingBar.setVisibility(item.hasRating() ? View.VISIBLE : View.GONE);
			holder.ratingDenominator.setVisibility(item.hasRating() ? View.VISIBLE : View.GONE);
			holder.unrated.setVisibility(item.hasRating() ? View.GONE : View.VISIBLE);
			holder.id.setText(String.valueOf(item.id));
			holder.lastModified.setText(item.getLastModifiedDescription());
			holder.updated.setText(item.getUpdatedDescription());
			holder.year.setText(item.getYearDescription());

			holder.status.setText(item.getStatus());
			holder.comment.setVisibility(TextUtils.isEmpty(item.comment) ? View.GONE : View.VISIBLE);
			holder.comment.setText(item.comment);

			// Private info
			if (item.hasPrivateInfo()) {
				holder.privateInfoRoot.setVisibility(View.VISIBLE);
				holder.price.setVisibility(item.hasPrice() ? View.VISIBLE : View.GONE);
				holder.price.setText(getString(R.string.price) + ": " + item.getPrice());
				holder.currentValue.setVisibility(item.hasValue() ? View.VISIBLE : View.GONE);
				holder.currentValue.setText(getString(R.string.value) + ": " + item.getValue());
				holder.quantity.setVisibility(item.hasQuantity() ? View.VISIBLE : View.GONE);
				holder.quantity.setText(getString(R.string.quantity) + ": " + item.quantity);
				holder.acquisition.setVisibility(item.hasAcquisition() ? View.VISIBLE : View.GONE);
				holder.acquisition.setText(item.getAcquistionDescription());
				holder.privateComments.setVisibility(item.hasPrivateComment() ? View.VISIBLE : View.GONE);
				holder.privateComments.setText(item.privateComment);
			} else {
				holder.privateInfoRoot.setVisibility(View.GONE);
			}

			holder.wishlistRoot.setVisibility(TextUtils.isEmpty(item.wishlistComment) ? View.GONE : View.VISIBLE);
			holder.wishlistContent.setText(item.wishlistComment);
			holder.conditionRoot.setVisibility(TextUtils.isEmpty(item.condition) ? View.GONE : View.VISIBLE);
			holder.conditionContent.setText(item.condition);
			holder.wantPartsRoot.setVisibility(TextUtils.isEmpty(item.wantParts) ? View.GONE : View.VISIBLE);
			holder.wantPartsContent.setText(item.wantParts);
			holder.hasPartsRoot.setVisibility(TextUtils.isEmpty(item.hasParts) ? View.GONE : View.VISIBLE);
			holder.hasPartsContent.setText(item.hasParts);

			mImageFetcher.loadThumnailImage(item.thumbnailUrl, Collection.buildThumbnailUri(item.id), holder.thumbnail);
		}
	}

	static class ViewHolder {
		BezelImageView thumbnail;
		TextView name;
		TextView rating;
		RatingBar ratingBar;
		TextView ratingDenominator;
		TextView unrated;
		TextView id;
		TextView lastModified;
		TextView updated;
		TextView year;

		TextView status;

		TextView comment;

		View privateInfoRoot;
		TextView price;
		TextView currentValue;
		TextView quantity;
		TextView acquisition;
		TextView privateComments;

		View wishlistRoot;
		TextView wishlistContent;
		View conditionRoot;
		TextView conditionContent;
		View wantPartsRoot;
		TextView wantPartsContent;
		View hasPartsRoot;
		TextView hasPartsContent;

		public ViewHolder(View view) {
			thumbnail = (BezelImageView) view.findViewById(R.id.thumbnail);
			name = (TextView) view.findViewById(R.id.name);
			rating = (TextView) view.findViewById(R.id.rating);
			ratingBar = (RatingBar) view.findViewById(R.id.rating_stars);
			ratingDenominator = (TextView) view.findViewById(R.id.rating_denominator);
			unrated = (TextView) view.findViewById(R.id.rating_unrated);
			id = (TextView) view.findViewById(R.id.collection_id);
			lastModified = (TextView) view.findViewById(R.id.last_modified);
			updated = (TextView) view.findViewById(R.id.updated);
			year = (TextView) view.findViewById(R.id.year);

			status = (TextView) view.findViewById(R.id.status);
			comment = (TextView) view.findViewById(R.id.comment);

			privateInfoRoot = view.findViewById(R.id.private_info);
			price = (TextView) view.findViewById(R.id.price);
			currentValue = (TextView) view.findViewById(R.id.current_value);
			quantity = (TextView) view.findViewById(R.id.quantity);
			acquisition = (TextView) view.findViewById(R.id.acquisition);
			privateComments = (TextView) view.findViewById(R.id.private_comments);

			wishlistRoot = view.findViewById(R.id.wishlist_root);
			wishlistContent = (TextView) view.findViewById(R.id.wishlist_content);
			conditionRoot = view.findViewById(R.id.condition_root);
			conditionContent = (TextView) view.findViewById(R.id.condition_content);
			wantPartsRoot = view.findViewById(R.id.want_parts_root);
			wantPartsContent = (TextView) view.findViewById(R.id.want_parts_content);
			hasPartsRoot = view.findViewById(R.id.has_parts_root);
			hasPartsContent = (TextView) view.findViewById(R.id.has_parts_content);
		}
	}

	private class CollectionItem {
		static final int _TOKEN = 0x31;

		String[] PROJECTION = { Collection._ID, Collection.COLLECTION_ID, Collection.COLLECTION_NAME,
			Collection.COLLECTION_SORT_NAME, Collection.COMMENT, Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
			Collection.PRIVATE_INFO_PRICE_PAID, Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
			Collection.PRIVATE_INFO_CURRENT_VALUE, Collection.PRIVATE_INFO_QUANTITY,
			Collection.PRIVATE_INFO_ACQUISITION_DATE, Collection.PRIVATE_INFO_ACQUIRED_FROM,
			Collection.PRIVATE_INFO_COMMENT, Collection.LAST_MODIFIED, Collection.COLLECTION_THUMBNAIL_URL,
			Collection.COLLECTION_IMAGE_URL, Collection.COLLECTION_YEAR_PUBLISHED, Collection.CONDITION,
			Collection.HASPARTS_LIST, Collection.WANTPARTS_LIST, Collection.WISHLIST_COMMENT, Collection.RATING,
			Collection.UPDATED, Collection.STATUS_OWN, Collection.STATUS_PREVIOUSLY_OWNED, Collection.STATUS_FOR_TRADE,
			Collection.STATUS_WANT, Collection.STATUS_WANT_TO_BUY, Collection.STATUS_WISHLIST,
			Collection.STATUS_WANT_TO_PLAY, Collection.STATUS_PREORDERED, Collection.STATUS_WISHLIST_PRIORITY };

		int COLLECTION_ID = 1;
		int COLLECTION_NAME = 2;
		// int COLLECTION_SORT_NAME = 3;
		int COMMENT = 4;
		int PRIVATE_INFO_PRICE_PAID_CURRENCY = 5;
		int PRIVATE_INFO_PRICE_PAID = 6;
		int PRIVATE_INFO_CURRENT_VALUE_CURRENCY = 7;
		int PRIVATE_INFO_CURRENT_VALUE = 8;
		int PRIVATE_INFO_QUANTITY = 9;
		int PRIVATE_INFO_ACQUISITION_DATE = 10;
		int PRIVATE_INFO_ACQUIRED_FROM = 11;
		int PRIVATE_INFO_COMMENT = 12;
		int LAST_MODIFIED = 13;
		int COLLECTION_THUMBNAIL_URL = 14;
		// int COLLECTION_IMAGE_URL = 15;
		int COLLECTION_YEAR_PUBLISHED = 16;
		int CONDITION = 17;
		int HASPARTS_LIST = 18;
		int WANTPARTS_LIST = 19;
		int WISHLIST_COMMENT = 20;
		int RATING = 21;
		int UPDATED = 22;
		int STATUS_OWN = 23;
		// int STATUS_PREVIOUSLY_OWNED = 24;
		// int STATUS_FOR_TRADE = 25;
		// int STATUS_WANT = 26;
		// int STATUS_WANT_TO_BUY = 27;
		int STATUS_WISHLIST = 28;
		// int STATUS_WANT_TO_PLAY = 29;
		int STATUS_PREORDERED = 30;
		int STATUS_WISHLIST_PRIORITY = 31;

		DecimalFormat currencyFormat = new DecimalFormat("#.##");

		Resources r;
		int id;
		String name;
		// String sortName;
		String comment;
		private String lastModifiedDateTime;
		private double rating;
		private long lastModified;
		private long updated;
		private String priceCurrency;
		private double price;
		private String currentValueCurrency;
		private double currentValue;
		int quantity;
		private String acquiredFrom;
		private String acquisitionDate;
		String privateComment;
		String thumbnailUrl;
		// TODO: enable clicking on thumbnail
		// String imageUrl;
		private int year;
		String condition;
		String wantParts;
		String hasParts;
		int wishlistPriority;
		String wishlistComment;

		private ArrayList<String> mStatus;

		public CollectionItem() {
			// TODO: delete this, here just to get the projection; gotta be a better way
		}

		public CollectionItem(Cursor cursor) {
			r = getResources();
			id = cursor.getInt(COLLECTION_ID);
			name = cursor.getString(COLLECTION_NAME);
			// sortName = cursor.getString(COLLECTION_SORT_NAME);
			comment = cursor.getString(COMMENT);
			lastModifiedDateTime = cursor.getString(LAST_MODIFIED);
			rating = cursor.getDouble(RATING);
			if (!TextUtils.isEmpty(lastModifiedDateTime) && TextUtils.isDigitsOnly(lastModifiedDateTime)) {
				this.lastModified = Long.parseLong(lastModifiedDateTime);
				lastModifiedDateTime = null;
			}
			updated = cursor.getLong(UPDATED);
			priceCurrency = cursor.getString(PRIVATE_INFO_PRICE_PAID_CURRENCY);
			price = cursor.getDouble(PRIVATE_INFO_PRICE_PAID);
			currentValueCurrency = cursor.getString(PRIVATE_INFO_CURRENT_VALUE_CURRENCY);
			currentValue = cursor.getDouble(PRIVATE_INFO_CURRENT_VALUE);
			quantity = cursor.getInt(PRIVATE_INFO_QUANTITY);
			privateComment = cursor.getString(PRIVATE_INFO_COMMENT);
			acquiredFrom = cursor.getString(PRIVATE_INFO_ACQUIRED_FROM);
			// TODO format this; is currently YYYY-MM-DD
			acquisitionDate = cursor.getString(PRIVATE_INFO_ACQUISITION_DATE);
			thumbnailUrl = cursor.getString(COLLECTION_THUMBNAIL_URL);
			// imageUrl = cursor.getString(COLLECTION_IMAGE_URL);
			year = cursor.getInt(COLLECTION_YEAR_PUBLISHED);
			wishlistPriority = cursor.getInt(STATUS_WISHLIST_PRIORITY);
			wishlistComment = cursor.getString(WISHLIST_COMMENT);
			condition = cursor.getString(CONDITION);
			wantParts = cursor.getString(WANTPARTS_LIST);
			hasParts = cursor.getString(HASPARTS_LIST);

			mStatus = new ArrayList<String>();
			for (int i = STATUS_OWN; i <= STATUS_PREORDERED; i++) {
				if (cursor.getInt(i) == 1) {
					if (i == STATUS_WISHLIST) {
						mStatus.add(getWishlistPriority());
					} else {
						mStatus.add(r.getStringArray(R.array.collection_status_filter_entries)[i - STATUS_OWN]);
					}
				}
			}
		}

		String getStatus() {
			String status = StringUtils.formatList(mStatus);
			if (TextUtils.isEmpty(status)) {
				return r.getString(R.string.invalid_collection_status);
			}
			return status;
		}

		CharSequence getLastModifiedDescription() {
			String s = r.getString(R.string.collection_modified) + ": ";
			if (TextUtils.isEmpty(lastModifiedDateTime)) {
				return s + DateUtils.getRelativeTimeSpanString(lastModified);
			}
			return s + lastModifiedDateTime;
		}

		CharSequence getUpdatedDescription() {
			return r.getString(R.string.updated) + ": " + DateUtils.getRelativeTimeSpanString(updated);
		}

		boolean hasRating() {
			return rating != 0.0;
		}

		String getRatingDescription() {
			return new DecimalFormat("#0.00").format(rating);
		}

		String getYearDescription() {
			if (year == 0) {
				return "?";
			}
			return String.valueOf(year);
		}

		String getWishlistPriority() {
			int i = wishlistPriority;
			if (wishlistPriority < 0 || wishlistPriority > 5) {
				i = 0;
			}
			return r.getStringArray(R.array.wishlist_priority)[i];
		}

		String getPrice() {
			return formatCurrency(priceCurrency) + currencyFormat.format(price);
		}

		String getValue() {
			return formatCurrency(currentValueCurrency) + currencyFormat.format(currentValue);
		}

		boolean hasPrivateInfo() {
			return hasPrice() || hasValue() || hasQuantity() || hasAcquisition() || hasPrivateComment();
		}

		boolean hasPrice() {
			return price > 0.0;
		}

		boolean hasValue() {
			return currentValue > 0.0;
		}

		boolean hasQuantity() {
			return quantity > 0;
		}

		boolean hasAcquisition() {
			return getAcquistionDescription() != null;
		}

		boolean hasPrivateComment() {
			return !TextUtils.isEmpty(privateComment);
		}

		String getAcquistionDescription() {
			int resource;
			if (TextUtils.isEmpty(acquiredFrom)) {
				if (TextUtils.isEmpty(acquisitionDate)) {
					return null;
				} else {
					resource = R.string.acquired_on;
				}
			} else {
				resource = TextUtils.isEmpty(acquisitionDate) ? R.string.acquired_from : R.string.acquired_on_from;
			}
			return String.format(r.getString(resource), acquisitionDate, acquiredFrom);
		}

		private String formatCurrency(String currency) {
			if ("USD".equals(currency) || "CAD".equals(currency) || "AUD".equals(currency)) {
				return "$";
			} else if ("EUR".equals(currency)) {
				return "\u20AC";
			} else if ("GBP".equals(currency)) {
				return "\u00A3";
			} else if ("YEN".equals(currency)) {
				return "\u00A5";
			}
			return "";
		}
	}
}
