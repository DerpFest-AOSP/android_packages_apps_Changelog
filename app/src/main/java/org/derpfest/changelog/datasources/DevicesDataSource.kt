/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.datasources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.derpfest.changelog.ext.fetch
import org.derpfest.changelog.models.ChangelogError
import org.derpfest.changelog.models.Device
import java.net.URI
import java.net.URL

/**
 * Fetches the devices DerpFest ships and the repositories of DerpFest-Devices.
 */
class DevicesDataSource(
    private val indexUrl: String,
    private val organization: String,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Fetch the devices DerpFest ships builds for.
     *
     * @throws ChangelogError If the devices couldn't be fetched
     */
    suspend fun getDevices(): List<Device> = withContext(Dispatchers.IO) {
        val response = json.decodeFromString<DevicesIndexResponse>(
            URL(indexUrl).fetch()
        )

        response.devices
            .map {
                Device(
                    codename = it.codename,
                    displayName = it.displayName,
                    maintainer = it.maintainer?.takeUnless { name ->
                        name.isBlank() || name.equals("Unknown", ignoreCase = true)
                    },
                )
            }
            .sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
            )
    }

    /**
     * Fetch the public repositories of the devices organization.
     *
     * @throws ChangelogError If the repositories couldn't be fetched
     */
    suspend fun getRepositories(): List<GitHubRepository> = withContext(Dispatchers.IO) {
        buildList {
            var page = 1

            while (true) {
                val url = URL(
                    "https://api.github.com/orgs/$organization/repos" +
                            "?per_page=$REPOS_PER_PAGE&page=$page&type=public"
                )

                val repositories = json.decodeFromString<List<GitHubRepositoryResponse>>(
                    url.fetch(
                        "Accept" to "application/vnd.github+json",
                        "X-GitHub-Api-Version" to "2022-11-28",
                    )
                )

                addAll(
                    repositories.map {
                        GitHubRepository(
                            name = it.name,
                            defaultBranch = it.defaultBranch,
                        )
                    }
                )

                if (repositories.size < REPOS_PER_PAGE) {
                    break
                }

                page++
            }
        }
    }

    /**
     * Fetch the repositories a device tree depends on.
     *
     * @param repository The name of the repository
     * @param branch The branch to read
     * @return The names of the repositories, or an empty list if none are declared
     * @throws ChangelogError If the dependencies couldn't be fetched for a reason other than
     *   the file being missing
     */
    suspend fun getDependencies(
        repository: String,
        branch: String,
    ): List<String> = withContext(Dispatchers.IO) {
        val url = URI(
            "https://raw.githubusercontent.com/$organization/$repository/$branch/lineage.dependencies"
        ).toURL()

        try {
            json.decodeFromString<List<DependencyResponse>>(url.fetch()).map { it.repository }
        } catch (e: ChangelogError.Http) {
            if (e.code == 404) {
                emptyList()
            } else {
                throw e
            }
        }
    }

    /**
     * A repository of the devices organization.
     *
     * @param name The name of the repository
     * @param defaultBranch The default branch of the repository
     */
    @Serializable
    data class GitHubRepository(
        val name: String,
        val defaultBranch: String,
    )

    @Serializable
    private data class DevicesIndexResponse(
        val devices: List<DeviceResponse>,
    )

    @Serializable
    private data class DeviceResponse(
        val codename: String,
        val displayName: String,
        val maintainer: String? = null,
    )

    @Serializable
    private data class GitHubRepositoryResponse(
        val name: String,
        @SerialName("default_branch") val defaultBranch: String,
    )

    @Serializable
    private data class DependencyResponse(
        val repository: String,
    )

    companion object {
        private const val REPOS_PER_PAGE = 100
    }
}
