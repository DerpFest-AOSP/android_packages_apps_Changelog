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
import org.derpfest.changelog.viewmodels.ChangelogViewModel

@Composable
fun ChangelogNavDisplay(
    navigationBackStack: SnapshotStateList<Screen>,
    changelogViewModel: ChangelogViewModel,
    modifier: Modifier = Modifier,
) {
    val entryProvider = entryProvider<Screen> {
        entry<Screen.Changelog> {
            ChangelogScreen(
                changelogViewModel = changelogViewModel,
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
