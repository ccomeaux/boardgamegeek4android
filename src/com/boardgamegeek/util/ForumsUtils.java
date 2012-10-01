package com.boardgamegeek.util;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;

import java.util.List;

import org.apache.http.client.HttpClient;

import android.app.Activity;
import android.app.ListActivity;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteForumHandler;
import com.boardgamegeek.io.RemoteThreadHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.ForumThread;
import com.boardgamegeek.model.ThreadArticle;
import com.boardgamegeek.ui.ForumActivity;

public class ForumsUtils {

	public static class ForumTask extends AsyncTask<Void, Void, RemoteForumHandler> {
		private ForumActivity mActivity;
		private String mTag;

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;
		private RemoteForumHandler mHandler = new RemoteForumHandler();

		public ForumTask(ForumActivity activity, String tag) {
			this.mActivity = activity;
			this.mTag = tag;
		}

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(mActivity, true);
			mExecutor = new RemoteExecutor(mHttpClient, null);
		}

		@Override
		protected RemoteForumHandler doInBackground(Void... params) {
			final String url = HttpUtils.constructForumUrl(mActivity.getForumId(), mActivity.getCurrentPage());  
			LOGI(mTag, "Loading forum content from " + url);
			try {
				mExecutor.executeGet(url, mHandler);
			} catch (HandlerException e) {
				LOGE(mTag, e.toString());
			}
			return mHandler;
		}

		@Override
		protected void onPostExecute(RemoteForumHandler result) {
			LOGI(mTag, "Threads count " + result.getTotalCount());
			mActivity.setThreadCount(result.getTotalCount());
			if (result.isBggDown()) {
				UIUtils.showListMessage(mActivity, R.string.bgg_down);
			} else if (mActivity.getThreadCount() == 0) {
				String message = String.format(mActivity.getResources().getString(R.string.empty_forum),
						mActivity.getTitle().toString());
				UIUtils.showListMessage(mActivity, message);
			} else {
				if (mActivity.getPageCount() == -1) {
					mActivity.setmPageCount((mActivity.getThreadCount() - 1) / ForumActivity.PAGE_SIZE + 1);
				}
				mActivity.addMoreForums(result.getResults());
				mActivity.updateDisplay();
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
			LOGI(mTag, "Loading thread content from " + mUrl);
			try {
				mExecutor.executeGet(mUrl, mHandler);
			} catch (HandlerException e) {
				LOGE(mTag, e.toString());
			}
			return mHandler;
		}

		@Override
		protected void onPostExecute(RemoteThreadHandler result) {
			LOGI(mTag, "Acticles count " + result.getCount());
			final int count = result.getCount();
			if (result.isBggDown()) {
				UIUtils.showListMessage(mActivity, R.string.bgg_down);
			} else if (count == 0) {
				String message = String.format(mActivity.getResources().getString(R.string.empty_thread),
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
				holder.body.loadDataWithBaseURL(null, article.body, "text/html", "UTF-8", null);
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
