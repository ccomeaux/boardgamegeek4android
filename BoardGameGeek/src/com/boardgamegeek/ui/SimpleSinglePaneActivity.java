package com.boardgamegeek.ui;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.widget.SearchView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.util.VersionUtils;

public abstract class SimpleSinglePaneActivity extends SherlockFragmentActivity {
	private Fragment mFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_singlepane_empty);

		if (savedInstanceState == null) {
			mFragment = onCreatePane();
			mFragment.setArguments(intentToFragmentArguments(getIntent()));
			getSupportFragmentManager().beginTransaction().add(R.id.root_container, mFragment, "single_pane").commit();
		} else {
			mFragment = getSupportFragmentManager().findFragmentByTag("single_pane");
		}
	}

	/**
	 * Called in <code>onCreate</code> when the fragment constituting this activity is needed. The returned fragment's
	 * arguments will be set to the intent used to invoke this activity.
	 */
	protected abstract Fragment onCreatePane();

	public Fragment getFragment() {
		return mFragment;
	}

	protected abstract int getOptionsMenuId();

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (getOptionsMenuId() != 0) {
			getSupportMenuInflater().inflate(getOptionsMenuId(), menu);
			setupSearchMenuItem(menu);
		}
		return true;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupSearchMenuItem(Menu menu) {
		MenuItem searchItem = menu.findItem(R.id.menu_search);
		if (searchItem != null && VersionUtils.hasHoneycomb()) {
			SearchView searchView = (SearchView) searchItem.getActionView();
			if (searchView != null) {
				SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
				searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(this);
				return true;
			case R.id.menu_search:
				if (!VersionUtils.hasHoneycomb()) {
					onSearchRequested();
					return true;
				}
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Converts an intent into a {@link Bundle} suitable for use as fragment arguments.
	 */
	public static Bundle intentToFragmentArguments(Intent intent) {
		Bundle arguments = new Bundle();
		if (intent == null) {
			return arguments;
		}

		final Uri data = intent.getData();
		if (data != null) {
			arguments.putParcelable("_uri", data);
		}

		final Bundle extras = intent.getExtras();
		if (extras != null) {
			arguments.putAll(intent.getExtras());
		}

		return arguments;
	}

	/**
	 * Converts a fragment arguments bundle into an intent.
	 */
	public static Intent fragmentArgumentsToIntent(Bundle arguments) {
		Intent intent = new Intent();
		if (arguments == null) {
			return intent;
		}

		final Uri data = arguments.getParcelable("_uri");
		if (data != null) {
			intent.setData(data);
		}

		intent.putExtras(arguments);
		intent.removeExtra("_uri");
		return intent;
	}
}
