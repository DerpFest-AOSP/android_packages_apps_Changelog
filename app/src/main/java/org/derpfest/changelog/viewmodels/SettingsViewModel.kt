/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.viewmodels

import android.app.Application
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.derpfest.changelog.ext.HIGHLIGHT_NEW_KEY
import org.derpfest.changelog.ext.TAP_ACTION_KEY
import org.derpfest.changelog.ext.highlightNew
import org.derpfest.changelog.ext.preferenceFlow
import org.derpfest.changelog.ext.sharedPreferences
import org.derpfest.changelog.ext.tapAction
import org.derpfest.changelog.models.TapAction

/**
 * View model of the settings screen.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.sharedPreferences

    /**
     * What tapping a change does.
     */
    val tapAction = sharedPreferences.preferenceFlow(
        TAP_ACTION_KEY,
        getter = SharedPreferences::tapAction,
    )
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sharedPreferences.tapAction,
        )

    /**
     * Whether new changes are highlighted in the list.
     */
    val highlightNew = sharedPreferences.preferenceFlow(
        HIGHLIGHT_NEW_KEY,
        getter = SharedPreferences::highlightNew,
    )
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sharedPreferences.highlightNew,
        )

    /**
     * The version name of the app.
     */
    val versionName = runCatching {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.packageManager.getPackageInfo(
                application.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            application.packageManager.getPackageInfo(application.packageName, 0)
        }

        packageInfo.versionName
    }.getOrNull() ?: "1.0"

    fun setTapAction(tapAction: TapAction) {
        sharedPreferences.tapAction = tapAction
    }

    fun setHighlightNew(highlightNew: Boolean) {
        sharedPreferences.highlightNew = highlightNew
    }
}
