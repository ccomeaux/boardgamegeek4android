package com.boardgamegeek.pref

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.MenuItem
import androidx.collection.arrayMapOf
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.DrawerActivity
import com.boardgamegeek.ui.viewmodel.SyncViewModel
import com.boardgamegeek.work.SyncWorker
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : DrawerActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this) {
            if (!supportFragmentManager.popBackStackImmediate()) {
               finish()
            }
        }

        if (savedInstanceState == null) {
            val prefFragment = PrefFragment()
            val args = Bundle()
            when (intent.action) {
                getString(R.string.intent_action_account) -> args.putString(KEY_SETTINGS_FRAGMENT, ACTION_ACCOUNT)
                getString(R.string.intent_action_sync) -> args.putString(KEY_SETTINGS_FRAGMENT, ACTION_SYNC)
                getString(R.string.intent_action_data) -> args.putString(KEY_SETTINGS_FRAGMENT, ACTION_DATA)
            }
            prefFragment.arguments = args
            supportFragmentManager.beginTransaction().add(R.id.root_container, prefFragment, TAG_SINGLE_PANE).commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override val navigationItemId = R.id.settings

    internal fun replaceFragment(key: String) {
        val fragment = PrefFragment().apply {
            arguments = bundleOf(KEY_SETTINGS_FRAGMENT to key)
        }
        supportFragmentManager.beginTransaction().replace(R.id.root_container, fragment).addToBackStack(null).commitAllowingStateLoss()
    }

    @AndroidEntryPoint
    class PrefFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
        private var entryValues = emptyArray<String>()
        private var entries = emptyArray<String>()
        private var syncType = SyncService.FLAG_SYNC_NONE
        private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(requireContext()) }
        private val syncViewModel by activityViewModels<SyncViewModel>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            syncViewModel.username.observe(this) {
                if (it.isNullOrBlank()) {
                    toast(R.string.msg_sign_out_success)
                    updateAccountPrefs("")
                } else {
                    updateAccountPrefs(it)
                }
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val fragmentKey = arguments?.getString(KEY_SETTINGS_FRAGMENT)
            if (fragmentKey.isNullOrBlank()) {
                addPreferencesFromResource(R.xml.preference_headers)
            } else {
                val fragmentId = FRAGMENT_MAP[fragmentKey]
                if (fragmentId != null) {
                    addPreferencesFromResource(fragmentId)
                }
            }

            when (fragmentKey) {
                ACTION_SYNC -> {
                    entryValues = resources.getStringArray(R.array.pref_sync_status_values)
                    entries = resources.getStringArray(R.array.pref_sync_status_entries)

                    updateSyncStatusSummary(PREFERENCES_KEY_SYNC_STATUSES)
                }
                ACTION_ABOUT -> {
                    findPreference<Preference>("open_source_licenses")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        LibsBuilder()
                            .withFields(R.string::class.java.fields)
                            .withAutoDetect(true)
                            .withLicenseShown(true)
                            .withActivityTitle(getString(R.string.pref_about_licenses))
                            .withActivityTheme(R.style.Theme_bgglight_About)
                            .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                            .withAboutIconShown(true)
                            .withAboutAppName(getString(R.string.app_name))
                            .withAboutVersionShownName(true)
                            .start(requireContext())
                        true
                    }
                }
            }
        }

        override fun onResume() {
            super.onResume()
            preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onStop() {
            super.onStop()
            if (syncType != SyncService.FLAG_SYNC_NONE) {
                SyncService.sync(activity, syncType)
                syncType = SyncService.FLAG_SYNC_NONE
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            when (key) {
                PREFERENCES_KEY_SYNC_STATUSES -> {
                    updateSyncStatusSummary(key)
                    syncPrefs.requestPartialSync()
                    syncType = syncType or SyncService.FLAG_SYNC_COLLECTION
                }
                PREFERENCES_KEY_SYNC_PLAYS -> {
                    syncPrefs.clearPlaysTimestamps()
                    SyncWorker.requestPlaySync(requireContext())
                }
                PREFERENCES_KEY_SYNC_BUDDIES -> {
                    syncPrefs.clearBuddyListTimestamps()
                    SyncWorker.requestBuddySync(requireContext())
                }
            }
        }

        private fun updateSyncStatusSummary(key: String) {
            val pref = findPreference<Preference>(key) ?: return
            val statuses = requireContext().preferences().getSyncStatusesOrDefault()
            pref.summary = if (statuses.isEmpty()) {
                getString(R.string.pref_list_empty)
            } else {
                entryValues.indices
                    .filter { statuses.contains(entryValues[it]) }
                    .joinToString { entries[it] }
            }
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            return preference?.key?.let {
                when {
                    it.startsWith(ACTION_PREFIX) -> {
                        (activity as SettingsActivity).replaceFragment(it)
                        true
                    }
                    else -> super.onPreferenceTreeClick(preference)
                }
            } ?: super.onPreferenceTreeClick(preference)
        }

        private val dialogFragmentTag = "PreferenceFragment.DIALOG"

        override fun onDisplayPreferenceDialog(preference: Preference?) {
            if (parentFragmentManager.findFragmentByTag(dialogFragmentTag) != null) {
                return
            }

            val dialogFragment: DialogFragment? = when (preference) {
                is SignOutPreference -> SignOutDialogFragment.newInstance(preference.key)
                is ConfirmDialogPreference -> ConfirmDialogFragment.newInstance(preference.key)
                is SyncTimestampsDialogPreference -> SyncTimestampsDialogFragment.newInstance(preference.key)
                else -> null
            }

            if (dialogFragment != null) {
                dialogFragment.setTargetFragment(this, 0)
                dialogFragment.show(parentFragmentManager, dialogFragmentTag)
            } else super.onDisplayPreferenceDialog(preference)
        }

        private fun updateAccountPrefs(username: String) {
            findPreference<LoginPreference>(KEY_LOGIN)?.update(username)
            findPreference<SignOutPreference>(KEY_LOGOUT)?.update()
        }
    }

    companion object {
        private const val TAG_SINGLE_PANE = "single_pane"
        private const val KEY_SETTINGS_FRAGMENT = "SETTINGS_FRAGMENT"

        private const val ACTION_PREFIX = "com.boardgamegeek.prefs."
        private const val ACTION_ACCOUNT = ACTION_PREFIX + "ACCOUNT"
        private const val ACTION_SYNC = ACTION_PREFIX + "SYNC"
        private const val ACTION_DATA = ACTION_PREFIX + "DATA"
        private const val ACTION_LOG = ACTION_PREFIX + "LOG"
        private const val ACTION_ADVANCED = ACTION_PREFIX + "ADVANCED"
        private const val ACTION_ABOUT = ACTION_PREFIX + "ABOUT"
        private const val ACTION_AUTHORS = ACTION_PREFIX + "AUTHORS"

        private val FRAGMENT_MAP = arrayMapOf(
            ACTION_ACCOUNT to R.xml.preference_account,
            ACTION_SYNC to R.xml.preference_sync,
            ACTION_DATA to R.xml.preference_data,
            ACTION_LOG to R.xml.preference_log,
            ACTION_ADVANCED to R.xml.preference_advanced,
            ACTION_ABOUT to R.xml.preference_about,
            ACTION_AUTHORS to R.xml.preference_authors
        )
    }
}
