package com.boardgamegeek.util;

import java.util.List;

import org.apache.http.client.HttpClient;

import android.app.Activity;
import android.app.ListActivity;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteForumHandler;
import com.boardgamegeek.io.RemoteForumlistHandler;
import com.boardgamegeek.io.RemoteThreadHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.Forum;
import com.boardgamegeek.model.ForumThread;
import com.boardgamegeek.model.ThreadArticle;

public class ForumsUtils {

	public static class ForumlistTask extends AsyncTask<Void, Void, RemoteForumlistHandler> {
		private Activity mActivity;
		private List<Forum> mForums;
		private String mUrl;
		private String mGameName;
		private String mTag;

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;
		private RemoteForumlistHandler mHandler = new RemoteForumlistHandler();

		public ForumlistTask(Activity activity, List<Forum> forums, String url, String gameName, String tag) {
			this.mActivity = activity;
			this.mForums = forums;
			this.mUrl = url;
			this.mGameName = gameName;
			this.mTag = tag;
		}

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(mActivity, true);
			mExecutor = new RemoteExecutor(mHttpClient, null);
		}

		@Override
		protected RemoteForumlistHandler doInBackground(Void... params) {
			Log.i(mTag, "Loading forums from " + mUrl);
			try {
				mExecutor.executeGet(mUrl, mHandler);
			} catch (HandlerException e) {
				Log.e(mTag, e.toString());
			}
			return mHandler;
		}

