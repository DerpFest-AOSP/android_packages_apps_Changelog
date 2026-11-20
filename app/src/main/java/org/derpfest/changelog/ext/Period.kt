/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ext

import org.derpfest.changelog.R
import org.derpfest.changelog.models.Period

val Period.labelResId: Int
    get() = when (this) {
        Period.SINCE_BUILD -> R.string.period_since_build
        Period.LAST_30_DAYS -> R.string.period_last_30_days
    }
