package com.boardgamegeek.ui;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteCommentsHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.Comment;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class CommentsActivity extends ListActivity {
	private final String TAG = "CommentsActivity";

	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	private static final String KEY_CURRENT_PAGE = "CURRENT_PAGE";
	private static final String KEY_LAST_DISPLAYED_PAGE = "LAST_DISPLAYED_PAGE";
	private static final String KEY_PAGE_COUNT = "PAGE_COUNT";
	private static final String KEY_COMMENT_COUNT = "COMMENT_COUNT";
	private static final String KEY_COMMENTS = "COMMENTS";
	private static final int PAGE_SIZE = 100;

	private static final int backgroundColors[] = {
		Color.WHITE,
		0xffff0000,
		0xffff3366,
		0xffff6699,
		0xffff66cc,
		0xffcc99ff,
		0xff9999ff,
		0xff99ffff,
		0xff66ff99,
		0xff33cc99,
		0xff00cc00 };

	private CommentsAdapter mAdapter;
	private TextView mInfoView;

	private List<Comment> mCurrentComments = new ArrayList<Comment>();
	private List<Comment> mAllComments = new ArrayList<Comment>();

	private int mGameId;
	private String mGameName;
	private String mThumbnailUrl;

	private int mCurrentPage;
	/** Used to not download already downloaded comments. */
	private int mLastDisplayedPage;
	private int mPageCount;
	private int mCommentCount;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_comments);
		findViewById(R.id.game_thumbnail).setClickable(false);

		if (savedInstanceState == null) {
			final Intent intent = getIntent();
			mGameId = intent.getExtras().getInt(KEY_GAME_ID);
			if (mGameId < 1) {
				Log.w(TAG, "Didn't get a game ID");
				finish();
			}
			mGameName = intent.getExtras().getString(KEY_GAME_NAME);
			mThumbnailUrl = intent.getExtras().getString(KEY_THUMBNAIL_URL);
			mCurrentPage = 1;
			mLastDisplayedPage = 1;
			mPageCount = -1;
			mCommentCount = 0;
		} else {
			mGameId = savedInstanceState.getInt(KEY_GAME_ID);
			mGameName = savedInstanceState.getString(KEY_GAME_NAME);
			mThumbnailUrl = savedInstanceState.getString(KEY_THUMBNAIL_URL);
			mCurrentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE);
			mLastDisplayedPage = savedInstanceState.getInt(KEY_LAST_DISPLAYED_PAGE);
			mPageCount = savedInstanceState.getInt(KEY_PAGE_COUNT);
			mCommentCount = savedInstanceState.getInt(KEY_COMMENT_COUNT);
			mAllComments = savedInstanceState.getParcelableArrayList(KEY_COMMENTS);
		}

		mInfoView = (TextView) findViewById(R.id.comment_info);

		UIUtils.setTitle(this);
		UIUtils u = new UIUtils(this);
		u.setGameName(mGameName);
		u.setThumbnail(mThumbnailUrl);

		mAdapter = new CommentsAdapter();
		setListAdapter(mAdapter);

		if (mAllComments == null || mAllComments.size() == 0) {
			CommentsTask task = new CommentsTask();
			task.execute();
		} else {
			updateDisplay();
		}
	}

	public void onHomeClick(View v) {
		UIUtils.goHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_GAME_ID, mGameId);
		outState.putString(KEY_GAME_NAME, mGameName);
		outState.putString(KEY_THUMBNAIL_URL, mThumbnailUrl);
		outState.putInt(KEY_CURRENT_PAGE, mCurrentPage);
		outState.putInt(KEY_LAST_DISPLAYED_PAGE, mLastDisplayedPage);
		outState.putInt(KEY_PAGE_COUNT, mPageCount);
		outState.putInt(KEY_COMMENT_COUNT, mCommentCount);
		outState.putParcelableArrayList(KEY_COMMENTS, (ArrayList<? extends Parcelable>) mAllComments);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		final MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.paging, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem mi = menu.findItem(R.id.menu_back);
		mi.setEnabled(mCurrentPage > 1);

		mi = menu.findItem(R.id.menu_forward);
		mi.setEnabled(mCurrentPage < mPageCount);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mCurrentComments.clear();
		switch (item.getItemId()) {
			case R.id.menu_back:
				mCurrentPage--;
				setInfoText();
				setCurrentCommentsPage();
				return true;
			case R.id.menu_forward:
				mAdapter.notifyDataSetChanged();
				mCurrentPage++;
				setInfoText();
				if (mCurrentPage > mLastDisplayedPage) {
					mLastDisplayedPage++;
					CommentsTask task = new CommentsTask();
					task.execute();
				} else {
					setCurrentCommentsPage();
				}
				return true;
		}
		return false;
	}

	private class CommentsTask extends AsyncTask<Void, Void, RemoteCommentsHandler> {

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;
		private RemoteCommentsHandler mHandler = new RemoteCommentsHandler();

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(CommentsActivity.this, true);
			mExecutor = new RemoteExecutor(mHttpClient, null);
		}

		@Override
		protected RemoteCommentsHandler doInBackground(Void... params) {
			String url = HttpUtils.constructCommentsUrl(mGameId, mCurrentPage);
			Log.i(TAG, "Loading comments from " + url);
			try {
				mExecutor.executeGet(url, mHandler);
			} catch (HandlerException e) {
				Log.e(TAG, e.toString());
			}
			return mHandler;
		}

		@Override
		protected void onPostExecute(RemoteCommentsHandler result) {
			mCommentCount = result.getCount();
			if (result.isBggDown()) {
				UIUtils.showListMessage(CommentsActivity.this, R.string.bgg_down);
			} else if (mCommentCount == 0) {
				String message = String.format(getResources().getString(R.string.comments_no_results), mGameName);
				UIUtils.showListMessage(CommentsActivity.this, message);
			} else {
				if (mPageCount == -1) {
					mPageCount = (mCommentCount - 1) / PAGE_SIZE + 1;
				}
				mAllComments.addAll(result.getResults());
				updateDisplay();
			}
		}
	}

	private void updateDisplay() {
		setInfoText();
		setCurrentCommentsPage();
	}

	private void setInfoText() {
		mInfoView.setVisibility(View.VISIBLE);
		mInfoView.setText(getPageStart() + " - " + getPageEnd() + " of " + mCommentCount + " comments");
	}

	private void setCurrentCommentsPage() {
		for (int i = getPageStart(); i <= getPageEnd(); i++) {
			mCurrentComments.add(mAllComments.get(i - 1));
		}
		if (mAdapter == null) {
			mAdapter = new CommentsAdapter();
			setListAdapter(mAdapter);
		} else {
			mAdapter.notifyDataSetChanged();
			this.setSelection(0);
		}
	}

	private int getPageEnd() {
		return Math.min(mCurrentPage * PAGE_SIZE, mCommentCount);
	}

	private int getPageStart() {
		return (mCurrentPage - 1) * PAGE_SIZE + 1;
	}

	private class CommentsAdapter extends ArrayAdapter<Comment> {
		private LayoutInflater mInflater;

		public CommentsAdapter() {
			super(CommentsActivity.this, R.layout.row_comment, mCurrentComments);
			mInflater = getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_comment, parent, false);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			Comment comment;
			try {
				comment = mCurrentComments.get(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (comment != null) {
				holder.username.setText(comment.Username);
				holder.rating.setText(new DecimalFormat("#0.00").format(StringUtils.parseDouble(comment.Rating, 0.0)));
				final int rating = (int) StringUtils.parseDouble(comment.Rating, 0.0);
				holder.rating.setBackgroundColor(backgroundColors[rating]);
				holder.comment.setText(comment.Value);
			}
			return convertView;
		}
	}

	static class ViewHolder {
		TextView username;
		TextView rating;
		TextView comment;

		public ViewHolder(View view) {
			username = (TextView) view.findViewById(R.id.username);
			rating = (TextView) view.findViewById(R.id.rating);
			comment = (TextView) view.findViewById(R.id.comment);
		}
	}
}
