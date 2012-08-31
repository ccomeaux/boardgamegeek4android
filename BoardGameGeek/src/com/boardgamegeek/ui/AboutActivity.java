package com.boardgamegeek.ui;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;

public class AboutActivity extends SherlockActivity {
	private WebView browser;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		browser = (WebView) findViewById(R.id.webkit);
		browser.loadUrl("file:///android_asset/About.html");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(this);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
