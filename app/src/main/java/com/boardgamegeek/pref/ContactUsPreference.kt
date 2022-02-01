package com.boardgamegeek.pref

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.preference.Preference
import com.boardgamegeek.R

class ContactUsPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    init {
        intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/email"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(getContext().getString(R.string.pref_about_contact_us_summary)))
            putExtra(Intent.EXTRA_SUBJECT, R.string.pref_feedback_title)
        }
    }
}
