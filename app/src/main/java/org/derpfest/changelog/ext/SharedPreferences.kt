/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ext

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import android.os.Build
import org.derpfest.changelog.models.ChangeSource
import org.derpfest.changelog.models.ChangesFilter
import org.derpfest.changelog.models.Period
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

// Filter prefs
const val PERIOD_KEY = "period"
private val PERIOD_DEFAULT = Period.SINCE_BUILD.ordinal
var SharedPreferences.period: Period
    get() = Period.entries[getInt(PERIOD_KEY, PERIOD_DEFAULT)]
    set(value) = edit {
        putInt(PERIOD_KEY, value.ordinal)
    }

const val SOURCES_KEY = "sources"
private val SOURCES_DEFAULT = ChangeSource.entries.toSet()
var SharedPreferences.sources: Set<ChangeSource>
    get() = getStringSet(SOURCES_KEY, null)?.mapNotNullTo(mutableSetOf()) { name ->
        ChangeSource.entries.firstOrNull { it.name == name }
    } ?: SOURCES_DEFAULT
    set(value) = edit {
        putStringSet(SOURCES_KEY, value.mapTo(mutableSetOf()) { it.name })
    }

const val SHOW_TRANSLATIONS_KEY = "show_translations"
private const val SHOW_TRANSLATIONS_DEFAULT = true
var SharedPreferences.showTranslations: Boolean
    get() = getBoolean(SHOW_TRANSLATIONS_KEY, SHOW_TRANSLATIONS_DEFAULT)
    set(value) = edit {
        putBoolean(SHOW_TRANSLATIONS_KEY, value)
    }

const val SHOW_MERGES_KEY = "show_merges"
private const val SHOW_MERGES_DEFAULT = true
var SharedPreferences.showMerges: Boolean
    get() = getBoolean(SHOW_MERGES_KEY, SHOW_MERGES_DEFAULT)
    set(value) = edit {
        putBoolean(SHOW_MERGES_KEY, value)
    }

val CHANGES_FILTER_KEYS = arrayOf(PERIOD_KEY, SOURCES_KEY, SHOW_TRANSLATIONS_KEY, SHOW_MERGES_KEY)
val SharedPreferences.changesFilter: ChangesFilter
    get() = ChangesFilter(
        period = period,
        sources = sources,
        showTranslations = showTranslations,
        showMerges = showMerges,
    )

// Devices prefs
const val SHOW_ALL_DEVICES_KEY = "show_all_devices"
private const val SHOW_ALL_DEVICES_DEFAULT = false
var SharedPreferences.showAllDevices: Boolean
    get() = getBoolean(SHOW_ALL_DEVICES_KEY, SHOW_ALL_DEVICES_DEFAULT)
    set(value) = edit {
        putBoolean(SHOW_ALL_DEVICES_KEY, value)
    }

const val WATCHED_DEVICES_KEY = "watched_devices"
var SharedPreferences.watchedDevices: Set<String>
    get() = getStringSet(WATCHED_DEVICES_KEY, null) ?: setOf(Build.DEVICE)
    set(value) = edit {
        putStringSet(WATCHED_DEVICES_KEY, value)
    }

val DEVICE_FILTER_KEYS = arrayOf(SHOW_ALL_DEVICES_KEY, WATCHED_DEVICES_KEY)

// List prefs
const val TAP_ACTION_KEY = "tap_action"
private val TAP_ACTION_DEFAULT = TapAction.DETAILS.ordinal
var SharedPreferences.tapAction: TapAction
    get() = TapAction.entries[getInt(TAP_ACTION_KEY, TAP_ACTION_DEFAULT)]
    set(value) = edit {
        putInt(TAP_ACTION_KEY, value.ordinal)
    }
