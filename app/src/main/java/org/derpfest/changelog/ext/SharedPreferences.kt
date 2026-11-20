/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ext

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.derpfest.changelog.models.TapAction

fun <T> SharedPreferences.preferenceFlow(
    vararg keys: String,
    getter: SharedPreferences.() -> T,
) = callbackFlow {
    val update = {
        trySend(getter())
    }

    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
        if (changedKey in keys) {
            update()
        }
    }

    registerOnSharedPreferenceChangeListener(listener)

    update()

    awaitClose {
        unregisterOnSharedPreferenceChangeListener(listener)
    }
}

// List prefs
const val TAP_ACTION_KEY = "tap_action"
private val TAP_ACTION_DEFAULT = TapAction.DETAILS.ordinal
var SharedPreferences.tapAction: TapAction
    get() = TapAction.entries[getInt(TAP_ACTION_KEY, TAP_ACTION_DEFAULT)]
    set(value) = edit {
        putInt(TAP_ACTION_KEY, value.ordinal)
    }
