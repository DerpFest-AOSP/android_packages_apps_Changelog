/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.repositories

import android.content.Context
import android.os.Build
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.derpfest.changelog.R
import org.derpfest.changelog.datasources.GerritDataSource
import org.derpfest.changelog.datasources.GitHubDataSource
import org.derpfest.changelog.ext.readJson
import org.derpfest.changelog.ext.writeJson
import org.derpfest.changelog.models.ChangeSource
import org.derpfest.changelog.models.Changelog
import org.derpfest.changelog.models.ChangelogError
import org.derpfest.changelog.models.ChangesResult
import org.derpfest.changelog.models.Manifest
import org.derpfest.changelog.models.Period
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Fetches the changes of the repositories the ROM is built from and keeps them for offline use.
 * The kept changes get dropped once another build is running.
 */
class ChangelogRepository(
    context: Context,
    private val manifestRepository: ManifestRepository,
) {
    private val gerritUrl = context.getString(R.string.gerrit_url)

    private val gitHubDataSource = GitHubDataSource()
    private val gerritDataSource = GerritDataSource(gerritUrl)

    private val gitHubOrganizations = mapOf(
        context.getString(R.string.github_organization) to ChangeSource.DERPFEST,
        context.getString(R.string.github_devices_organization) to ChangeSource.DEVICES,
    )

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val cacheFile = AtomicFile(File(context.cacheDir, CACHE_FILE_NAME))

    private val mutex = Mutex()

    private var isLoaded = false

    /**
     * The changes known when the app got started, used to tell which changes are new.
     */
    private var baseline: Baseline? = null

    private val _changelog = MutableStateFlow<Changelog?>(null)

    /**
     * The changelog of the running build, null until it got loaded or fetched.
     */
    val changelog = _changelog.asStateFlow()

    private val _newChangeIds = MutableStateFlow(setOf<String>())

    /**
     * The identifiers of the changes that showed up since the app got started.
     */
    val newChangeIds = _newChangeIds.asStateFlow()

    /**
     * Load the changelog kept from the last refresh.
     */
    suspend fun load() = mutex.withLock {
        loadLocked()
    }

    /**
     * Refresh the changelog so it covers [period]. When the kept changelog already covers it,
     * only the changes since the last refresh get fetched.
     *
     * @param period The period the changelog has to cover
     * @param force Whether to refresh even if the changelog got refreshed moments ago
     * @throws ChangelogError If the changelog couldn't be refreshed
     */
    suspend fun refresh(period: Period, force: Boolean) = mutex.withLock {
        loadLocked()

        val now = System.currentTimeMillis()
        val start = period.startTime(now)
        val changelog = _changelog.value?.takeIf { it.since <= start }

        if (!force && changelog != null && now - changelog.updatedAt in 0 until AUTO_REFRESH_INTERVAL) {
            return@withLock
        }

        val since = changelog?.let { maxOf(start, it.updatedAt - UPDATE_OVERLAP) } ?: start
        val manifest = manifestRepository.getManifest()

        val results = coroutineScope {
            listOf(
                async { fetchGitHubChanges(manifest, since) },
                async { fetchGerritChanges(manifest, since) },
            ).awaitAll()
        }

        // Keep what both periods need, so switching between them doesn't always refetch
        val retainSince = Period.entries.minOf { it.startTime(now) }
        val changes = (results.flatMap { it.changes } + changelog?.changes.orEmpty())
            .distinctBy { it.id }
            .filter { it.timestamp >= retainSince }
            .sortedByDescending { it.timestamp }

        val storedSince = maxOf(changelog?.since ?: start, retainSince)
        val completeSince = completeSince(changelog, since, results)

        val newChangelog = Changelog(
            buildTime = Build.TIME,
            since = storedSince,
            updatedAt = now,
            changes = changes,
            isTruncated = completeSince > storedSince,
            completeSince = completeSince,
        )

        withContext(Dispatchers.IO) {
            cacheFile.writeJson(json, newChangelog)
        }
        _changelog.value = newChangelog

        val knownChanges = baseline ?: Baseline(newChangelog).also { baseline = it }
        _newChangeIds.value = changes.filter {
            it.id !in knownChanges.changeIds && it.timestamp >= knownChanges.since
        }.mapTo(mutableSetOf()) { it.id }
    }

    /**
     * The time from which [results], merged with [previous], include every change.
     *
     * An update keeps an older hole. A fetch that reaches the start of [previous] and is not cut
     * off replaces it.
     */
    private fun completeSince(
        previous: Changelog?,
        fetchedFrom: Long,
        results: List<ChangesResult>,
    ): Long {
        val isTruncated = results.any { it.isTruncated }
        val fetchedCompleteSince = if (!isTruncated) {
            fetchedFrom
        } else {
            results.filter { it.isTruncated }
                .mapNotNull { it.changes.minOfOrNull { change -> change.timestamp } }
                .maxOrNull()
                ?: Long.MAX_VALUE
        }

        if (previous == null || (!isTruncated && fetchedFrom <= previous.since)) {
            return fetchedCompleteSince
        }

        val previousComplete = previous.knownCompleteSince()

        return if (isTruncated) {
            maxOf(previousComplete, fetchedCompleteSince)
        } else if (
            previous.completeSince == Changelog.UNKNOWN_COMPLETE_SINCE &&
            previous.isTruncated
        ) {
            previousComplete
        } else {
            minOf(previousComplete, fetchedFrom)
        }
    }

    private suspend fun loadLocked() {
        if (isLoaded) {
            return
        }

        val changelog = withContext(Dispatchers.IO) {
            cacheFile.readJson<Changelog>(json)
        }?.takeIf { it.buildTime == Build.TIME }

        _changelog.value = changelog
        baseline = changelog?.let { Baseline(it) }
        isLoaded = true
    }

    private suspend fun fetchGitHubChanges(manifest: Manifest, since: Long): ChangesResult {
        val projects = manifest.projects.mapNotNull { project ->
            project.gitHubRepository?.let { it.lowercase() to project }
        }.toMap()

        val result = gitHubDataSource.searchCommits(gitHubOrganizations, since)
        val defaultBranches = buildMap {
            gitHubOrganizations.filterValues { it == ChangeSource.DERPFEST }.keys.forEach { org ->
                putAll(gitHubDataSource.defaultBranches(org))
            }
        }

        return result.copy(
            changes = result.changes.mapNotNull { change ->
                when (change.source) {
                    // Organizations also host repositories that aren't part of the ROM.
                    // Search only returns the default branch, so it isn't the tracked branch
                    // when those two differ.
                    ChangeSource.DERPFEST -> projects[change.repository.lowercase()]?.let { project ->
                        val defaultBranch = defaultBranches[change.repository.lowercase()]
                        val trackedBranch = project.branch

                        if (
                            trackedBranch != null &&
                            defaultBranch != null &&
                            !trackedBranch.equals(defaultBranch, ignoreCase = true)
                        ) {
                            null
                        } else {
                            change.copy(path = project.path, branch = trackedBranch)
                        }
                    }

                    else -> change
                }
            }
        )
    }

    private suspend fun fetchGerritChanges(manifest: Manifest, since: Long): ChangesResult {
        val gerritHost = gerritUrl.hostName()
        val projects = manifest.projects.filter {
            it.review?.hostName() == gerritHost && it.branch != null
        }

        val result = gerritDataSource.getMergedChanges(
            projectBranches = projects.groupBy({ it.name }, { it.branch!! }).mapValues {
                it.value.toSet()
            },
            since = since,
        )

        val paths = projects.associate { (it.name to it.branch) to it.path }

        return result.copy(
            changes = result.changes.map {
                it.copy(path = paths[it.repository to it.branch])
            }
        )
    }

    /**
     * The changes known at a given point.
     *
     * @param since Since when the changes are known
     * @param changeIds The identifiers of the changes
     */
    private data class Baseline(
        val since: Long,
        val changeIds: Set<String>,
    ) {
        constructor(changelog: Changelog) : this(
            since = changelog.since,
            changeIds = changelog.changes.mapTo(mutableSetOf()) { it.id },
        )
    }

    companion object {
        private const val CACHE_FILE_NAME = "changelog.json"

        /**
         * Automatic refreshes are skipped if the changelog got refreshed within this interval.
         */
        private val AUTO_REFRESH_INTERVAL = TimeUnit.MINUTES.toMillis(5)

        /**
         * How far before the last refresh updates start, to catch commits pushed a while after
         * they got committed.
         */
        private val UPDATE_OVERLAP = TimeUnit.DAYS.toMillis(3)

        private fun String.hostName() = removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
    }
}
