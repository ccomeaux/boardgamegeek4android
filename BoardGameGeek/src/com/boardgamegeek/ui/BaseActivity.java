package com.boardgamegeek.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.pref.Preferences;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HelpUtils;

public abstract class BaseActivity extends SherlockFragmentActivity {

	protected int getOptionsMenuId() {
		return 0;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater menuInflater = getSupportMenuInflater();
		menuInflater.inflate(R.menu.base, menu);
		if (getOptionsMenuId() != 0) {
			menuInflater.inflate(getOptionsMenuId(), menu);
			setupSearchMenuItem(menu);
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		AccountManager am = AccountManager.get(this);
		Account account = Authenticator.getAccount(am);
		menu.findItem(R.id.menu_sign_out).setVisible(account != null && !TextUtils.isEmpty(am.getPassword(account)));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (this instanceof TopLevelActivity) {
					// bug in ActionBarDrawerToggle
					return false;
				}
				NavUtils.navigateUpFromSameTask(this);
				return true;
			case R.id.menu_contact_us:
				Intent emailIntent = new Intent(Intent.ACTION_SEND);
				emailIntent.setType("text/email");
				emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "bgg4android@gmail.com" });
				emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
				startActivity(emailIntent);
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(this, Preferences.class));
				return true;
			case R.id.menu_about:
				HelpUtils.showAboutDialog(this);
				return true;
			case R.id.menu_sign_out:
				ActivityUtils.createConfirmationDialog(this, R.string.are_you_sure_sign_out,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Authenticator.signOut(BaseActivity.this);
						}
					}).show();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setupSearchMenuItem(Menu menu) {
		MenuItem searchItem = menu.findItem(R.id.menu_search);
		if (searchItem != null) {
			SearchView searchView = (SearchView) searchItem.getActionView();
			if (searchView != null) {
				SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
				searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			}
		}
	}
}