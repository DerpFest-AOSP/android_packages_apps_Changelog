/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import org.derpfest.changelog.ui.composables.ChangelogFloatingToolbar
import org.derpfest.changelog.ui.composables.ChangelogToolbarHost
import org.derpfest.changelog.ui.screens.ChangelogScreen
import org.derpfest.changelog.ui.screens.DevicesScreen
import org.derpfest.changelog.ui.screens.SettingsScreen
import org.derpfest.changelog.viewmodels.ChangelogViewModel
import org.derpfest.changelog.viewmodels.DevicesViewModel
import org.derpfest.changelog.viewmodels.SettingsViewModel

@Composable
fun ChangelogNavDisplay(
    navigationBackStack: SnapshotStateList<Screen>,
    changelogViewModel: ChangelogViewModel,
    devicesViewModel: DevicesViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val toolbarHost = remember { ChangelogToolbarHost() }
    val onChangelog = navigationBackStack.lastOrNull() == Screen.Changelog
    val entryProvider = entryProvider<Screen> {
        entry<Screen.Changelog> {
            ChangelogScreen(
                changelogViewModel = changelogViewModel,
                onShowDevices = { navigationBackStack.add(Screen.Devices) },
                onShowSettings = { navigationBackStack.add(Screen.Settings) },
                toolbarHost = toolbarHost,
            )
        }

        entry<Screen.Devices> {
            DevicesScreen(
                devicesViewModel = devicesViewModel,
                onBack = { navigationBackStack.removeLastOrNull() },
            )
        }

        entry<Screen.Settings> {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                onBack = { navigationBackStack.removeLastOrNull() },
            )
        }
    }

    // The toolbar window sits above the activity, so a page fade would leave it on screen until
    // the old page is removed. It stays outside that fade and slides on its own.
    Box(modifier) {
        NavDisplay(
            backStack = navigationBackStack,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            popTransitionSpec = { fadeIn() togetherWith fadeOut() },
            predictivePopTransitionSpec = { fadeIn() togetherWith fadeOut() },
            entryProvider = entryProvider,
        )
        ChangelogToolbarOverlay(
            onChangelog = onChangelog,
            toolbarHost = toolbarHost,
        )
    }
}

@Composable
private fun ChangelogToolbarOverlay(
    onChangelog: Boolean,
    toolbarHost: ChangelogToolbarHost,
) {
    ChangelogFloatingToolbar(
        onPage = onChangelog,
        expanded = toolbarHost.expanded,
        covered = onChangelog && toolbarHost.covered,
        bounds = toolbarHost.bounds,
        onRefresh = { if (onChangelog) toolbarHost.onRefresh() },
        onSearch = { if (onChangelog) toolbarHost.onSearch() },
        onShowFilters = { if (onChangelog) toolbarHost.onShowFilters() },
        onShowDevices = { if (onChangelog) toolbarHost.onShowDevices() },
        onShowSettings = { if (onChangelog) toolbarHost.onShowSettings() },
    )
}
