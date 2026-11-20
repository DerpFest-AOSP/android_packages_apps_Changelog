/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.viewmodels

import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.derpfest.changelog.ChangelogApplication
import org.derpfest.changelog.ext.CHANGES_FILTER_KEYS
import org.derpfest.changelog.ext.TAP_ACTION_KEY
import org.derpfest.changelog.ext.changesFilter
import org.derpfest.changelog.ext.period
import org.derpfest.changelog.ext.preferenceFlow
import org.derpfest.changelog.ext.sharedPreferences
import org.derpfest.changelog.ext.showMerges
import org.derpfest.changelog.ext.showTranslations
import org.derpfest.changelog.ext.sources
import org.derpfest.changelog.ext.tapAction
import org.derpfest.changelog.models.Change
import org.derpfest.changelog.models.ChangeSource
import org.derpfest.changelog.models.ChangelogEntry
import org.derpfest.changelog.models.ChangelogError
import org.derpfest.changelog.models.Period
import java.time.Instant
import java.time.ZoneId

/**
 * View model of the changelog screen.
 */
class ChangelogViewModel(application: Application) : AndroidViewModel(application) {
    private val changelogRepository = getApplication<ChangelogApplication>().changelogRepository

    private val sharedPreferences = application.sharedPreferences

    /**
     * Which changes are shown.
     */
    val filter = sharedPreferences.preferenceFlow(
        *CHANGES_FILTER_KEYS,
        getter = SharedPreferences::changesFilter,
    )
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sharedPreferences.changesFilter,
        )

    /**
     * What tapping a change does.
     */
    val tapAction = sharedPreferences.preferenceFlow(
        TAP_ACTION_KEY,
        getter = SharedPreferences::tapAction,
    )
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sharedPreferences.tapAction,
        )

    /**
     * The changelog, null until it got loaded or fetched.
     */
    val changelog = changelogRepository.changelog

    /**
     * The entries of the list, null until the changelog got loaded or fetched.
     */
    val entries = combine(changelog, filter) { changelog, filter ->
        changelog?.let {
            val since = filter.period.startTime(System.currentTimeMillis())

            it.changes
                .filter { change -> change.timestamp >= since && filter.matches(change) }
                .toEntries(
                    buildTime = Build.TIME.takeIf { time ->
                        filter.period != Period.SINCE_BUILD && time >= since
                    },
                )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    /**
     * Whether the changelog is being refreshed.
     */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    /**
     * The error of the last refresh, until it got dismissed.
     */
    private val _error = MutableStateFlow<ChangelogError?>(null)
    val error = _error.asStateFlow()

    private var refreshJob: Job? = null

    init {
        // A longer period needs older changes
        viewModelScope.launch {
            filter
                .map { it.period }
                .distinctUntilChanged()
                .collect {
                    startRefresh(force = false)
                }
        }
    }

    /**
     * Refresh the changelog, unless a refresh is already running.
     */
    fun refresh() {
        if (refreshJob?.isActive == true) {
            return
        }

        startRefresh(force = true)
    }

    /**
     * Dismiss the error of the last refresh.
     */
    fun dismissError() {
        _error.value = null
    }

    fun setPeriod(period: Period) {
        sharedPreferences.period = period
    }

    fun setSourceShown(source: ChangeSource, shown: Boolean) {
        sharedPreferences.sources = when (shown) {
            true -> sharedPreferences.sources + source
            false -> sharedPreferences.sources - source
        }
    }

    fun setShowTranslations(showTranslations: Boolean) {
        sharedPreferences.showTranslations = showTranslations
    }

    fun setShowMerges(showMerges: Boolean) {
        sharedPreferences.showMerges = showMerges
    }

    /**
     * Start a refresh for the current period, replacing the running one.
     *
     * @param force Whether to refresh even if the changelog got refreshed moments ago
     */
    private fun startRefresh(force: Boolean) {
        val previousJob = refreshJob

        refreshJob = viewModelScope.launch {
            previousJob?.cancelAndJoin()

            _isRefreshing.value = true
            _error.value = null

            try {
                changelogRepository.refresh(filter.value.period, force)
            } catch (e: ChangelogError) {
                _error.value = e
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    companion object {
        /**
         * Group the changes by the day they got merged on, keeping their order.
         *
         * @param buildTime When the running build got made, to mark it among the changes
         */
        private fun List<Change>.toEntries(buildTime: Long?): List<ChangelogEntry> {
            if (isEmpty()) {
                return emptyList()
            }

            val zoneId = ZoneId.systemDefault()

            var isBuildMarked = buildTime == null

            return buildList {
                this@toEntries.groupBy {
                    Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
                }.forEach { (date, changes) ->
                    val (newerChanges, olderChanges) = when (isBuildMarked) {
                        true -> changes to emptyList()
                        false -> changes.partition { it.timestamp >= buildTime!! }
                    }

                    if (newerChanges.isEmpty()) {
                        addBuildMarker(buildTime, isBuildMarked)
                        isBuildMarked = true
                    }

                    add(ChangelogEntry.Day(date))
                    addItems(newerChanges)

                    if (olderChanges.isNotEmpty()) {
                        addBuildMarker(buildTime, isBuildMarked)
                        isBuildMarked = true
                    }

                    addItems(olderChanges)
                }

                addBuildMarker(buildTime, isBuildMarked)
            }
        }

        private fun MutableList<ChangelogEntry>.addBuildMarker(
            buildTime: Long?,
            isBuildMarked: Boolean,
        ) {
            if (buildTime != null && !isBuildMarked) {
                add(ChangelogEntry.BuildMarker(buildTime))
            }
        }

        private fun MutableList<ChangelogEntry>.addItems(changes: List<Change>) {
            changes.forEachIndexed { index, change ->
                add(
                    ChangelogEntry.Item(
                        change = change,
                        index = index,
                        count = changes.size,
                    )
                )
            }
        }
    }
}
