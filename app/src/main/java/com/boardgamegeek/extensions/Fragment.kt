@file:Suppress("NOTHING_TO_INLINE", "unused")

package com.boardgamegeek.extensions

import android.app.Activity
import androidx.fragment.app.Fragment

inline fun <reified T : Activity> Fragment.startActivity(vararg params: Pair<String, Any?>) {
    requireActivity().startActivity(T::class.java, params)
}

inline fun Fragment.toast(messageResourceId: Int) = requireActivity().toast(messageResourceId)

inline fun Fragment.toast(message: CharSequence) = requireActivity().toast(message)

inline fun Fragment.longToast(message: CharSequence) = requireActivity().toast(message)
