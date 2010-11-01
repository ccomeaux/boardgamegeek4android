package com.boardgamegeek.view;

import com.boardgamegeek.R;
import com.boardgamegeek.model.BoardGame;
import com.boardgamegeek.ui.BoardgameActivity;

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
		BoardGame boardGame = BoardgameActivity.boardGame;
		if (boardGame == null) {
			return;
		}

		setText(R.id.bggGameLink, "http://boardgame.geekdo.com/boardgame/" + boardGame.getGameId());
		setText(R.id.bgPricesLink, "http://boardgameprices.com/iphone/?s=" + boardGame.getNameForUrl());
		setText(R.id.amazonLink, "http://www.amazon.com/gp/aw/s.html/?m=aps&k=" + boardGame.getNameForUrl()
			+ "&i=toys-and-games&submitSearch=GO");
		setText(R.id.ebayLink, "http://m.ebay.com/Pages/SearchResults.aspx?cid=233&rf=0&sv="
			+ boardGame.getNameForUrl() + "&emvcc=0");
	}

	private void setText(int textViewId, String text) {
		TextView textView = (TextView) findViewById(textViewId);
		textView.setText(text);
	}
}
