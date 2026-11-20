/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.models

/**
 * The changes returned by a data source.
 *
 * @param changes The changes, newest first
 * @param isTruncated Whether older changes got left out because of the limits of the API
 */
data class ChangesResult(
    val changes: List<Change>,
    val isTruncated: Boolean,
)
