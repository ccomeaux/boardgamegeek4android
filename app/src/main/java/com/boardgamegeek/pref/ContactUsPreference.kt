package com.boardgamegeek.pref

import android.content.Context
import android.content.Intent
import androidx.preference.Preference
import android.util.AttributeSet

import com.boardgamegeek.R
import com.boardgamegeek.util.ActivityUtils

class ContactUsPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    init {
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/email"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(getContext().getString(R.string.pref_about_contact_us_summary)))
            putExtra(Intent.EXTRA_SUBJECT, R.string.pref_feedback_title)
        }
        if (ActivityUtils.isIntentAvailable(getContext(), emailIntent)) {
            intent = emailIntent
        }
    }
}