		@Override
		protected void onPostExecute(RemoteForumlistHandler result) {
			Log.i(mTag, "Forums count " + result.getCount());
			final int count = result.getCount();
			if (result.isBggDown()) {
				UIUtils.showListMessage(mActivity, R.string.bgg_down);
			} else if (count == 0) {
				// TODO: show a different message if mGameName is empty
				final String message = String.format(mActivity.getResources().getString(R.string.forumlist_no_results),
						mGameName);
				UIUtils.showListMessage(mActivity, message);
			} else {
				mForums.addAll(result.getResults());
				((ListActivity) mActivity).setListAdapter(new ForumlistAdapter(mActivity, mForums));
			}
		}
	}

	public static class ForumlistAdapter extends ArrayAdapter<Forum> {
		private Activity mActivity;
		private List<Forum> mForums;

		private LayoutInflater mInflater;
		private Resources mResources;
		private String mLastPostText;

		public ForumlistAdapter(Activity activity, List<Forum> forums) {
			super(activity, R.layout.row_forum, forums);
			this.mActivity = activity;
			this.mForums = forums;
			mInflater = this.mActivity.getLayoutInflater();
			mResources = this.mActivity.getResources();
			mLastPostText = mResources.getString(R.string.forum_last_post);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ForumlistViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_forum, parent, false);
				holder = new ForumlistViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ForumlistViewHolder) convertView.getTag();
			}

			Forum forumlist;
			try {
				forumlist = mForums.get(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (forumlist != null) {
				holder.forumId = forumlist.id;
				holder.forumTitle.setText(forumlist.title);
				holder.numThreads.setText(mResources.getQuantityString(R.plurals.forum_threads, forumlist.numthreads,
						forumlist.numthreads));
				if (forumlist.lastpostdate > 0) {
					holder.lastPost.setText(String.format(mLastPostText, DateUtils.getRelativeTimeSpanString(
							forumlist.lastpostdate, System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS, 0)));
					holder.lastPost.setVisibility(View.VISIBLE);
				} else {
					holder.lastPost.setVisibility(View.GONE);
				}
			}
			return convertView;
		}
	}

	public static class ForumlistViewHolder {
		public String forumId;
		public TextView forumTitle;
		public TextView numThreads;
		public TextView lastPost;

		public ForumlistViewHolder(View view) {
			forumTitle = (TextView) view.findViewById(R.id.forum_title);
			numThreads = (TextView) view.findViewById(R.id.numthreads);
			lastPost = (TextView) view.findViewById(R.id.lastpost);
		}
	}

	public static class ForumTask extends AsyncTask<Void, Void, RemoteForumHandler> {
		private Activity mActivity;
		private List<ForumThread> mForumThreads;
		private String mUrl;
		private String mForumName;
		private String mTag;

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;
		private RemoteForumHandler mHandler = new RemoteForumHandler();

		public ForumTask(Activity activity, List<ForumThread> forumThreads, String url, String forumName, String tag) {
			this.mActivity = activity;
			this.mForumThreads = forumThreads;
			this.mUrl = url;
			this.mForumName = forumName;
			this.mTag = tag;
		}

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(mActivity, true);
			mExecutor = new RemoteExecutor(mHttpClient, null);
		}

		@Override
		protected RemoteForumHandler doInBackground(Void... params) {
			Log.i(mTag, "Loading forum content from " + mUrl);
			try {
				mExecutor.executeGet(mUrl, mHandler);
			} catch (HandlerException e) {
				Log.e(mTag, e.toString());
			}
			return mHandler;
		}

		@Override
		protected void onPostExecute(RemoteForumHandler result) {
			Log.i(mTag, "Threads count " + result.getCount());
			final int count = result.getCount();
			if (result.isBggDown()) {
				UIUtils.showListMessage(mActivity, R.string.bgg_down);
			} else if (count == 0) {
				String message = String.format(mActivity.getResources().getString(R.string.forum_no_results),
						mForumName);
				UIUtils.showListMessage(mActivity, message);
			} else {
				mForumThreads.addAll(result.getResults());
				((ListActivity) mActivity).setListAdapter(new ForumAdapter(mActivity, mForumThreads));
			}
		}
	}

	public static class ForumAdapter extends ArrayAdapter<ForumThread> {
		private Activity mActivity;
		private List<ForumThread> mForumThreads;

		private LayoutInflater mInflater;
		private Resources mResources;
		private String mAuthorText;
		private String mLastPostText;
		private String mCreatedText;

		public ForumAdapter(Activity activity, List<ForumThread> forumThreads) {
			super(activity, R.layout.row_forumthread, forumThreads);
			this.mActivity = activity;
			this.mForumThreads = forumThreads;
			mInflater = this.mActivity.getLayoutInflater();
			mResources = this.mActivity.getResources();
			mAuthorText = mResources.getString(R.string.forum_thread_author);
			mLastPostText = mResources.getString(R.string.forum_last_post);
			mCreatedText = mResources.getString(R.string.forum_thread_created);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ForumViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_forumthread, parent, false);
				holder = new ForumViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ForumViewHolder) convertView.getTag();
			}

			ForumThread thread;
			try {
				thread = mForumThreads.get(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (thread != null) {
				holder.threadId = thread.id;
				holder.subject.setText(thread.subject);
				holder.author.setText(String.format(mAuthorText, thread.author));
				int replies = thread.numarticles - 1;
				holder.numarticles.setText(mResources.getQuantityString(R.plurals.forum_thread_replies, replies,
						replies));
				holder.lastpostdate.setText(String.format(mLastPostText, DateUtils.getRelativeTimeSpanString(
						thread.lastpostdate, System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS, 0)));
				holder.postdate.setText(String.format(mCreatedText, DateUtils.getRelativeTimeSpanString(
						thread.postdate, System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS, 0)));
			}
			return convertView;
		}
	}

	public static class ForumViewHolder {
		public String threadId;
		public TextView subject;
		public TextView author;
		public TextView numarticles;
		public TextView lastpostdate;
		public TextView postdate;

		public ForumViewHolder(View view) {
			subject = (TextView) view.findViewById(R.id.thread_title);
			author = (TextView) view.findViewById(R.id.thread_author);
			numarticles = (TextView) view.findViewById(R.id.thread_numarticles);
			lastpostdate = (TextView) view.findViewById(R.id.thread_lastpostdate);
			postdate = (TextView) view.findViewById(R.id.thread_postdate);
		}
	}

	public static class ThreadTask extends AsyncTask<Void, Void, RemoteThreadHandler> {
		private Activity mActivity;
		private List<ThreadArticle> mArticles;
		private String mUrl;
		private String mThreadSubject;
		private String mTag;

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;
		private RemoteThreadHandler mHandler = new RemoteThreadHandler();

		public ThreadTask(Activity activity, List<ThreadArticle> articles, String url, String threadSubject, String tag) {
			this.mActivity = activity;
			this.mArticles = articles;
			this.mUrl = url;
			this.mThreadSubject = threadSubject;
			this.mTag = tag;
		}

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(mActivity, true);
			mExecutor = new RemoteExecutor(mHttpClient, null);
		}

		@Override
		protected RemoteThreadHandler doInBackground(Void... params) {
			Log.i(mTag, "Loading thread content from " + mUrl);
			try {
				mExecutor.executeGet(mUrl, mHandler);
			} catch (HandlerException e) {
				Log.e(mTag, e.toString());
			}
			return mHandler;
		}

		@Override
		protected void onPostExecute(RemoteThreadHandler result) {
			Log.i(mTag, "Acticles count " + result.getCount());
			final int count = result.getCount();
			if (result.isBggDown()) {
				UIUtils.showListMessage(mActivity, R.string.bgg_down);
			} else if (count == 0) {
				String message = String.format(mActivity.getResources().getString(R.string.thread_no_results),
						mThreadSubject);
				UIUtils.showListMessage(mActivity, message);
			} else {
				mArticles.addAll(result.getResults());
				((ListActivity) mActivity).setListAdapter(new ThreadAdapter(mActivity, mArticles));
			}
		}
	}

	public static class ThreadAdapter extends ArrayAdapter<ThreadArticle> {
		private Activity mActivity;
		private List<ThreadArticle> mArticles;

		private LayoutInflater mInflater;

		public ThreadAdapter(Activity activity, List<ThreadArticle> articles) {
			super(activity, R.layout.row_threadarticle, articles);
			this.mActivity = activity;
			this.mArticles = articles;
			mInflater = mActivity.getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ThreadViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_threadarticle, parent, false);
				holder = new ThreadViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ThreadViewHolder) convertView.getTag();
			}

			ThreadArticle article;
			try {
				article = mArticles.get(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (article != null) {
				holder.username.setText(article.username + ":");
				holder.editdate.setText(DateUtils.getRelativeTimeSpanString(article.editdate,
						System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS, 0));
				holder.body.loadData(article.body, "text/html", "UTF-8");
			}
			return convertView;
		}
	}

	public static class ThreadViewHolder {
		TextView username;
		TextView editdate;
		WebView body;

		public ThreadViewHolder(View view) {
			username = (TextView) view.findViewById(R.id.article_username);
			editdate = (TextView) view.findViewById(R.id.article_editdate);
			body = (WebView) view.findViewById(R.id.article_webkit);
		}
	}
}
