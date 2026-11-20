/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.models

/**
 * What tapping a change in the list does.
 */
enum class TapAction {
    /**
     * Show the details of the change in a sheet.
     */
    DETAILS,

    /**
     * Expand the change in the list to show its whole message.
     */
    EXPAND,
}
