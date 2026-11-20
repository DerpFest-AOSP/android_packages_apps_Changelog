/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.models

import kotlinx.serialization.Serializable

/**
 * A change merged into one of the repositories covered by the changelog.
 *
 * @param id A unique identifier of the change
 * @param source Where the change comes from
 * @param repository The repository of the change, as `owner/name`
 * @param path The path of the repository in the source tree, if it's part of the manifest
 * @param branch The branch the change got merged into, if known
 * @param revision The hash of the commit
 * @param message The full commit message
 * @param author The name of the author
 * @param timestamp When the change got merged, in milliseconds since the epoch
 * @param url The web page of the change
 * @param isMerge Whether the change is a merge commit
 */
@Serializable
data class Change(
    val id: String,
    val source: ChangeSource,
    val repository: String,
    val path: String? = null,
    val branch: String? = null,
    val revision: String,
    val message: String,
    val author: String,
    val timestamp: Long,
    val url: String,
    val isMerge: Boolean = false,
) {
    /**
     * The first line of the commit message.
     */
    val subject: String
        get() = message.substringBefore('\n').trim()

    /**
     * The path of the repository in the source tree. Repositories that aren't part of the
     * manifest follow the LineageOS naming scheme, so their path is derived from their name.
     */
    val displayPath: String
        get() = path ?: repository.substringAfter('/')
            .removePrefix("android_")
            .removePrefix("proprietary_")
            .replaceFirst('_', '/')
            .replaceFirst('_', '/')
}
