/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import org.derpfest.changelog.ui.screens.ChangelogScreen
import org.derpfest.changelog.ui.screens.DevicesScreen
import org.derpfest.changelog.viewmodels.ChangelogViewModel
import org.derpfest.changelog.viewmodels.DevicesViewModel

@Composable
fun ChangelogNavDisplay(
    navigationBackStack: SnapshotStateList<Screen>,
    changelogViewModel: ChangelogViewModel,
    devicesViewModel: DevicesViewModel,
    modifier: Modifier = Modifier,
) {
    val entryProvider = entryProvider<Screen> {
        entry<Screen.Changelog> {
            ChangelogScreen(
                changelogViewModel = changelogViewModel,
                onShowDevices = { navigationBackStack.add(Screen.Devices) },
            )
        }

        entry<Screen.Devices> {
            DevicesScreen(
                devicesViewModel = devicesViewModel,
                onBack = { navigationBackStack.removeLastOrNull() },
            )
        }
    }

    NavDisplay(
        backStack = navigationBackStack,
        modifier = modifier,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        popTransitionSpec = { fadeIn() togetherWith fadeOut() },
        predictivePopTransitionSpec = { fadeIn() togetherWith fadeOut() },
        entryProvider = entryProvider,
    )
}
