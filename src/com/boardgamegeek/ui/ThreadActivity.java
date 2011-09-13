package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteThreadHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.ThreadArticle;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.UIUtils;

public class ThreadActivity extends ListActivity {
	private final String TAG = "ThreadActivity";

	public static final String KEY_THREAD_ID = "THREAD_ID";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	public static final String KEY_THREAD_SUBJECT = "THREAD_SUBJECT";
	
	private ThreadAdapter mAdapter;
	private List<ThreadArticle> mArticles = new ArrayList<ThreadArticle>();
	
	private String mThreadId;
//	private String mThumbnailUrl;
	private String mThreadSubject;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_thread);
		
		if (savedInstanceState == null) {
			final Intent intent = getIntent();
			mThreadId = intent.getExtras().getString(KEY_THREAD_ID);
//			mThumbnailUrl = intent.getExtras().getString(KEY_THUMBNAIL_URL);
			mThreadSubject = intent.getExtras().getString(KEY_THREAD_SUBJECT);
		}
		else {
			mThreadId = savedInstanceState.getString(KEY_THREAD_ID);
//			mThumbnailUrl = savedInstanceState.getString(KEY_THUMBNAIL_URL);
			mThreadSubject = savedInstanceState.getString(KEY_THREAD_SUBJECT);
		}
		
		((TextView)findViewById(R.id.thread_subject)).setText(mThreadSubject);
		UIUtils.setTitle(this);
		
		ThreadTask task = new ThreadTask();
		task.execute();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_THREAD_ID, mThreadId);
//		outState.putString(KEY_THUMBNAIL_URL, mThumbnailUrl);
		outState.putString(KEY_THREAD_SUBJECT, mThreadSubject);
	}

	public void onHomeClick(View v) {
		UIUtils.goHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}
	
	private class ThreadTask extends AsyncTask<Void, Void, RemoteThreadHandler> {
		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;
		private RemoteThreadHandler mHandler = new RemoteThreadHandler();

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(ThreadActivity.this, true);
			mExecutor = new RemoteExecutor(mHttpClient, null);
		}
		
		@Override
		protected RemoteThreadHandler doInBackground(Void... params) {
			final String url = HttpUtils.constructThreadUrl(mThreadId);
			Log.i(TAG, "Loading thread content from " + url);
			try {
				mExecutor.executeGet(url, mHandler);
			} catch (HandlerException e) {
				Log.e(TAG, e.toString());
			}
			return mHandler;
		}
		
		@Override
		protected void onPostExecute(RemoteThreadHandler result) {
			Log.i(TAG, "Acticles count " + result.getCount());
			final int count = result.getCount();
			if (result.isBggDown()) {
				UIUtils.showListMessage(ThreadActivity.this, R.string.bgg_down);
			} else if (count == 0) {
				String message = String.format(getResources().getString(R.string.thread_no_results), mThreadSubject);
				UIUtils.showListMessage(ThreadActivity.this, message);
			} else {
				mArticles.addAll(result.getResults());
				mAdapter = new ThreadAdapter();
				setListAdapter(mAdapter);
			}
		}
	}
	
	private class ThreadAdapter extends ArrayAdapter<ThreadArticle> {
		private LayoutInflater mInflater;
		
		public ThreadAdapter() {
			super(ThreadActivity.this, R.layout.row_threadarticle, mArticles);
			mInflater = getLayoutInflater();
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_threadarticle, parent, false);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			}
			else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			ThreadArticle article;
			try {
				article = mArticles.get(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (article != null) {
				holder.username.setText(article.username + ":");
				holder.body.loadData(article.body, "text/html", "UTF-8");
			}
			return convertView;
		}
	}
	
	static class ViewHolder {
		TextView username;
		View divider;
//		TextView subject;
		WebView body;

		public ViewHolder(View view) {
			username = (TextView) view.findViewById(R.id.article_username);
			divider = (View) view.findViewById(R.id.article_divider);
//			subject = (TextView) view.findViewById(R.id.thread_lastpostdate);
			body = (WebView) view.findViewById(R.id.article_webkit);
		}
	}
}
