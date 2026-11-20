/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.derpfest.changelog.ui.navigation.ChangelogNavDisplay
import org.derpfest.changelog.ui.navigation.Screen
import org.derpfest.changelog.viewmodels.ChangelogViewModel
import org.derpfest.changelog.viewmodels.DevicesViewModel
import org.derpfest.changelog.viewmodels.SettingsViewModel

@Composable
fun ChangelogApp(
    changelogViewModel: ChangelogViewModel,
    devicesViewModel: DevicesViewModel,
    settingsViewModel: SettingsViewModel,
) {
    val navigationBackStack = remember { mutableStateListOf<Screen>(Screen.Changelog) }

    ChangelogNavDisplay(
        navigationBackStack = navigationBackStack,
        changelogViewModel = changelogViewModel,
        devicesViewModel = devicesViewModel,
        settingsViewModel = settingsViewModel,
        modifier = Modifier.fillMaxSize(),
    )
}
