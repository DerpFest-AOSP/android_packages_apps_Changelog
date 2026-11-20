/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.datasources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.derpfest.changelog.ext.fetch
import org.derpfest.changelog.models.Change
import org.derpfest.changelog.models.ChangeSource
import org.derpfest.changelog.models.ChangelogError
import org.derpfest.changelog.models.ChangesResult
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * Searches the commits of GitHub organizations. Anonymous clients may do 10 searches per minute.
 */
class GitHubDataSource {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Search the commits that got committed since [since], newest first. GitHub only searches the
     * default branch of each repository and returns at most [MAX_RESULTS] commits.
     *
     * @param organizations The organizations to search, with the source of their changes
     * @param since The minimum committer date, in milliseconds since the epoch
     * @throws ChangelogError If a request failed
     */
    suspend fun searchCommits(
        organizations: Map<String, ChangeSource>,
        since: Long,
    ) = withContext(Dispatchers.IO) {
        val query = buildList {
            organizations.keys.forEach { add("org:$it") }
            add("committer-date:>=${Instant.ofEpochMilli(since).truncatedTo(ChronoUnit.SECONDS)}")
        }.joinToString(" ")

        val changes = mutableListOf<Change>()
        var page = 1
        var isTruncated = false

        while (true) {
            val response = searchCommits(query, page)
            response.items.mapNotNullTo(changes) { it.toChange(organizations) }

            val lastPage = (minOf(response.totalCount, MAX_RESULTS) + PER_PAGE - 1) / PER_PAGE
            if (response.items.size < PER_PAGE || page >= lastPage) {
                isTruncated = response.totalCount > MAX_RESULTS
                break
            }

            page++
        }

        ChangesResult(
            changes = changes.distinctBy { it.id },
            isTruncated = isTruncated,
        )
    }

    private fun searchCommits(query: String, page: Int): SearchCommitsResponse {
        val url = URL(
            "$API_URL/search/commits?q=${URLEncoder.encode(query, "UTF-8")}" +
                    "&sort=committer-date&order=desc&per_page=$PER_PAGE&page=$page"
        )

        val body = url.fetch(
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to API_VERSION,
        )

        return try {
            json.decodeFromString<SearchCommitsResponse>(body)
        } catch (e: SerializationException) {
            throw ChangelogError.InvalidResponse
        }
    }

    private fun CommitItem.toChange(organizations: Map<String, ChangeSource>): Change? {
        val owner = repository.fullName.substringBefore('/')
        val source = organizations.entries.firstOrNull {
            it.key.equals(owner, ignoreCase = true)
        }?.value ?: return null

        val date = commit.committer?.date ?: commit.author?.date ?: return null
        val timestamp = try {
            OffsetDateTime.parse(date).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            return null
        }

        return Change(
            id = "${repository.fullName}/$sha",
            source = source,
            repository = repository.fullName,
            revision = sha,
            message = commit.message,
            author = commit.author?.name ?: commit.committer?.name.orEmpty(),
            timestamp = timestamp,
            url = htmlUrl,
            isMerge = parents.size > 1,
        )
    }

    @Serializable
    private data class SearchCommitsResponse(
        @SerialName("total_count") val totalCount: Int,
        val items: List<CommitItem>,
    )

    @Serializable
    private data class CommitItem(
        val sha: String,
        @SerialName("html_url") val htmlUrl: String,
        val commit: Commit,
        val repository: Repository,
        val parents: List<Parent> = listOf(),
    )

    @Serializable
    private data class Commit(
        val author: GitUser? = null,
        val committer: GitUser? = null,
        val message: String,
    )

    @Serializable
    private data class GitUser(
        val name: String? = null,
        val date: String? = null,
    )

    @Serializable
    private data class Repository(
        @SerialName("full_name") val fullName: String,
    )

    @Serializable
    private data class Parent(
        val sha: String,
    )

    companion object {
        private const val API_URL = "https://api.github.com"
        private const val API_VERSION = "2022-11-28"

        private const val PER_PAGE = 100

        /**
         * The maximum amount of results GitHub returns for a search.
         */
        private const val MAX_RESULTS = 1000
    }
}
