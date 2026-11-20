/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ext

import org.derpfest.changelog.R
import org.derpfest.changelog.models.TapAction

val TapAction.labelResId: Int
    get() = when (this) {
        TapAction.DETAILS -> R.string.tap_action_details
        TapAction.EXPAND -> R.string.tap_action_expand
    }

val TapAction.summaryResId: Int
    get() = when (this) {
        TapAction.DETAILS -> R.string.tap_action_details_summary
        TapAction.EXPAND -> R.string.tap_action_expand_summary
    }
