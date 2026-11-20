/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.navigation

/**
 * App screen.
 */
sealed interface Screen {
    /**
     * Changelog screen.
     */
    data object Changelog : Screen
}
