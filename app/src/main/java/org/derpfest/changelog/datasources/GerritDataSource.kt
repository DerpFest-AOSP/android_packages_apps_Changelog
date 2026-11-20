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
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Gerrit REST API client, used for the repositories tracked straight from LineageOS.
 *
 * @param url The URL of the Gerrit instance
 */
class GerritDataSource(private val url: String) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Get the changes merged since [since], newest first.
     *
     * Listing every merged change of the branches is cheap, but most of them belong to device
     * repositories, so only the changes of the requested projects get fetched with their commit
     * details afterwards.
     *
     * @param projectBranches The branches to look at for each project
     * @param since The minimum submit date, in milliseconds since the epoch
     * @throws ChangelogError If a request failed
     */
    suspend fun getMergedChanges(
        projectBranches: Map<String, Set<String>>,
        since: Long,
    ) = withContext(Dispatchers.IO) {
        if (projectBranches.isEmpty()) {
            return@withContext ChangesResult(listOf(), false)
        }

        val branchesQuery = projectBranches.values.flatten().distinct().sorted().joinToString(
            separator = " OR ",
            prefix = "(",
            postfix = ")",
        ) { "branch:$it" }
        val query = "status:merged after:\"${queryDateFormatter.format(Instant.ofEpochMilli(since))}\" $branchesQuery"

        val numbers = mutableListOf<Int>()
        var start = 0
        var hasMoreChanges: Boolean
        do {
            val changes = queryChanges(query, LIST_PAGE_SIZE, start)

            changes.filter {
                projectBranches[it.project]?.contains(it.branch) == true
                        && (it.submittedTimestamp ?: 0) >= since
            }.mapTo(numbers) { it.number }

            start += changes.size
            hasMoreChanges = changes.lastOrNull()?.moreChanges == true
        } while (hasMoreChanges && start < MAX_LISTED_CHANGES)

        ChangesResult(
            changes = numbers.take(MAX_CHANGES).chunked(DETAILS_PAGE_SIZE).flatMap { chunk ->
                queryChanges(
                    query = chunk.joinToString(" OR ") { "change:$it" },
                    limit = chunk.size,
                    options = DETAILS_OPTIONS,
                ).mapNotNull { it.toChange() }
            }.sortedByDescending { it.timestamp },
            isTruncated = hasMoreChanges || numbers.size > MAX_CHANGES,
        )
    }

    private fun queryChanges(
        query: String,
        limit: Int,
        start: Int = 0,
        options: List<String> = listOf(),
    ): List<ChangeInfo> {
        val url = URL(
            buildString {
                append("$url/changes/?q=${URLEncoder.encode(query, "UTF-8")}&n=$limit")
                if (start > 0) {
                    append("&S=$start")
                }
                options.forEach {
                    append("&o=$it")
                }
            }
        )

        val body = url.fetch("Accept" to "application/json")

        return try {
            json.decodeFromString<List<ChangeInfo>>(body.removePrefix(XSSI_PREFIX))
        } catch (e: SerializationException) {
            throw ChangelogError.InvalidResponse
        }
    }

    private fun ChangeInfo.toChange(): Change? {
        val revision = currentRevision ?: return null
        val commit = revisions[revision]?.commit ?: return null
        val timestamp = submittedTimestamp ?: return null

        return Change(
            id = "$project/$number",
            source = ChangeSource.LINEAGEOS,
            repository = project,
            branch = branch,
            revision = revision,
            message = commit.message,
            author = commit.author?.name.orEmpty(),
            timestamp = timestamp,
            url = "$url/c/$project/+/$number",
            isMerge = commit.parents.size > 1,
        )
    }

    @Serializable
    private data class ChangeInfo(
        @SerialName("_number") val number: Int,
        val project: String,
        val branch: String,
        val submitted: String? = null,
        @SerialName("current_revision") val currentRevision: String? = null,
        val revisions: Map<String, RevisionInfo> = mapOf(),
        @SerialName("_more_changes") val moreChanges: Boolean = false,
    ) {
        val submittedTimestamp: Long?
            get() = submitted?.let {
                try {
                    LocalDateTime.parse(it, timestampFormatter)
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli()
                } catch (e: DateTimeParseException) {
                    null
                }
            }
    }

    @Serializable
    private data class RevisionInfo(
        val commit: CommitInfo? = null,
    )

    @Serializable
    private data class CommitInfo(
        val parents: List<ParentInfo> = listOf(),
        val author: GitPersonInfo? = null,
        val message: String = "",
    )

    @Serializable
    private data class GitPersonInfo(
        val name: String,
    )

    @Serializable
    private data class ParentInfo(
        val commit: String,
    )

    companion object {
        /**
         * Prefix Gerrit adds to JSON responses against cross-site script inclusion.
         */
        private const val XSSI_PREFIX = ")]}'"

        private const val LIST_PAGE_SIZE = 500
        private const val DETAILS_PAGE_SIZE = 50

        private const val MAX_LISTED_CHANGES = 5000
        private const val MAX_CHANGES = 1000

        private val DETAILS_OPTIONS = listOf("CURRENT_REVISION", "CURRENT_COMMIT")

        private val queryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")
            .withZone(ZoneOffset.UTC)

        private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS")
    }
}
