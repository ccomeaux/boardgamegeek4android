package com.boardgamegeek.pref

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.support.v4.util.ArrayMap
import android.view.MenuItem
import android.widget.Toast
import com.boardgamegeek.R
import com.boardgamegeek.events.SignInEvent
import com.boardgamegeek.events.SignOutEvent
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.DrawerActivity
import com.boardgamegeek.util.PreferencesUtils
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import hugo.weaving.DebugLog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SettingsActivity : DrawerActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            fragmentManager.beginTransaction().add(R.id.root_container, PrefFragment(), TAG_SINGLE_PANE).commit()
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
        if (!fragmentManager.popBackStackImmediate()) {
            super.onBackPressed()
        }
    }

    internal fun replaceFragment(key: String) {
        val args = Bundle()
        args.putString(KEY_SETTINGS_FRAGMENT, key)
        val fragment = PrefFragment()
        fragment.arguments = args
        fragmentManager.beginTransaction().replace(R.id.root_container, fragment).addToBackStack(null).commitAllowingStateLoss()
    }

    class PrefFragment : PreferenceFragment(), OnSharedPreferenceChangeListener {
        private var entryValues = emptyArray<String>()
        private var entries = emptyArray<String>()
        private var syncType = 0

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val fragment = arguments?.getString(KEY_SETTINGS_FRAGMENT)
            if (fragment == null) {
                addPreferencesFromResource(R.xml.preference_headers)
            } else {
                val fragmentId = FRAGMENT_MAP[fragment]
                if (fragmentId != null) {
                    addPreferencesFromResource(fragmentId)
                }
            }

            entryValues = resources.getStringArray(R.array.pref_sync_status_values) ?: emptyArray()
            entries = resources.getStringArray(R.array.pref_sync_status_entries) ?: emptyArray()

            updateSyncStatusSummary(PreferencesUtils.KEY_SYNC_STATUSES)

            findPreference("open_source_licenses")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                LibsBuilder()
                        .withFields(R.string::class.java.fields)
                        .withLibraries(
                                "AndroidIcons",
                                "Hugo",
                                "Jsoup",
                                "LeakCanary",
                                "MaterialRangeBar",
                                "MPAndroidChart",
                                "PhotoView")
                        .withAutoDetect(true)
                        .withLicenseShown(true)
                        .withActivityTitle(getString(R.string.pref_about_licenses))
                        .withActivityTheme(R.style.Theme_bgglight_About)
                        .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                        .withAboutIconShown(true)
                        .withAboutAppName(getString(R.string.app_name))
                        .withAboutVersionShownName(true)
                        .start(activity)
                true
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
            if (syncType > 0) {
                SyncService.sync(activity, syncType)
                syncType = 0
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            when (key) {
                PreferencesUtils.KEY_SYNC_STATUSES -> {
                    updateSyncStatusSummary(key)
                    SyncPrefs.clearCollection(activity)
                    syncType = syncType or SyncService.FLAG_SYNC_COLLECTION
                }
                PreferencesUtils.KEY_SYNC_STATUSES_OLD -> {
                    SyncPrefs.clearCollection(activity)
                    syncType = syncType or SyncService.FLAG_SYNC_COLLECTION
                }
                PreferencesUtils.KEY_SYNC_PLAYS -> {
                    SyncPrefs.clearPlaysTimestamps(activity)
                    syncType = syncType or SyncService.FLAG_SYNC_PLAYS
                }
                PreferencesUtils.KEY_SYNC_BUDDIES -> {
                    SyncPrefs.clearBuddyListTimestamps(activity)
                    syncType = syncType or SyncService.FLAG_SYNC_BUDDIES
                }
            }
        }

        private fun updateSyncStatusSummary(key: String) {
            val pref = findPreference(key) ?: return
            val statuses = PreferencesUtils.getSyncStatuses(activity)
            if (statuses == null || statuses.size == 0) {
                pref.setSummary(R.string.pref_list_empty)
            } else {
                val sb = StringBuilder()
                entryValues.indices
                        .filter { statuses.contains(entryValues[it]) }
                        .forEach { sb.append(entries[it]).append(", ") }
                pref.summary = if (sb.length > 2) sb.substring(0, sb.length - 2) else sb.toString()
            }
        }

        override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen, preference: Preference): Boolean {
            with(preference) {
                if (key != null && key.startsWith(ACTION_PREFIX)) {
                    (activity as SettingsActivity).replaceFragment(key)
                    return true
                }
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference)
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onEvent(event: SignInEvent) {
            updateAccountPrefs(event.username)
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onEvent(@Suppress("UNUSED_PARAMETER") event: SignOutEvent) {
            Toast.makeText(activity, R.string.msg_sign_out_success, Toast.LENGTH_SHORT).show()
            updateAccountPrefs("")
        }

        private fun updateAccountPrefs(username: String) {
            (findPreference(PreferencesUtils.KEY_LOGIN) as? LoginPreference)?.update(username)
            (findPreference(PreferencesUtils.KEY_LOGOUT) as? SignOutPreference)?.update()
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