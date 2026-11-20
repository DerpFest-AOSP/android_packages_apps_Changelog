/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.models

import android.os.Build
import java.util.concurrent.TimeUnit

/**
 * The time range covered by the changelog.
 */
enum class Period {
    /**
     * The changes merged since the build running on the device got made.
     */
    SINCE_BUILD,

    /**
     * The changes merged during the last 30 days.
     */
    LAST_30_DAYS;

    /**
     * When the period starts, in milliseconds since the epoch.
     *
     * @param now The current time, in milliseconds since the epoch
     */
    fun startTime(now: Long) = when (this) {
        SINCE_BUILD -> Build.TIME
        LAST_30_DAYS -> now - TimeUnit.DAYS.toMillis(30)
    }
}
