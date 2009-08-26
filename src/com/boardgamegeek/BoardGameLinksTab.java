package com.boardgamegeek;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class BoardGameLinksTab extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.boardgamelinks);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateUI();
	}

	private void updateUI() {
		BoardGame boardGame = ViewBoardGame.boardGame;
		if (boardGame == null) {
			return;
		}

		setText(R.id.bggGameLink, "http://www.boardgamegeek.com/boardgame/"
				+ boardGame.getGameId());
		setText(R.id.bgPricesLink, "http://boardgameprices.com/iphone/?s="
				+ boardGame.getNameForUrl());
		setText(R.id.amazonLink, "http://www.amazon.com/gp/aw/s.html/?m=aps&k="
				+ boardGame.getNameForUrl() + "&i=toys-and-games&submitSearch=GO");
		setText(R.id.ebayLink, "http://toys.shop.ebay.com/items/?_nkw="
				+ boardGame.getNameForUrl() + "&_sacat=220");
	}

	private void setText(int textViewId, String text) {
		TextView textView = (TextView) findViewById(textViewId);
		textView.setText(text);
	}
}
