package com.boardgamegeek.ui;

import com.boardgamegeek.R;
import com.boardgamegeek.util.UIUtils;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

public class AboutActivity extends Activity {
	private WebView browser;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.activity_about);
		
		UIUtils.setTitle(this);
		
		browser = (WebView) findViewById(R.id.webkit);
		browser.loadUrl("file:///android_asset/About.html");
	}
	
	public void onHomeClick(View v) {
		UIUtils.resetToHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}
}
