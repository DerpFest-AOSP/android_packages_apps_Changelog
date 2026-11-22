/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * App screen.
 */
sealed interface Screen {
    /**
     * Name kept across recreation.
     */
    val name: String

    /**
     * Changelog screen.
     */
    data object Changelog : Screen {
        override val name = "changelog"
    }

    /**
     * Devices screen.
     */
    data object Devices : Screen {
        override val name = "devices"
    }

    /**
     * Settings screen.
     */
    data object Settings : Screen {
        override val name = "settings"
    }

    companion object {
        /**
         * The screen saved under [name], or null if it isn't one of the screens.
         */
        fun fromName(name: String): Screen? = when (name) {
            Changelog.name -> Changelog
            Devices.name -> Devices
            Settings.name -> Settings
            else -> null
        }
    }
}

/**
 * Saves the back stack across recreation. An empty stack is the changelog.
 */
val NavigationBackStackSaver = listSaver<SnapshotStateList<Screen>, String>(
    save = { screens ->
        screens.map { it.name }.ifEmpty { listOf(Screen.Changelog.name) }
    },
    restore = { saved ->
        mutableStateListOf<Screen>().apply {
            saved.mapNotNull(Screen::fromName).forEach(::add)
            if (isEmpty()) {
                add(Screen.Changelog)
            }
        }
    },
)
