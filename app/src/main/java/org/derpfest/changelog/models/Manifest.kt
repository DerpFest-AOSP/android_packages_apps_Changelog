/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.models

import kotlinx.serialization.Serializable

/**
 * A repo manifest.
 *
 * @param projects The projects of the manifest
 */
@Serializable
data class Manifest(
    val projects: List<Project>,
) {
    /**
     * A project of the manifest, with its remote and revision resolved.
     *
     * @param path The path of the project in the source tree
     * @param name The name of the project on its remote
     * @param url The URL of the project's repository
     * @param review The host of the review server of the project's remote, if any
     * @param revision The revision tracked by the manifest, usually a branch
     */
    @Serializable
    data class Project(
        val path: String,
        val name: String,
        val url: String,
        val review: String?,
        val revision: String,
    ) {
        /**
         * The `owner/name` of the project's GitHub repository, or null if it isn't hosted on
         * GitHub.
         */
        val gitHubRepository: String?
            get() = url.takeIf { it.startsWith(GITHUB_URL) }?.removePrefix(GITHUB_URL)

        /**
         * The branch tracked by the manifest, or null if it tracks a tag or a commit.
         */
        val branch: String?
            get() = when {
                revision.startsWith(BRANCH_PREFIX) -> revision.removePrefix(BRANCH_PREFIX)
                revision.startsWith("refs/") -> null
                commitRegex.matches(revision) -> null
                else -> revision
            }

        companion object {
            private const val GITHUB_URL = "https://github.com/"
            private const val BRANCH_PREFIX = "refs/heads/"

            private val commitRegex = "[0-9a-f]{40}".toRegex()
        }
    }
}
