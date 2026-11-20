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
 * @param since Since when the changelog is complete, in milliseconds since the epoch
 * @param updatedAt When the changelog got refreshed, in milliseconds since the epoch
 * @param changes The changes, newest first
 * @param isTruncated Whether older changes got left out because of the limits of the APIs
 */
@Serializable
data class Changelog(
    val buildTime: Long,
    val since: Long,
    val updatedAt: Long,
    val changes: List<Change>,
    val isTruncated: Boolean = false,
)
