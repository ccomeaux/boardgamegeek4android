package com.boardgamegeek.pref

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.MenuItem
import androidx.collection.ArrayMap
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.boardgamegeek.R
import com.boardgamegeek.events.SignInEvent
import com.boardgamegeek.events.SignOutEvent
import com.boardgamegeek.extensions.*
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.DrawerActivity
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import hugo.weaving.DebugLog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import org.jetbrains.anko.toast

class SettingsActivity : DrawerActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (!supportFragmentManager.popBackStackImmediate()) {
            super.onBackPressed()
        }
    }

    override val navigationItemId = R.id.settings

    internal fun replaceFragment(key: String) {
        val args = Bundle()
        args.putString(KEY_SETTINGS_FRAGMENT, key)
        val fragment = PrefFragment()
        fragment.arguments = args
        supportFragmentManager.beginTransaction().replace(R.id.root_container, fragment).addToBackStack(null).commitAllowingStateLoss()
    }

    class PrefFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
        private var entryValues = emptyArray<String>()
        private var entries = emptyArray<String>()
        private var syncType = SyncService.FLAG_SYNC_NONE
        private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(requireContext()) }

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
                    entryValues = resources.getStringArray(R.array.pref_sync_status_values) ?: emptyArray()
                    entries = resources.getStringArray(R.array.pref_sync_status_entries) ?: emptyArray()

                    updateSyncStatusSummary(PREFERENCES_KEY_SYNC_STATUSES)
                }
                ACTION_ABOUT -> {
                    findPreference<Preference>("open_source_licenses")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        LibsBuilder()
                                .withFields(R.string::class.java.fields)
                                .withLibraries(
                                        "AndroidIcons",
                                        "Hugo",
                                        "MaterialRangeBar"
                                )
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

        @DebugLog
        override fun onStart() {
            super.onStart()
            EventBus.getDefault().register(this)
        }

        override fun onResume() {
            super.onResume()
            preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        @DebugLog
        override fun onStop() {
            super.onStop()
            EventBus.getDefault().unregister(this)
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
                    syncType = syncType or SyncService.FLAG_SYNC_PLAYS
                }
                PREFERENCES_KEY_SYNC_BUDDIES -> {
                    syncPrefs.clearBuddyListTimestamps()
                    syncType = syncType or SyncService.FLAG_SYNC_BUDDIES
                }
            }
        }

        private fun updateSyncStatusSummary(key: String) {
            val pref = findPreference<Preference>(key) ?: return
            val statuses = defaultSharedPreferences.getSyncStatusesOrDefault()
            pref.summary = if (statuses.isEmpty()) {
                getString(R.string.pref_list_empty)
            } else {
                entryValues.indices
                        .filter { statuses.contains(entryValues[it]) }
                        .joinToString { entries[it] }
            }
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            preference?.key?.let {
                return when {
                    it.startsWith(ACTION_PREFIX) -> {
                        (activity as SettingsActivity).replaceFragment(it)
                        true
                    }
                    else -> {
                        super.onPreferenceTreeClick(preference)
                    }
                }
            }
            return super.onPreferenceTreeClick(preference)
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

        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onEvent(event: SignInEvent) {
            updateAccountPrefs(event.username)
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onEvent(@Suppress("UNUSED_PARAMETER") event: SignOutEvent) {
            context?.toast(R.string.msg_sign_out_success)
            updateAccountPrefs("")
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
        private val FRAGMENT_MAP = buildFragmentMap()

        private fun buildFragmentMap(): ArrayMap<String, Int> {
            val map = ArrayMap<String, Int>()
            map[ACTION_ACCOUNT] = R.xml.preference_account
            map[ACTION_SYNC] = R.xml.preference_sync
            map[ACTION_DATA] = R.xml.preference_data
            map[ACTION_LOG] = R.xml.preference_log
            map[ACTION_ADVANCED] = R.xml.preference_advanced
            map[ACTION_ABOUT] = R.xml.preference_about
            map[ACTION_AUTHORS] = R.xml.preference_authors
            return map
        }
    }
}
