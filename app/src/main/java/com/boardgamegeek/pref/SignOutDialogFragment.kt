package com.boardgamegeek.pref

import android.os.Bundle
import androidx.preference.PreferenceDialogFragmentCompat
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.service.SyncService

class SignOutDialogFragment : PreferenceDialogFragmentCompat() {
    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            SyncService.cancelSync(context)
            Authenticator.signOut(context)
        }
    }

    companion object {
        fun newInstance(key: String): SignOutDialogFragment {
            return SignOutDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_KEY, key) }
            }
        }
    }
}
