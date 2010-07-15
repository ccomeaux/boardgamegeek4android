package com.boardgamegeek.view;

import com.boardgamegeek.R;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;


public class AboutView extends Activity {
	private WebView browser;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.about);
		browser = (WebView) findViewById(R.id.webkit);
		browser.loadUrl("file:///android_asset/About.html");
	}
}
