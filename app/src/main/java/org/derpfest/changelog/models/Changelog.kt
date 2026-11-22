/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.models

import kotlinx.serialization.Serializable

/**
 * The changes fetched for the build running on the device.
 *
 * @param buildTime The build the changelog belongs to, see [android.os.Build.TIME]
 * @param since Since when the changelog was fetched, in milliseconds since the epoch
 * @param updatedAt When the changelog got refreshed, in milliseconds since the epoch
 * @param changes The changes, newest first
 * @param isTruncated Whether older changes got left out because of the limits of the APIs
 * @param completeSince Changes at or after this time are all present. Older caches leave this
 *   unset and fall back to [isTruncated]
 */
@Serializable
data class Changelog(
    val buildTime: Long,
    val since: Long,
    val updatedAt: Long,
    val changes: List<Change>,
    val isTruncated: Boolean = false,
    val completeSince: Long = UNKNOWN_COMPLETE_SINCE,
) {
    /**
     * Whether [periodStart] reaches into changes that were left out.
     *
     * @param periodStart When the period on screen starts, in milliseconds since the epoch
     */
    fun isMissingChangesBefore(periodStart: Long) = when (completeSince) {
        UNKNOWN_COMPLETE_SINCE -> isTruncated
        else -> periodStart < completeSince
    }

    /**
     * The time from which this changelog is complete, or [Long.MAX_VALUE] when an older cache
     * only recorded that something was left out.
     */
    fun knownCompleteSince() = when {
        completeSince != UNKNOWN_COMPLETE_SINCE -> completeSince
        isTruncated -> Long.MAX_VALUE
        else -> since
    }

    companion object {
        /**
         * [completeSince] for a cache written before that field existed.
         */
        const val UNKNOWN_COMPLETE_SINCE = Long.MIN_VALUE
    }
}
