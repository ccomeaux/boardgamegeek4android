package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ListView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Forum;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.UIUtils;

public class GeneralForumlistActivity extends ListActivity {
	private static final String GENERAL_FORUMLIST_LINK = "http://boardgamegeek.com/xmlapi2/forumlist?id=1&type=region";

	private final String TAG = "GeneralForumlistActivity";
	
	public static final String KEY_FORUMS = "FORUMS";
	
//	private ForumlistActivity.ForumlistAdapter mAdapter;
	private List<Forum> mForums = new ArrayList<Forum>();
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_forumlist);
		UIUtils.setTitle(this);
		findViewById(R.id.game_thumbnail).setClickable(false);
		findViewById(R.id.forumlist_game_header).setVisibility(View.GONE);
		findViewById(R.id.forumlist_header_divider).setVisibility(View.GONE);
		
		if (mForums == null || mForums.size() == 0) {
			ForumsUtils.ForumlistTask task =
				new ForumsUtils.ForumlistTask(this, mForums, GENERAL_FORUMLIST_LINK, this.getTitle().toString(), TAG);
			task.execute();
		} else {
			setListAdapter(new ForumsUtils.ForumlistAdapter(this, mForums));
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(KEY_FORUMS, (ArrayList<? extends Parcelable>) mForums);
	}

	public void onHomeClick(View v) {
		UIUtils.goHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	@Override
	protected void onListItemClick(ListView listView, View convertView, int position, long id) {
		ForumsUtils.ForumlistViewHolder holder = (ForumsUtils.ForumlistViewHolder) convertView.getTag();
		if (holder != null) {
			Intent forumsIntent = new Intent(this, ForumActivity.class);
			forumsIntent.putExtra(ForumActivity.KEY_FORUM_ID, holder.forumId);
			forumsIntent.putExtra(ForumActivity.KEY_GAME_NAME, "");
			forumsIntent.putExtra(ForumActivity.KEY_THUMBNAIL_URL, "");
			forumsIntent.putExtra(ForumActivity.KEY_FORUM_TITLE, holder.forumTitle.getText());
			forumsIntent.putExtra(ForumActivity.KEY_NUM_THREADS, holder.numThreads.getText());
			this.startActivity(forumsIntent);
		}
	}
}
